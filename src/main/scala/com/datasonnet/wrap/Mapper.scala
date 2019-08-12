package com.datasonnet.wrap

import java.io.{File, PrintWriter, StringWriter}
import java.util

import com.datasonnet.PortX

import scala.collection.JavaConverters._
import fastparse.{IndexedParserInput, Parsed}
import os.Path
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet._

import scala.io.Source

object Mapper {
  def wrap(jsonnet: String, arguments: util.Map[String, String])=
    (Seq("payload") ++ arguments.asScala.keys).mkString("function(", ",", ")\n") + jsonnet
}


class Mapper(jsonnet: String, arguments: java.util.Map[String, String]) {

  var wrapped = false;  // for use when setting line numbers
  def lineOffset = if (wrapped) 1 else 0



  def this(jsonnet: String, arguments: java.util.Map[String, String], needsWrapper: Boolean) {
    this(if (needsWrapper) Mapper.wrap(jsonnet, arguments) else jsonnet, arguments)
    wrapped = needsWrapper
  }

  def this(jsonnet: File, arguments: java.util.Map[String, String]) =
    this(os.read(Path(jsonnet.getAbsoluteFile())), arguments)
  def this(jsonnet: File, arguments: java.util.Map[String, String], needsWrapper: Boolean) =
    this(os.read(Path(jsonnet.getAbsoluteFile())), arguments, needsWrapper)

  private val parseCache = collection.mutable.Map[String, fastparse.Parsed[Expr]]()

  // TODO finish this. Might need a first evaluator; not sure how best to do that
//  private def library(name: String): Val = {
//    val resource = Source.fromResource(s"/$name.libsonnet").mkString
//
//  }

  private val portx = Map(
//    "Time" -> PortX.Time,
    "ZonedDateTime" -> PortX.ZonedDateTime,
    "LocalDateTime" -> PortX.LocalDateTime,
    "CSV" -> PortX.CSV,
    "Crypto" -> PortX.Crypto
  )

  private val libraries = Map(
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
  // TODO add our java functions in the same way Std is done (can probably reuse Std utils a bunch, verify)
  private val scope = new Scope(None, None, None, libraries, os.pwd / "(memory)", os.pwd , List(), None)

  private val evaluator = new NoFileEvaluator(parseCache, scope)

  private val parsedArguments = arguments.asScala.mapValues { (value) =>
      ujson.read(value) // not sure how to handle errors here
    }

  def expandParseErrorLineNumber(error: String): String =
    error.replaceFirst("([a-zA-Z-]):(\\d+):(\\d+)", "$1 at line $2 column $3")


  private val function = (for {
    parsed <- parseCache.getOrElseUpdate(jsonnet, fastparse.parse(jsonnet, Parser.document(_))) match {
      case f @ Parsed.Failure(l, i, e) => throw new IllegalArgumentException("Problem parsing map: " + expandParseErrorLineNumber(f.trace().msg))
      case Parsed.Success(r, index) => Some(r)
    }
    evaluated <-
      try Some(evaluator.visitExpr(parsed, scope))
      catch { case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        // as far as I can tell this is impossible if it parses unless there's some unexpected internal error
        throw new IllegalArgumentException("Please contact the developers with this error! Problem compiling map: " + s.toString.replace("\t", "    "))
      }
    verified <- evaluated match {
      case f: Val.Func => Some(f) // TODO check for at least one argument
      case _ => throw new IllegalArgumentException("Not a valid map. Maps must have a Top Level Function.")
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

    val data = Materializer.toExpr(ujson.read(payload))

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

    materialized.toString()
  }
}
