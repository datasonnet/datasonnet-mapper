package com.datasonnet.wrap

import java.io.{PrintWriter, StringWriter}

import scala.collection.JavaConverters._
import fastparse.Parsed
import sjsonnet.Expr.Params
import sjsonnet._

import scala.io.Source


class Mapper(jsonnet: String, arguments: java.util.Map[String, String]) {

  private val parseCache = collection.mutable.Map[String, fastparse.Parsed[Expr]]()

  // TODO finish this. Might need a first evaluator; not sure how best to do that
//  private def library(name: String): Val = {
//    val resource = Source.fromResource(s"/$name.libsonnet").mkString
//
//  }

  private val libraries = Map(
    "std" -> Lazy(Std.Std),
    // "portx" -> Lazy(library("portx"))
  )
  // TODO add our java functions in the same way Std is done (can probably reuse Std utils a bunch, verify)
  private val scope = new Scope(None, None, None, libraries, os.pwd / "(memory)", os.pwd , List(), None)

  private val evaluator = new NoFileEvaluator(parseCache, scope)

  private val parsedArguments = arguments.asScala.mapValues { (value) =>
      ujson.read(value) // not sure how to handle errors here
    }


  private val function = (for {
    parsed <- parseCache.getOrElseUpdate(jsonnet, fastparse.parse(jsonnet, Parser.document(_))) match {
      case f @ Parsed.Failure(l, i, e) => throw new IllegalArgumentException("Parse error: " + f.trace().msg)
      case Parsed.Success(r, index) => Some(r)
    }
    evaluated <-
      try Some(evaluator.visitExpr(parsed, scope))
      catch { case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        throw new IllegalArgumentException("Problem evaluating map: " + s.toString.replace("\t", "    "))
      }
    verified <- evaluated match {
      case f: Val.Func => Some(f) // TODO check for at least one argument
      case _ => throw new IllegalArgumentException("Not a valid map. Maps must have a Top Level Function.")
    }
  } yield verified).get

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
      case DelegateError(msg) => throw new IllegalArgumentException("Problem executing map: " + msg)
      case e: Throwable =>
        val s = new StringWriter()
        val p = new PrintWriter(s)
        e.printStackTrace(p)
        p.close()
        throw new IllegalArgumentException("Problem executing map: " + s.toString.replace("\t", "    "))
    }

    materialized.toString()
  }
}
