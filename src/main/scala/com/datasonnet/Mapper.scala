package com.datasonnet

import java.io.{PrintWriter, StringWriter}
import java.util
import java.util.Collections

import com.datasonnet.document.{Document, StringDocument}
import com.datasonnet.header.Header
import com.datasonnet.spi.DataFormatService
import com.datasonnet.wrap.{DataSonnetPath, NoFileEvaluator}
import fastparse.{IndexedParserInput, Parsed}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.Lazy
import sjsonnet._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}

object Mapper {
  def wrap(jsonnet: String, argumentNames: Iterable[String])=
    (Seq("payload") ++ argumentNames).mkString("function(", ",", ")\n") + jsonnet

  val location = raw"\.\(([a-zA-Z-_\.]*):(\d+):(\d+)\)|([a-zA-Z-]+):(\d+):(\d+)".r

  def expandErrorLineNumber(error: String, lineOffset: Int) = location.replaceAllIn(error, _ match {
    case location(filename, frow, fcolumn, token, trow, tcolumn) => {
      if (token != null) {
        s"$token at line ${trow.toInt - lineOffset} column $tcolumn"
      } else if (filename.length > 0) {
        s"$filename line $frow column $fcolumn"
      } else {
        s"line ${frow.toInt - lineOffset} column $fcolumn of the transformation"
      }
    }
  })

  def evaluate(evaluator: Evaluator, cache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]], jsonnet: String, libraries: Map[String, Val], lineOffset: Int): Try[Val] = {

    for {
      fullParse <- cache.getOrElseUpdate(jsonnet, fastparse.parse(jsonnet, Parser.document(_))) match {
        case f @ Parsed.Failure(l, i, e) => Failure(new IllegalArgumentException("Problem parsing: " + expandErrorLineNumber(f.trace().msg, lineOffset)))
        case Parsed.Success(r, index) => Success(r)
      }

      (parsed, indices) = fullParse

      evaluated <-
        try Success(evaluator.visitExpr(parsed)(
          Mapper.scope(indices, libraries),
          new FileScope(DataSonnetPath("."), indices)
        ))
        catch { case e: Throwable =>
          val s = new StringWriter()
          val p = new PrintWriter(s)
          e.printStackTrace(p)
          p.close()
          Failure(new IllegalArgumentException("Please contact the developers with this error! Problem compiling: " + s.toString.replace("\t", "    ")))
        }
    } yield evaluated
  }

  def scope(indices: Map[String, Int], roots: Map[String, Val]) = Std.scope(indices.size + 1).extend(
    roots flatMap {
      case (key, value) =>
        if (indices.contains(key))
          Seq((indices(key), (_: Option[Val.Obj], _: Option[Val.Obj]) => Lazy(value)))
        else
          Seq()
    }
  )

  def objectify(objects: Map[String, Val]) = new Val.Obj(
    objects.map{
      case (key, value) =>
        (key, Val.Obj.Member(false, Visibility.Hidden, (self: Val.Obj, sup: Option[Val.Obj], _, _) => value))
    }
    .toMap,
    _ => (),
    None
  )

  def library(evaluator: Evaluator, name: String, cache: collection.mutable.Map[String, fastparse.Parsed[(Expr, Map[String, Int])]]): (String, Val) = {
    val jsonnetLibrarySource = Source.fromURL(getClass.getResource(s"/$name.libsonnet")).mkString
    val jsonnetLibrary = Mapper.evaluate(evaluator, cache, jsonnetLibrarySource, Map(), 0).get  // libraries are never wrapped
    (name -> jsonnetLibrary)
  }

  def input(name: String, data: Document[_], header: Header): Expr = {
    val plugin = DataFormatService.getInstance().getPluginFor(data.getMimeType)
    if (plugin != null) {
      val params = header.getInputParameters(name, data.getMimeType)
      checkParams(params, plugin.getReadParameters(), plugin.getPluginId)
      val json = plugin.read(data.getContents(), params.asInstanceOf[util.Map[String, AnyRef]])
      Materializer.toExpr(json)
    } else {
      throw new IllegalArgumentException("The input mime type " + data.getMimeType + " is not supported")
    }
  }

  def output(output: ujson.Value, mimeType: String, header: Header): Document[_] = {
    val plugin = DataFormatService.getInstance().getPluginFor(mimeType)
    if (plugin != null) {
      val params = header.getOutputParameters(mimeType)
      checkParams(params, plugin.getWriteParameters(), plugin.getPluginId)
//      val str = plugin.write(output, params.asInstanceOf[util.Map[String, AnyRef]])
//      new StringDocument(str, mimeType)
      plugin.write(output, params.asInstanceOf[util.Map[String, AnyRef]], mimeType)
    } else {
      throw new IllegalArgumentException("The output mime type " + mimeType + " is not supported")
    }
  }

  def checkParams(params: util.Map[_, _], supportedParams: util.Map[_, _], pluginId: String) = {
    for (paramName <- params.keySet().toArray) {
      if (!supportedParams.containsKey(paramName)) {
        throw new IllegalArgumentException("The parameter '" + paramName + "' not supported by plugin " + pluginId)
      }
    }
  }
}

