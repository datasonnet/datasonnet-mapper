package com.datasonnet.wrap

import java.io.StringWriter
import java.util.Base64

import sjsonnet.Expr.Member.Visibility
import sjsonnet.Expr.Params
import sjsonnet._
import sjsonnet.Scope.empty

import scala.collection.mutable.ArrayBuffer
import scala.collection.compat._


object Library {
  sealed trait ReadWriter[T]{
    def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path): Either[String, T]
    def write(t: T): Val
  }
  object ReadWriter{
    implicit object StringRead extends ReadWriter[String]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case Val.Str(s) => Right(s)
        case _ => Left("String")
      }
      def write(t: String) = Val.Str(t)
    }
    implicit object BooleanRead extends ReadWriter[Boolean]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case Val.True => Right(true)
        case Val.False => Right(false)
        case _ => Left("Boolean")
      }
      def write(t: Boolean) = Val.bool(t)
    }
    implicit object IntRead extends ReadWriter[Int]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case Val.Num(s) => Right(s.toInt)
        case _ => Left("Int")
      }
      def write(t: Int) = Val.Num(t)
    }
    implicit object DoubleRead extends ReadWriter[Double]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case Val.Num(s) => Right(s)
        case _ => Left("Number")
      }
      def write(t: Double) = Val.Num(t)
    }
    implicit object ValRead extends ReadWriter[Val]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = Right(t)
      def write(t: Val) = t
    }
    implicit object ObjRead extends ReadWriter[Val.Obj]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case v: Val.Obj => Right(v)
        case _ => Left("Object")
      }
      def write(t: Val.Obj) = t
    }
    implicit object ArrRead extends ReadWriter[Val.Arr]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case v: Val.Arr => Right(v)
        case _ => Left("Array")
      }
      def write(t: Val.Arr) = t
    }
    implicit object FuncRead extends ReadWriter[Val.Func]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case v: Val.Func => Right(v)
        case _ => Left("Function")
      }
      def write(t: Val.Func) = t
    }

    implicit object ApplyerRead extends ReadWriter[Applyer]{
      def apply(t: Val, extVars: Map[String, ujson.Value], wd: os.Path) = t match{
        case v: Val.Func => Right(Applyer(v, extVars, wd))
        case _ => Left("Function")
      }
      def write(t: Applyer) = t.f
    }
  }
  case class Applyer(f: Val.Func, extVars: Map[String, ujson.Value], wd: os.Path){
    def apply(args: Lazy*) = f.apply(args.map((None, _)), "(memory)", extVars, -1, wd)
  }

  def validate(vs: Seq[Val], extVars: Map[String, ujson.Value], wd: os.Path, rs: Seq[ReadWriter[_]]) = {
    for((v, r) <- vs.zip(rs)) yield r.apply(v, extVars, wd) match{
      case Left(err) => throw new DelegateError("Wrong parameter type: expected " + err + ", got " + v.prettyName)
      case Right(x) => x
    }
  }

  def builtin[R: ReadWriter, T1: ReadWriter](name: String, p1: String)
                                            (eval:  (os.Path, Map[String, ujson.Value], T1) => R): (String, Val.Func) = builtin0(name, p1){ (vs, extVars, wd) =>
    val Seq(v: T1) = validate(vs, extVars, wd, Seq(implicitly[ReadWriter[T1]]))
    eval(wd, extVars, v)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter](name: String, p1: String, p2: String)
                                                            (eval:  (os.Path, Map[String, ujson.Value], T1, T2) => R): (String, Val.Func) = builtin0(name, p1, p2){ (vs, extVars, wd) =>
    val Seq(v1: T1, v2: T2) = validate(vs, extVars, wd, Seq(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]]))
    eval(wd, extVars, v1, v2)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter](name: String, p1: String, p2: String, p3: String)
                                                                            (eval:  (os.Path, Map[String, ujson.Value], T1, T2, T3) => R): (String, Val.Func) = builtin0(name, p1, p2, p3){ (vs, extVars, wd) =>
    val Seq(v1: T1, v2: T2, v3: T3) = validate(vs, extVars, wd, Seq(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]]))
    eval(wd, extVars, v1, v2, v3)
  }

  def builtin[R: ReadWriter, T1: ReadWriter, T2: ReadWriter, T3: ReadWriter, T4: ReadWriter](name: String, p1: String, p2: String, p3: String, p4: String)
                                                                            (eval:  (os.Path, Map[String, ujson.Value], T1, T2, T3, T4) => R): (String, Val.Func) = builtin0(name, p1, p2, p3, p4){ (vs, extVars, wd) =>
    val Seq(v1: T1, v2: T2, v3: T3, v4: T4) = validate(vs, extVars, wd, Seq(implicitly[ReadWriter[T1]], implicitly[ReadWriter[T2]], implicitly[ReadWriter[T3]], implicitly[ReadWriter[T4]]))
    eval(wd, extVars, v1, v2, v3, v4)
  }
  
  def builtin0[R: ReadWriter](name: String, params: String*)(eval: (Seq[Val], Map[String, ujson.Value], os.Path) => R) = {
    name -> Val.Func(
      empty,
      Params(params.map(_ -> None)),
      {(scope, thisFile, extVars, outerOffset, wd) => implicitly[ReadWriter[R]].write(eval(params.map(scope.bindings(_).get.force), extVars, wd))}
    )
  }
  /**
    * Helper function that can define a built-in function with default parameters
    *
    * Arguments of the eval function are (args, extVars, wd)
    */
  def builtinWithDefaults[R: ReadWriter](name: String, params: (String, Option[Expr])*)(eval: (Map[String, Val], Map[String, ujson.Value], os.Path) => R): (String, Val.Func) = {
    name -> Val.Func(
      empty,
      Params(params),
      { (scope, thisFile, extVars, outerOffset, wd) =>
        val args = params.map {case (k, v) => k -> scope.bindings(k).get.force }.toMap
        implicitly[ReadWriter[R]].write(eval(args, extVars, wd))
      },
      { (expr, scope) => new Evaluator(scala.collection.mutable.Map(), scope, Map(), null, None).visitExpr(expr, scope)}
    )
  }
  def library(functions: (String, Val.Func)*): Val.Obj = Val.Obj(
    functions
      .map{
        case (k, v) =>
          (
            k,
            Val.Obj.Member(
              false,
              Visibility.Hidden,
              (self: Val.Obj, sup: Option[Val.Obj], _) => Lazy(v)
            )
          )
      }
      .toMap,
    _ => (),
    None
  )
}