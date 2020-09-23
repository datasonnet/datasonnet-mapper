package com.datasonnet

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.{PrintWriter, StringWriter}
import java.util.Collections

import com.datasonnet.document.{DefaultDocument, Document, MediaType, MediaTypes}
import com.datasonnet.header.Header
import com.datasonnet.spi.{DataFormatService, Library}
import com.datasonnet.wrap.{DataSonnetPath, NoFileEvaluator}
import fastparse.Parsed
import sjsonnet.Expr.Params
import sjsonnet.Val.{Func, Lazy, Obj}
import sjsonnet._
import ujson.Value

import scala.jdk.CollectionConverters.{IterableHasAsScala, MapHasAsScala}
import scala.util.{Failure, Success, Try}

object Mapper {
  private def asFunction(script: String, argumentNames: Iterable[String]) =
    (Seq("payload") ++ argumentNames).mkString("function(", ",", ")\n") + script

  private val ERROR_LINE_REGEX = raw"\.\(([a-zA-Z-_\.]*):(\d+):(\d+)\)|([a-zA-Z-]+):(\d+):(\d+)".r

  private def expandErrorLineNumber(error: String, lineOffset: Int) = ERROR_LINE_REGEX.replaceAllIn(error, _ match {
    case ERROR_LINE_REGEX(filename, frow, fcolumn, token, trow, tcolumn) => {
      if (token != null) {
        s"$token at line ${trow.toInt - lineOffset} column $tcolumn"
      } else if (filename.length > 0) {
        s"$filename line $frow column $fcolumn"
      } else {
        s"line ${frow.toInt - lineOffset} column $fcolumn of the transformation"
      }
    }
  })

  def evaluate(script: String, evaluator: Evaluator, cache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]], libraries: Map[String, Val], lineOffset: Int): Try[Val] = {
    for {
      fullParse <- cache.getOrElseUpdate(script, fastparse.parse(script, Parser.document(_))) match {
        case f @ Parsed.Failure(l, i, e) => Failure(new IllegalArgumentException("Problem parsing: " + expandErrorLineNumber(f.trace().msg, lineOffset)))
        case Parsed.Success(r, index) => Success(r)
      }

      (parsed, indices) = fullParse

      evaluated <-
        try Success(evaluator.visitExpr(parsed)(
          Mapper.scope(indices, libraries),
          new FileScope(DataSonnetPath("."), indices)
        ))
        catch {
          case e: Throwable =>
            val s = new StringWriter()
            val p = new PrintWriter(s)
            e.printStackTrace(p)
            p.close()
            Failure(new IllegalArgumentException("Please contact the developers with this error! Problem compiling: " + s.toString.replace("\t", "    ")))
        }
    } yield evaluated
  }

  private def scope(indices: Map[String, Int], roots: Map[String, Val]) = Std.scope(indices.size + 1).extend(
    roots flatMap {
      case (key, value) =>
        if (indices.contains(key))
          Seq((indices(key), (_: Option[Obj], _: Option[Obj]) => Lazy(value)))
        else
          Seq()
    }
  )
}

