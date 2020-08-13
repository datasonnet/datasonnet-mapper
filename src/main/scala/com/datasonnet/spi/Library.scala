package com.datasonnet.spi

import com.datasonnet.Mapper
import fastparse.Parsed
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Val.Obj
import sjsonnet.{Evaluator, Expr, Val}

import scala.collection.mutable
import scala.io.Source

object Library {
  val emptyObj = new Val.Obj(mutable.HashMap.empty[String, Obj.Member], _ => (), None)
}

abstract class Library {
  def namespace(): String
  def functions(dataFormats: DataFormatService): Map[String, Val.Func]
  def modules(dataFormats: DataFormatService): Map[String, Val.Obj]
  def libsonnets(): Set[String]

  protected def moduleFrom(functions: (String, Val.Func)*): Val.Obj = new Val.Obj(
    mutable.LinkedHashMap[String, Val.Obj.Member] (
      functions.map{
          case (k, v) =>(k, memberOf(v))
        }: _*),
    _ => (),
    None
  )

  def makeLib(dataFormatService: DataFormatService, evaluator: Evaluator, cache: mutable.Map[String, Parsed[(Expr, Map[String, Int])]]): Map[String, Val.Obj] = Map(
    namespace() -> new Val.Obj(
      mutable.LinkedHashMap()
        ++ functions(dataFormatService).map {
        case (key, value) => (key, memberOf(value))
      }
        ++ modules(dataFormatService).map {
        case (key, value) => (key, memberOf(value))
      }
        ++ libsonnets().map {
        key => (key, memberOf(resolveLibsonnet(key, evaluator, cache)))
      }.toMap,
      _ => (),
      None
    )
  )

  private def resolveLibsonnet(name: String, evaluator: Evaluator, cache: mutable.Map[String, Parsed[(Expr, Map[String, Int])]]): Val = {
    val jsonnetLibrarySource = Source.fromURL(getClass.getResource(s"/$name.libsonnet"))
    val jsonnetLibrary = Mapper.evaluate(jsonnetLibrarySource.mkString, evaluator, cache, Map(), 0).get // libraries are never wrapped
    jsonnetLibrarySource.close

    jsonnetLibrary
  }

  private def memberOf(value: Val): Obj.Member = {
    Val.Obj.Member(add = false, Visibility.Hidden, (self: Val.Obj, sup: Option[Val.Obj], _, _) => value)
  }
}
