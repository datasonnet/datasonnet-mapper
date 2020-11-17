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

import scala.collection.mutable
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
        case f@Parsed.Failure(l, i, e) => Failure(new IllegalArgumentException("Problem parsing: " + expandErrorLineNumber(f.trace().msg, lineOffset)))
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
             dataFormats: DataFormatService = DataFormatService.DEFAULT,
             defaultOutput: MediaType = MediaTypes.APPLICATION_JSON) {

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

  // using uppercase DS is deprecated, but will remain supported
  private val defaultLibraries: Map[String, Obj] = DSLowercase.makeLib(dataFormats, header, evaluator, parseCache) concat DSUppercase.makeLib(dataFormats, header, evaluator, parseCache)
  private val libraries = additionalLibs.asScala.foldLeft(defaultLibraries) {
    (acc, lib) => acc concat lib.makeLib(dataFormats, header, evaluator, parseCache)
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

  // If the requested type is ANY then look in the header, default to JSON
  private def effectiveOutput(output: MediaType): MediaType = {
    if (output.equalsTypeAndSubtype(MediaTypes.ANY)) {
      val fromHeader = header.getDefaultOutput
      if (fromHeader.isPresent && !fromHeader.get.equalsTypeAndSubtype(MediaTypes.ANY)) header.combineOutputParams(fromHeader.get)
      else header.combineOutputParams(defaultOutput)
    } else {
      header.combineOutputParams(output)
    }
  }

  // If the input type is UNKNOWN then look in the header, default to JAVA
  private def effectiveInput[T](name: String, input: Document[T]): Document[T] = {
    if (input.getMediaType.equalsTypeAndSubtype(MediaTypes.UNKNOWN)){
      val fromHeader = header.getDefaultNamedInput(name)
      if (fromHeader.isPresent) header.combineInputParams(name, input.withMediaType(fromHeader.get))
      else header.combineInputParams(name, input.withMediaType(MediaTypes.APPLICATION_JAVA))
    } else {
      header.combineInputParams(name, input)
    }
  }

  // supports a Map[String, Document] to enable a scenario where documents are grouped into a single input
  private def resolveInput(name: String, input: Document[_]): ujson.Value = {
    if (!input.getContent.isInstanceOf[java.util.Map[_, _]]) return dataFormats.mandatoryRead(effectiveInput(name, input))

    val entrySet = input.getContent.asInstanceOf[java.util.Map[_, _]].entrySet()
    if (entrySet.isEmpty) return dataFormats.mandatoryRead(effectiveInput(name, input))

    val it = entrySet.iterator
    val firstEntry = it.next
    if (!firstEntry.getKey.isInstanceOf[String] || !firstEntry.getValue.isInstanceOf[Document[_]])
      return dataFormats.mandatoryRead(effectiveInput(name, input))

    val builder = mutable.LinkedHashMap.newBuilder[String, ujson.Value]
    val key = firstEntry.getKey.asInstanceOf[String]
    builder.addOne((key, dataFormats.mandatoryRead(effectiveInput(name + "." + key, firstEntry.getValue.asInstanceOf[Document[_]]))))
    while (it.hasNext) {
      val entry = it.next
      val key1 = entry.getKey.asInstanceOf[String]
      builder.addOne((key1, dataFormats.mandatoryRead(effectiveInput(name + "." + key1, entry.getValue.asInstanceOf[Document[_]]))))
    }
    ujson.Obj(builder.result)
  }

  def transform(payload: String): String = {
    transform(new DefaultDocument[String](payload)).getContent
  }

  def transform(payload: Document[_]): Document[String] = {
    transform(payload, Collections.emptyMap(), MediaTypes.ANY, classOf[String])
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
    val payloadExpr: Expr = Materializer.toExpr(dataFormats.mandatoryRead(effectiveInput("payload", payload)))
    val inputExprs: Map[String, Expr] = inputs.asScala.view.toMap[String, Document[_]].map {
      case (name, input) => (name, Materializer.toExpr(resolveInput(name, input)))
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

    val effectiveOut = effectiveOutput(output)
    dataFormats.mandatoryWrite(materialized, effectiveOut, target)
  }
}