class Mapper(var script: String,
             inputNames: java.lang.Iterable[String] = Collections.emptySet(),
             imports: java.util.Map[String, String] = Collections.emptyMap(),
             asFunction: Boolean = true,
             additionalLibs: java.util.Collection[Library] = Collections.emptyList(),
             dataFormats: DataFormatService = DataFormatService.DEFAULT) {

  private val header = Header.parseHeader(script)

  if (asFunction) {
    script = Mapper.asFunction(script, inputNames.asScala)
  }

  private val lineOffset = if (asFunction) 1 else 0

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String],
           asFunction: Boolean,
           additionalLibs: java.util.Collection[Library]) = {
    this(script, inputNames, imports, asFunction, additionalLibs, DataFormatService.DEFAULT)
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String],
           asFunction: Boolean) = {
    this(script, inputNames, imports, asFunction, Collections.emptyList())
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String],
           imports: java.util.Map[String, String]) = {
    this(script, inputNames, imports, true, Collections.emptyList())
  }

  def this(script: String,
           inputNames: java.lang.Iterable[String]) = {
    this(script, inputNames, Collections.emptyMap())
  }

  def this(script: String) = {
    this(script, Collections.emptySet())
  }

  private def importer(parent: Path, path: String): Option[(Path, String)] = for {
    resolved <- parent match {
      case DataSonnetPath("") => Some(path)
      case DataSonnetPath(p) => Some(p + "/" + path)
      case _ => None
    }
    contents <- imports.get(resolved) match {
      case null => None
      case v => Some(v)
    }

    combined = (DataSonnetPath(resolved) -> contents)
  } yield combined

  private val parseCache = collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]]()
  private val evaluator = new NoFileEvaluator(script, DataSonnetPath("."), parseCache, importer, header.isPreserveOrder)

  private val libraries = additionalLibs.asScala.foldLeft(DS.makeLib(dataFormats, evaluator, parseCache)){
    (acc, lib) => acc concat lib.makeLib(dataFormats, evaluator, parseCache)
  }

  imports.forEach((name, lib) => {
    if (name.endsWith(".libsonnet") || name.endsWith(".ds")) {
      val evaluated = Mapper.evaluate(lib, evaluator, parseCache, libraries, 0)
      evaluated match {
        case Success(value) =>
        case Failure(f) => throw new IllegalArgumentException("Unable to parse library: " + name, f)
      }
    }
  })

  private val function = (for {
    evaluated <- Mapper.evaluate(script, evaluator, parseCache, libraries, lineOffset)
    verified <- evaluated match {
      case f: Func => {
        val topLevelF = f.asInstanceOf[Func]
        if (topLevelF.params.args.size < 1)
          Failure(new IllegalArgumentException("Top Level Function must have at least one argument."))
        else
          Success(f)
      }
      case _ => Failure(new IllegalArgumentException("Not a valid map. Maps must have a Top Level Function."))
    }
  } yield verified).get

  def transform(payload: String): String = {
    transform(new DefaultDocument[String](payload)).getContent
  }

  def transform(payload: Document[_]): Document[String] = {
    transform(payload, Collections.emptyMap(), MediaTypes.APPLICATION_JSON, classOf[String])
  }

  def transform(payload: Document[_],
                inputs: java.util.Map[String, Document[_]],
                output: MediaType): Document[String] = {
    transform(payload, inputs, output, classOf[String])
  }

  def transform[T](payload: Document[_],
                   inputs: java.util.Map[String, Document[_]],
                   output: MediaType,
                   target: Class[T]): Document[T] = {
    def valueFrom(doc: Document[_]): Value = {
      dataFormats.thatAccepts(doc)
        .orElseThrow(() => new IllegalArgumentException("The input mime type " + payload.getMediaType + " is not supported"))
        .read(doc)
    }

    val payloadExpr: Expr = Materializer.toExpr(valueFrom(header.combineInputParams("payload", payload)))
    val inputExprs: Map[String, Expr] = inputs.asScala.view.toMap[String, Document[_]].map {
      case (name, input) => (name, Materializer.toExpr(valueFrom(header.combineInputParams(name, input))))
    }

    val payloadArg +: inputArgs = function.params.args

    val materialPayload = payloadArg.copy(_2 = Some(payloadExpr))
    val materialInputs = inputArgs.map { case (name, default, i) =>
      val argument: Option[Expr] = inputExprs.get(name).orElse(default)
      (name, argument, i)
    }.toVector

    val materialized = try Materializer.apply(function.copy(params = Params(materialPayload +: materialInputs)))(evaluator)
    catch {
      // if there's a parse error it must be in an import, so the offset is 0
      case Error(msg, stack, underlying) if msg.contains("had Parse error") => throw new IllegalArgumentException("Problem executing script: " + Mapper.expandErrorLineNumber(msg, 0))
      case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        throw new IllegalArgumentException("Problem executing script: " + Mapper.expandErrorLineNumber(s.toString, lineOffset).replace("\t", "    "))
    }

    val mixedOutput = header.combineOutputParams(output)

    dataFormats.thatProduces(mixedOutput, target)
      .orElseThrow(() => new IllegalArgumentException("The output mime type " + output + " is not supported"))
      .write(materialized, mixedOutput, target)
  }
}
