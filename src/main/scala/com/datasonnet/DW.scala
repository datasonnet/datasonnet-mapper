package com.datasonnet


import java.time.format.DateTimeFormatter
import java.time.{Instant, Period, ZoneId, ZoneOffset}

import com.datasonnet.spi.UjsonUtil
import com.datasonnet
import com.datasonnet.spi.{DataFormatPlugin, DataFormatService, UnsupportedMimeTypeException, UnsupportedParameterException}
import com.datasonnet.wrap.Library.library
import sjsonnet.ReadWriter.StringRead
import sjsonnet.Std._
import sjsonnet.{Applyer, EvalScope, Expr, Materializer, Val}

import scala.util.Random
import scala.util.Failure

object DW {
  val libraries = Map(
    "Core" -> library(

      builtin("abs", "num"){
        (_,_, num: Double) =>
          Math.abs(num);
      },

      builtin("avg", "array"){
        (_,_, array: Val.Arr) =>
          var value = 0.0
          val size = array.value.size
          var test = for (            i <- array.value
                                      ) yield value += i.force.asInstanceOf[Val.Num].value

          value.doubleValue() / size;
      },

      builtin("ceil", "num"){
        (_,_, num: Double) =>
          Math.ceil(num);
      },

      builtin("contains", "container", "value"){
        (ev,_, container: Val, value: Val) =>
          container match{
            case Val.Arr(s) =>
              val size = (
                for(
                  v <- s
                  if Materializer(v.force)(ev) == Materializer(value)(ev)
                ) yield Val.Lazy(Val.True)
                ).size
              if(size > 0) true else false

            case Val.Str(s) =>
              value.prettyName match {
                case "string" =>
                  if(s.toUpperCase().contains(value.toString.toUpperCase())) true else false;
                //TODO (string,regex)
                case _ => throw new IllegalArgumentException(
                  "Expected (string,string), got: (string," + value.prettyName + ")" );
              }

            case _ => throw new IllegalArgumentException(
              "Expected Array or String, got: " + container.prettyName);
          }
      },

      //TODO
      builtin("distinctBy", "container", "funct"){
        (_,_, container: Val, funct: Applyer) =>
          container match {
            case Val.Arr(s) =>
              Val.Arr(
                for(
                  item <- s
                )yield Val.Lazy(funct.apply(item))
              )

            case s: Val.Obj =>
              s

            case _ =>
              throw new IllegalArgumentException(
                "Expected Array or Object, got: " + container.prettyName);
          }
      },

      builtin("endsWith", "main", "sub"){
        (_,_, main: String, sub: String) =>
          main.toUpperCase.endsWith(sub.toUpperCase);
      },

      builtin("filter", "array", "funct"){
        (_,_, array: Val.Arr, funct: Applyer) =>
          Val.Arr(
            for(
              v <- array.value
              if funct.apply(v) == Val.True
            ) yield Val.Lazy(v.force)
          )

      },

      //TODO
      builtin("filterObject", "obj", "funct"){
        (_,_, obj: Val, funct: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("find", "container", "value"){
        (ev,_, container: Val, value: Val) =>
          container match {

            case Val.Str(s) =>
              value.prettyName match{
                case "string" =>
                  value //TODO String,String   String,regex
                case _ => throw new IllegalArgumentException(
                  "Expected (String,String), got: (String," + value.prettyName+")" );
              }

            case Val.Arr(s) =>
              Val.Arr(
                for(
                  (v,i) <- s.zipWithIndex
                  if Materializer(v.force)(ev) == Materializer(value)(ev)
                ) yield Val.Lazy(Val.Num(i))
              )

            case _ => throw new IllegalArgumentException(
              "Expected Array or String, got: " + container.prettyName);
          }
      },

      builtin("flatMap", "array", "funct"){
        (_,_, array: Val, funct: Val) =>
          array match {
            case Val.Arr(s) =>
              Val.Arr(
                for(
                  v <- s
                ) yield Val.Lazy(v.force) //TODO
              )
            case Val.Null => null
            case _ =>  throw new IllegalArgumentException(
              "Expected Array, got: " + array.prettyName);
          }
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("flatten", "array"){
        (_,_, array: Val.Arr) =>
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for(x <- array.value){
            x.force match{
              case Val.Null => // do nothing
              case Val.Arr(v) => out.appendAll(v)
              case x => throw new IllegalArgumentException(
                "Expected Array, got: " + array.prettyName);
            }
          }
          Val.Arr(out.toSeq)
      },

      builtin("floor", "num"){
        (_,_, num: Double) =>
          Math.floor(num);
      },

      builtin("groupBy", "container", "funct"){
        (_,_, container: Val, funct: Val) =>
          container match{
            case Val.Arr(s) => //TODO
            case s: Val.Obj => //TODO
            case Val.Null => Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
            case _ => throw new IllegalArgumentException(
              "Expected Array or Object, got: " + container.prettyName);
          }
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("isBlank", "value"){
        (_,_, value: Val) =>
          value match {
            case Val.Str(s) => s.trim().isEmpty
            case Val.Null => true
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + value.prettyName);
          }
      },

      builtin("isDecimal", "value"){
        (_,_, value: Double) =>
          (Math.ceil(value) != Math.floor(value)).booleanValue()
      },

      builtin("isEmpty", "container"){
        (_,_, container: Val) =>
          container match{
            case Val.Null => true
            case Val.Str(s) => s.isEmpty.booleanValue()
            case Val.Arr(s) => s.isEmpty.booleanValue()
            case s: Val.Obj => s.getVisibleKeys().isEmpty.booleanValue()
            case _ => throw new IllegalArgumentException(
              "Expected String, Array, or Object, got: " + container.prettyName);
          }
      },

      builtin("isEven", "num"){
        (_,_, num: Double) =>
          if((num%2)==0){
            true
          }
          else{
            false
          }
      },

      builtin("isInteger", "value") {
        (_,_, value: Val) =>
          com.datasonnet.DWCore.isInteger(value).booleanValue();
      },

      //TODO
      builtin("isLeapYear", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("isOdd", "num"){
        (_,_, num: Double) =>
          if((num%2)==0){
            false
          }
          else{
            true
          }
      },

      builtin("joinBy", "array", "sep"){
        (_,_, array: Val.Arr, sep: String) =>
          var acc = ""
          for(x <- array.value){
            val value = x.force match{
              case Val.Str(x) => x
              case Val.True => "true"
              case Val.False => "false"
              case Val.Num(x) => if(Math.ceil(x) != Math.floor(x)) x.toString else x.intValue().toString
              case _ => throw new IllegalArgumentException(
                "Expected String, Number, Boolean, got: " + x.force.prettyName);
            }
            if(acc.isEmpty) acc += value else acc += (sep + value)
          }
          acc
      },

      builtin("lower", "str"){
        (_,_, str: String) =>
          str.toLowerCase();
      },

      builtin("map", "array", "funct"){
        (_,_, array: Val, funct: Applyer) =>
          array match {
            case Val.Null => Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
            case Val.Arr(s) =>
              Val.Arr(
                for(
                  (v,i) <- s.zipWithIndex
                )yield Val.Lazy(funct.apply(v,Val.Lazy(Val.Num(i))))
              )
            case _ => throw new IllegalArgumentException(
              "Expected Array, got: " + array.prettyName);
          }
      },

      //TODO
      builtin("mapObject", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("match", "string", "regex") {
        (_, _, string: String, regex: String) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf(com.datasonnet.DWCore.`match`(string, regex)))

      },
      builtin("matches", "string", "regex") {
        (_,_, string: String, regex: String) =>
          com.datasonnet.DWCore.matches(string,regex).booleanValue()
      },

      builtin("max", "array"){
        (_,_, array: Val.Arr) =>

          var value = array.value.head
          for(x <- array.value){
            if(value.force.toString < x.force.toString){
              value = x
            }
          }
          value.force
      },

      builtin("maxBy", "array", "funct"){
        (_,_, array: Val.Arr, funct: Applyer) =>
          var value = array.value.head
          for(x <- array.value){
            if(funct.apply(value).toString < funct.apply(x).toString){
              value = x
            }
          }
          value.force
      },

      builtin("min", "array"){
        (_,_, array: Val.Arr) =>
          var value = array.value.head
          for(x <- array.value){
            if(value.force.toString > x.force.toString){
              value = x
            }
          }
          value.force
      },

      builtin("minBy", "array", "funct"){
        (_,_, array: Val.Arr, funct: Applyer) =>
          var value = array.value.head
          for(x <- array.value){
            if(funct.apply(value).toString > funct.apply(x).toString){
              value = x
            }
          }
          value.force
      },


      builtin("mod", "num1", "num2"){
        (_,_, num1: Double, num2: Double) =>
          num1 % num2;
      },

      //TODO
      builtin("orderBy", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      //TODO
      builtin("pluck", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("pow", "num1", "num2"){
        (_,_, num1: Double, num2: Double) =>
          Math.pow(num1,num2)
      },

      builtin0("random") {
        (_, _,_) =>
          (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
      },
      builtin("randomint", "num") {
        (_,_, num: Int) =>
          (Random.nextInt((num - 0) + 1) + 0).intValue()
      },

      //TODO
      builtin("read", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      //TODO
      builtin("readUrl", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      //TODO
      builtin("reduce", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },


      builtin("replace", "string", "regex", "replacement") {
        (_,_, string: String, regex: String, replacement: String) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf(com.datasonnet.DWCore.replace(string, regex, replacement)));
      },

      builtin("round", "num") {
        (_,_, num: Double) =>
          Math.round(num).intValue()
      },

      //TODO
      builtin0("scan"){
        (_,_,_) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("sizeOf", "value"){
        (_,_, value: Val) =>
          value match {
            case Val.Str(s) => s.length()
            case s: Val.Obj => s.getVisibleKeys().size
            case Val.Arr(s) => s.size
            case Val.Null => 0
            case _ => throw new IllegalArgumentException(
              "Expected Array, String, Object got: " + value.prettyName);
          }
      },

      //TODO String,String String,Regex
      builtin("splitBy", "value"){
        (_,_, value: Val) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf("null"));
      },

      builtin("sqrt", "num"){
        (_,_, num: Double) =>
          Math.sqrt(num)
      },

      builtin("startsWith", "str1", "str2"){
        (_,_, str1: String, str2: String) =>
          str1.toUpperCase().startsWith(str2.toUpperCase());
      },

      builtin("sum", "array"){
        (_,_, array: Val.Arr) =>
          var total = 0.0
          for(x <- array.value){
            x.force match {
              case Val.Num(s) => total += s
              case _ => throw new IllegalArgumentException(
                "Expected Array of numbers got: " + x.force.prettyName);
            }
          }
          total
      },

      builtin("to", "begin", "end"){
        (_,_, begin: Int, end: Int) =>
          Val.Arr(
            (begin until (end+1)).map( i =>
              Val.Lazy(Val.Num(i))
            )
          )
      },

      builtin("trim", "str"){
        (_,_, str: String) =>
          str.trim()
      },

      builtin("typeOf", "value"){
        (_,_, value: Val) =>
          value match{
            case Val.True | Val.False => "boolean"
            case Val.Null => "null"
            case _: Val.Obj => "object"
            case _: Val.Arr => "array"
            case _: Val.Func => "function"
            case _: Val.Num => "number"
            case _: Val.Str => "string"
          }
      },

      builtin("unzip", "array"){
        (_, _, array: Val.Arr) =>
          var size = Int.MaxValue
          for(v <- array.value){
            v.force match{
              case Val.Arr(s) => if(s.size < size) size = s.size
              case _ =>   throw new IllegalArgumentException(
                "Expected Array, got: " + v.force.prettyName);
            }
          }
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for(i <-0 until size) {
            val current = collection.mutable.Buffer.empty[Val.Lazy]
            for (x <- array.value) {
              x.force match{
                case Val.Arr(s) => current.append(s(i))
              }
            }
            out.append(Val.Lazy(Val.Arr(current.toSeq)))
          }
          Val.Arr(out.toSeq)
      },

      builtin("upper", "str"){
        (_,_, str: String) =>
          str.toUpperCase()
      },

      builtin0("uuid") {
        (_, _, _) =>
          Materializer.reverse(UjsonUtil.jsonObjectValueOf(com.datasonnet.DWCore.uuid()));
      },

      builtin("zip", "array1", "array2") {
        (_, _, array1: Val.Arr, array2: Val.Arr) =>

          val smallArray = if(array1.value.size <= array2.value.size) array1 else array2
          val bigArray = (if(smallArray == array1) array2 else array1).value
          val out = collection.mutable.Buffer.empty[Val.Lazy]
          for((v,i) <- smallArray.value.zipWithIndex){
            val current = collection.mutable.Buffer.empty[Val.Lazy]
            if(smallArray == array1) {
              current.append(v)
              current.append(bigArray(i))
            }
            else{
              current.append(bigArray(i))
              current.append(v)
            }
            out.append(Val.Lazy(Val.Arr(current.toSeq)))
          }
          Val.Arr(out.toSeq)
      },
    )
  )
}