class Mapper(var jsonnet: String, argumentNames: java.lang.Iterable[String], imports: java.util.Map[String, String], needsWrapper: Boolean) {

  var header = Header.parseHeader(jsonnet);

  if(needsWrapper) {
    jsonnet = Mapper.wrap(jsonnet, argumentNames.asScala)
  }

  def lineOffset = if (needsWrapper) 1 else 0

  def this(jsonnet: String, argumentNames: java.lang.Iterable[String], needsWrapper: Boolean) {
    this(jsonnet, argumentNames, Collections.emptyMap(), needsWrapper)
  }

  def importer(parent: Path, path: String): Option[(Path, String)] = for {
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

  val evaluator = new NoFileEvaluator(jsonnet, DataSonnetPath("."), parseCache, importer)

  private val libraries = Map(
    "DS" -> Mapper.objectify(
      PortX.libraries + Mapper.library(evaluator, "Util", parseCache)
    )
  )

  imports.forEach((name, lib) => {
    if (name.endsWith(".libsonnet") || name.endsWith(".ds")) {
      val evaluated = Mapper.evaluate(evaluator, parseCache, lib, libraries, 0)
      evaluated match {
        case Success(value) =>
        case Failure(f) => throw new IllegalArgumentException("Unable to parse library: " + name, f)
      }
    }
  })

  private val function = (for {
    evaluated <- Mapper.evaluate(evaluator, parseCache, jsonnet, libraries, lineOffset)
    verified <- evaluated match {
      case f: Val.Func => {
        val topLevelF = f.asInstanceOf[Val.Func]
        if (topLevelF.params.args.size < 1)
          Failure(new IllegalArgumentException("Top Level Function must have at least one argument."))
        else
          Success(f)
      }
      case _ => Failure(new IllegalArgumentException("Not a valid map. Maps must have a Top Level Function."))
    }
  } yield verified).get

  private val mapIndex = new IndexedParserInput(jsonnet);

  def transform(payload: String): String = {
    transform(new StringDocument(payload, "application/json"), new java.util.HashMap(), "application/json").asInstanceOf[StringDocument].getContents
  }

  def transform(payload: Document[_], arguments: java.util.Map[String, Document[_]]): Document[_] = {
    transform(payload, arguments,"application/json")
  }

  def transform(payload: Document[_], arguments: java.util.Map[String, Document[_]], outputMimeType: String): Document[_] = {

    val data = Mapper.input("payload", payload, header)

    //val parsedArguments = arguments.asScala.view.mapValues { Mapper.input(_, header) }
    val parsedArguments = arguments.asScala.view.toMap[String, Document[_]].map { case (name, data) => (name, Mapper.input(name, data, header)) }

    val first +: rest = function.params.args

    val firstMaterialized = first.copy(_2 = Some(data))

    val values = rest.map { case (name, default, i) =>
      val argument: Option[Expr] = parsedArguments.get(name).orElse(default)
      (name, argument, i)
    }.toVector


    val materialized = try Materializer.apply(function.copy(params = Params(firstMaterialized +: values)))(evaluator)
    catch {
      // if there's a parse error it must be in an import, so the offset is 0
      case Error(msg, stack, underlying) if msg.contains("had Parse error")=> throw new IllegalArgumentException("Problem executing map: " + Mapper.expandErrorLineNumber(msg, 0))
      case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        throw new IllegalArgumentException("Problem executing map: " + Mapper.expandErrorLineNumber(s.toString, lineOffset).replace("\t", "    "))
    }

    Mapper.output(materialized, outputMimeType, header)
  }
}
