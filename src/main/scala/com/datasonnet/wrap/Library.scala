package com.datasonnet.wrap

import java.io.StringWriter
import java.util.Base64

import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet._

import scala.collection.mutable.ArrayBuffer
import scala.collection.compat._


object Library {
  def validate(vs: Array[Val],
               ev: EvalScope,
               fs: FileScope,
               rs: Array[ReadWriter[_]]) = {
    for(i <- vs.indices) yield {
      val v = vs(i)
      val r = rs(i)
      r.apply(v, ev, fs) match {
        case Left(err) => throw new Error.Delegate("Wrong parameter type: expected " + err + ", got " + v.prettyName)
        case Right(x) => x
      }
    }
  }

  def builtin[R: ReadWriter, T1: ReadWriter](name: String, p1: String)
                                            (eval: (EvalScope, FileScope, T1) => R): (String, Val.Func) = builtin0(name, p1){ (vs, ev, fs) =>
    val Seq(v: T1) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]]))
    eval(ev, fs, v)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter](name: String, p1: String, p2: String)
                                                            (eval: (EvalScope, FileScope, T1, T2) => R): (String, Val.Func) = builtin0(name, p1, p2){ (vs, ev, fs) =>
    val Seq(v1: T1, v2: T2) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]]))
    eval(ev, fs, v1, v2)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter](name: String, p1: String, p2: String, p3: String)
                                                                            (eval: (EvalScope, FileScope, T1, T2, T3) => R): (String, Val.Func) = builtin0(name, p1, p2, p3){ (vs, ev, fs) =>
    val Seq(v1: T1, v2: T2, v3: T3) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]]))
    eval(ev, fs, v1, v2, v3)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String)
                                                                            (eval: (EvalScope, FileScope, T1, T2, T3, T4) => R): (String, Val.Func) = builtin0(name, p1, p2, p3, p4){ (vs, ev, fs) =>
    val Seq(v1: T1, v2: T2, v3: T3, v4: T4) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]], implicitly[ReadWriter[T4]]))
    eval(ev, fs, v1, v2, v3, v4)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter, T5: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String, p5: String)
                                                                                            (eval: (EvalScope, FileScope, T1, T2, T3, T4, T5) => R): (String, Val.Func) = builtin0(name, p1, p2, p3, p4, p5){ (vs, ev, fs) =>
    val Seq(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]], implicitly[ReadWriter[T4]], implicitly[ReadWriter[T5]]))
    eval(ev, fs, v1, v2, v3, v4, v5)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter, T5: ReadWriter, T6: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String, p5: String, p6: String)
                                                                                                            (eval: (EvalScope, FileScope, T1, T2, T3, T4, T5, T6) => R): (String, Val.Func) = builtin0(name, p1, p2, p3, p4, p5, p6){ (vs, ev, fs) =>
    val Seq(v1: T1, v2: T2, v3: T3, v4: T4, v5: T5, v6: T6) = validate(vs, ev, fs, Array(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]], implicitly[ReadWriter[T4]], implicitly[ReadWriter[T5]], implicitly[ReadWriter[T6]]))
    eval(ev, fs, v1, v2, v3, v4, v5, v6)
  }

  def builtin0[R: ReadWriter](name: String, params: String*)(eval: (Array[Val], EvalScope, FileScope) => R) = {
    val paramData = params.zipWithIndex.map{case (k, i) => (k, None, i)}.toArray
    val paramIndices = params.indices.toArray
    name -> Val.Func(
      None,
      Params(paramData),
      {(scope, thisFile, ev, fs, outerOffset) =>
        implicitly[ReadWriter[R]].write(
          eval(paramIndices.map(i => scope.bindings(i).get.force), ev, fs)
        )
      }
    )
  }
  /**
    * Helper function that can define a built-in function with default parameters
    *
    * Arguments of the eval function are (args, ev)
    */
  def builtinWithDefaults[R: ReadWriter](name: String, params: (String, Option[Expr])*)
                                        (eval: (Map[String, Val], EvalScope) => R): (String, Val.Func) = {
    val indexedParams = params.zipWithIndex.map{case ((k, v), i) => (k, v, i)}.toArray
    val indexedParamKeys = params.zipWithIndex.map{case ((k, v), i) => (k, i)}
    name -> Val.Func(
      None,
      Params(indexedParams),
      { (scope, thisFile, ev, fs, outerOffset) =>
        val args = indexedParamKeys.map {case (k, i) => k -> scope.bindings(i).get.force }.toMap
        implicitly[ReadWriter[R]].write(eval(args, ev))
      },
      { (expr, scope, eval) =>
        eval.visitExpr(expr)(scope, new FileScope(null, Map.empty))
      }
    )
  }

  def library(functions: (String, Val.Func)*): Val.Obj = new Val.Obj(
    functions
      .map{
        case (k, v) =>
          (
            k,
            Val.Obj.Member(
              false,
              Visibility.Hidden,
              (self: Val.Obj, sup: Option[Val.Obj], _, _) => v
            )
          )
      }
      .toMap,
    _ => (),
    None
  )
}