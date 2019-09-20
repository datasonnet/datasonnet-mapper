package com.datasonnet

import java.io.{File, PrintWriter, StringWriter}

import com.datasonnet.wrap.NoFileEvaluator
import fastparse.{IndexedParserInput, Parsed}
import os.Path
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet._

import scala.collection.JavaConverters._
import scala.io.Source
import scala.util.{Failure, Success, Try}


case class StringDocument(contents: String, mimeType: String) extends Document

object Mapper {
  def wrap(jsonnet: String, argumentNames: Iterable[String])=
    (Seq("payload") ++ argumentNames).mkString("function(", ",", ")\n") + jsonnet

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

  def input(data: Document): Expr = {
    val json = data.mimeType match {
      case "text/plain" | "application/csv" => ujson.Str(data.contents)
      case "application/json" => ujson.read(data.contents)
      case x => throw new IllegalArgumentException("The input mime type " + x + " is not supported")
    }

    Materializer.toExpr(json)
  }

  def output(output: ujson.Value, mimeType: String): Document = {
    val string = mimeType match {
      case "application/json" => output.toString()
      case "text/plain" | "application/csv" => output.str
      case x => throw new IllegalArgumentException("The output mime type " + x + " is not supported")
    }
    new StringDocument(string, mimeType)
  }
}


class Mapper(var jsonnet: String, argumentNames: java.lang.Iterable[String], needsWrapper: Boolean) {

  if(needsWrapper) {
    jsonnet = Mapper.wrap(jsonnet, argumentNames.asScala)
  }

  def lineOffset = if (needsWrapper) 1 else 0


  def this(jsonnet: File, argumentNames: java.lang.Iterable[String], needsWrapper: Boolean) =
    this(os.read(Path(jsonnet.getAbsoluteFile())), argumentNames, needsWrapper)

  private val parseCache = collection.mutable.Map[String, Parsed[Expr]]()

  private val standardLibrary = Map(
    "std" -> Lazy(Std.Std)
  )

  private var portx: Map[String, Val] = Map(
    "ZonedDateTime" -> PortX.ZonedDateTime,
    "LocalDateTime" -> PortX.LocalDateTime,
    "CSV" -> PortX.CSV,
    "Crypto" -> PortX.Crypto,
    //"XML" -> PortX.XML,
    "JsonPath" -> PortX.JsonPath
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


  def transform(payload: String): String = {
    transform(new StringDocument(payload, "application/json"), new java.util.HashMap(), "application/json").contents
  }

  def transform(payload: Document, arguments: java.util.Map[String, Document]): Document = {
    transform(payload, arguments,"application/json")
  }

  def transform(payload: Document, arguments: java.util.Map[String, Document], outputMimeType: String): Document = {

    val data = Mapper.input(payload)

    val parsedArguments = arguments.asScala.view.mapValues { Mapper.input(_) }


    val first :: rest: Seq[(String, Option[Expr])] = function.params.args

    val firstMaterialized: (String, Option[Expr]) = first.copy(_2 = Some(data))

    val values: List[(String, Option[Expr])] = rest.map { case (name, default) =>
      val argument: Option[Expr] = parsedArguments.get(name).orElse(default)
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

    Mapper.output(materialized, outputMimeType)
  }
}
