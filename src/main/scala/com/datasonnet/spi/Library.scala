package com.datasonnet.spi

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

import com.datasonnet.Mapper
import com.datasonnet.header.Header
import fastparse.Parsed
import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet.Val.Obj
import sjsonnet.{Evaluator, Expr, Val}

import scala.collection.mutable
import scala.io.Source
import scala.jdk.CollectionConverters._

object Library {
  val EmptyObj = new Val.Obj(mutable.HashMap.empty[String, Obj.Member], _ => (), None)

  def memberOf(value: Val): Obj.Member = Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => value)
}

abstract class Library {
  def namespace(): String

  def functions(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Func]

  def modules(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Obj]

  def libsonnets(): java.util.Set[String]

  protected def moduleFrom(functions: (String, Val.Func)*): Val.Obj = new Val.Obj(
    mutable.LinkedHashMap[String, Val.Obj.Member](
      functions.map {
        case (k, v) => (k, memberOf(v))
      }: _*),
    _ => (),
    None
  )

  protected def makeSimpleFunc(params: java.util.List[String], eval: java.util.function.Function[java.util.List[Val], Val]): Val.Func = {
    val paramData = params.asScala.zipWithIndex.map{case (k,  i) => (k, None, i)}.toArray
    val paramIndices = params.asScala.indices
    Val.Func(
      None,
      Params(paramData),
      {(scope, thisFile, ev, fs, outerOffset) => eval.apply(paramIndices.map(i => scope.bindings(i).get.force).asJava) }
    )
  }

  def makeLib(dataFormatService: DataFormatService,
              header: Header,
              evaluator: Evaluator,
              cache: mutable.Map[String, Parsed[(Expr, Map[String, Int])]]
             ): Map[String, Val.Obj] = Map(
    namespace() -> new Val.Obj(
      mutable.LinkedHashMap()
        ++ functions(dataFormatService, header).asScala.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ modules(dataFormatService, header).asScala.map {
        case (key, value) => (key, memberOf(value))
      }
        ++ libsonnets().asScala.map {
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
