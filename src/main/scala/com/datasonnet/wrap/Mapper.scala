package com.datasonnet.wrap

import java.io.{File, PrintWriter, StringWriter}
import java.util

import com.datasonnet.PortX
import fastparse.{IndexedParserInput, Parsed}

import scala.collection.JavaConverters._
import os.Path
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet._

import scala.io.Source
import scala.util.{Failure, Success, Try}

object Mapper {
  def wrap(jsonnet: String, arguments: util.Map[String, String])=
    (Seq("payload") ++ arguments.asScala.keys).mkString("function(", ",", ")\n") + jsonnet

  def expandParseErrorLineNumber(error: String): String =
    error.replaceFirst("([a-zA-Z-]):(\\d+):(\\d+)", "$1 at line $2 column $3")

  def evaluate(cache: collection.mutable.Map[String, fastparse.Parsed[Expr]], scope: Scope, jsonnet: String): Try[Val] = {
    val evaluator = new NoFileEvaluator(cache, scope)

    for {
      parsed <- cache.getOrElseUpdate(jsonnet, fastparse.parse(jsonnet, Parser.document(_))) match {
        case f @ Parsed.Failure(l, i, e) => Failure(new IllegalArgumentException("Problem parsing: " + expandParseErrorLineNumber(f.trace().msg)))
        case Parsed.Success(r, index) => Success(r)
      }
      evaluated <-
        try Success(evaluator.visitExpr(parsed, scope))
        catch { case e: Throwable =>
          val s = new StringWriter()
          val p = new PrintWriter(s)
          e.printStackTrace(p)
          p.close()
          Failure(new IllegalArgumentException("Please contact the developers with this error! Problem compiling: " + s.toString.replace("\t", "    ")))
        }
    } yield evaluated
  }

  def scope(libraries: Map[String, Lazy]) = new Scope(None, None, None, libraries, os.pwd / "(memory)", os.pwd, List(), None)

  def library(libraries: Map[String, Lazy], cache: collection.mutable.Map[String, fastparse.Parsed[Expr]], name: String): (String, Val) = {
    val jsonnetLibrarySource = Source.fromURL(getClass.getResource(s"/$name.libsonnet")).mkString
    val jsonnetLibrary = Mapper.evaluate(cache, scope(libraries), jsonnetLibrarySource).get
    (name -> jsonnetLibrary)
  }
}


class Mapper(var jsonnet: String, arguments: java.util.Map[String, String], needsWrapper: Boolean) {

  if(needsWrapper) {
    jsonnet = Mapper.wrap(jsonnet, arguments)
  }

  def lineOffset = if (needsWrapper) 1 else 0


  def this(jsonnet: File, arguments: util.Map[String, String], needsWrapper: Boolean) =
    this(os.read(Path(jsonnet.getAbsoluteFile())), arguments, needsWrapper)

  private val parseCache = collection.mutable.Map[String, Parsed[Expr]]()

  private val standardLibrary = Map(
    "std" -> Lazy(Std.Std)
  )

  private var portx: Map[String, Val] = Map(
    "ZonedDateTime" -> PortX.ZonedDateTime,
    "LocalDateTime" -> PortX.LocalDateTime,
    "CSV" -> PortX.CSV,
    "Crypto" -> PortX.Crypto
  )

  portx = portx + Mapper.library(standardLibrary, parseCache, "Util")


  private var libraries = Map(
    "std" -> Lazy(Std.Std),
    "PortX" -> Lazy(Val.Obj(portx.map {
        case (k, v) =>
          (
            k,
            Val.Obj.Member(
              false,
              Visibility.Hidden,
              (self: Val.Obj, sup: Option[Val.Obj], _) => Lazy(v)
            )
          )
      }.toMap,
      _ => (),
      None
    )
  ))

  private val scope = Mapper.scope(libraries)

  
  private val function = (for {
    evaluated <- Mapper.evaluate(parseCache, scope, jsonnet)
    verified <- evaluated match {
      case f: Val.Func => Success(f) // TODO check for at least one argument
      case _ => Failure(new IllegalArgumentException("Not a valid map. Maps must have a Top Level Function."))
    }
  } yield verified).get


  private val mapIndex = new IndexedParserInput(jsonnet);

  private val offset = raw"\.\(\(memory\) offset::(\d+)\)".r

  def expandExecuteErrorLineNumber(error: String): String = offset.replaceAllIn(error, _ match {
    case offset(position) => {
      val Array(line, column) = mapIndex.prettyIndex(position.toInt).split(":")
      s"line ${line.toInt - lineOffset} column $column"
    }
  })

  private val parsedArguments = arguments.asScala.view.mapValues { (value) =>
    ujson.read(value) // not sure how to handle errors here
  }

  def transform(payload: String): String = {
    transform(payload, "application/json")
  }

  def transform(payload: String, inputMimeType: String): String = {
    transform(payload, inputMimeType,"application/json")
  }

  def transform(payload: String, inputMimeType: String, outputMimeType: String): String = {

    val jsonData = inputMimeType match {
      case "text/plain" | "application/csv" => ujson.Str(payload).render(2, true)
      case "application/xml" => throw new IllegalArgumentException("XML mapping is not supported yet")
      case "application/json" => payload
      case _ => throw new IllegalArgumentException("The input mime type " + inputMimeType + " is not supported")
    }

    val data = Materializer.toExpr(ujson.read(jsonData))

    val first :: rest: Seq[(String, Option[Expr])] = function.params.args

    val firstMaterialized: (String, Option[Expr]) = first.copy(_2 = Some(data))

    val values: List[(String, Option[Expr])] = rest.map { case (name, default) =>
      val argument: Option[Expr] = parsedArguments.get(name).map(Materializer.toExpr(_)).orElse(default)
      (name, argument)
    } .toList

    val materialized = try Materializer(function.copy(params = Params(firstMaterialized :: values)), Map(), os.pwd)
    catch {
      case DelegateError(msg) => throw new IllegalArgumentException("Problem executing map: " + expandExecuteErrorLineNumber(msg))
      case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        throw new IllegalArgumentException("Problem executing map: " + expandExecuteErrorLineNumber(s.toString).replace("\t", "    "))
    }

    if (outputMimeType == "application/csv")
      ujson.read(materialized.toString()).str.trim()
    else
      materialized.toString()
  }

  private def isJSON(data: String): Boolean = {
    try {
      ujson.read(data)
      true
    } catch {
      case e: Exception => false
    }
  }
}
