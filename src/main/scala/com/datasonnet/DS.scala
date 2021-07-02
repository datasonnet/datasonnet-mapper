package com.datasonnet

/*-
 * Copyright 2019-2021 the original author or authors.
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

import java.math.{BigDecimal, RoundingMode}
import java.net.URL
import java.security.SecureRandom
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{DateTimeException, Duration, Instant, LocalDateTime, Period, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.function.Function
import java.util.{Base64, Scanner}

import com.datasonnet
import com.datasonnet.document.{DefaultDocument, MediaType}
import com.datasonnet.header.Header
import com.datasonnet.modules.{Crypto, JsonPath, Regex}
import com.datasonnet.spi.{DataFormatService, Library, ujsonUtils}
import javax.crypto.Cipher
import javax.crypto.spec.{IvParameterSpec, SecretKeySpec}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.ReadWriter.{ApplyerRead, ArrRead, StringRead}
import sjsonnet.Std.{builtin, builtinWithDefaults, _}
import sjsonnet.{Applyer, Error, EvalScope, Expr, FileScope, Materializer, Val}
import ujson.Value

import scala.collection.mutable
import scala.util.Random
import scala.jdk.CollectionConverters._

object DSLowercase extends Library {

  override def namespace() = "ds"

  override def libsonnets(): java.util.Set[String] = Set("util").asJava

  override def functions(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Func] = Map(
    builtin("contains", "container", "value") {
      (_, _, container: Val, value: Val) =>
        container match {
          // See: scala.collection.IterableOnceOps.exists
          case Val.Arr(array) =>
            array.exists(_.force == value)
          case Val.Str(s) =>
            value.cast[Val.Str].value.r.findAllMatchIn(s).nonEmpty;
          case i => throw Error.Delegate("Expected Array or String, got: " + i.prettyName)
        }
    },

    builtin("entriesOf", "obj") {
      (ev, fs, obj: Val.Obj) =>
        Val.Arr(obj.getVisibleKeys().keySet.collect({
          case key =>
            val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
            currentObj += ("key" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Lazy(Val.Str(key)).force))
            currentObj += ("value" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))

            Val.Lazy(new Val.Obj(currentObj, _ => (), None))
        }).toSeq)
    },

    builtin("filter", "array", "funct") {
      (_, _, value: Val, funct: Applyer) =>
        value match {
          case Val.Arr(array) => filter(array, funct)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
        }
    },

    builtin("filterObject", "obj", "func") {
      (ev, fs, value: Val, func: Applyer) =>
        value match {
          case obj: Val.Obj => filterObject(obj, func, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
        }
    },

    builtin("find", "container", "value") {
      (_, _, container: Val, value: Val) =>
        container match {
          case Val.Str(str) =>
            val sub = value.cast[Val.Str].value
            Val.Arr(sub.r.findAllMatchIn(str).map(_.start).map(item => Val.Lazy(Val.Num(item))).toSeq)
          case Val.Arr(s) =>
            Val.Arr(s.zipWithIndex.collect({
              case (v, i) if v.force == value => Val.Lazy(Val.Num(i))
            }))
          case i => throw Error.Delegate("Expected Array or String, got: " + i.prettyName)
        }
    },

    builtin("flatMap", "array", "funct") {
      (_, _, array: Val, funct: Applyer) =>
        array match {
          case Val.Arr(s) => flatMap(s, funct)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
        }
    },

    builtin("flatten", "array") {
      (_, _, array: Val) =>
        array match {
          case Val.Arr(outerArray) =>
            val out = collection.mutable.Buffer.empty[Val.Lazy]
            for (innerArray <- outerArray) {
              innerArray.force match {
                case Val.Null => out.append(Val.Lazy(Val.Null))
                case Val.Arr(v) => out.appendAll(v)
                case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
              }
            }
            Val.Arr(out.toSeq)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
        }
    },

    builtin("distinctBy", "container", "funct") {
      (ev, fs, container: Val, funct: Applyer) =>
        container match {
          case Val.Arr(arr) =>
            distinctBy(arr, funct)
          case obj: Val.Obj =>
            distinctBy(obj, funct, ev, fs)
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },

    builtin("endsWith", "main", "sub") {
      (_, _, main: String, sub: String) =>
        main.toUpperCase.endsWith(sub.toUpperCase);
    },

    builtin("groupBy", "container", "funct") {
      (ev, fs, container: Val, funct: Applyer) =>
        container match {
          case Val.Arr(s) =>
            groupBy(s, funct)
          case obj: Val.Obj =>
            groupBy(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },


    builtin("isBlank", "value") {
      (_, _, value: Val) =>
        value match {
          case Val.Str(s) => s.trim().isEmpty
          case Val.Null => true
          case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
        }
    },

    builtin("isDecimal", "value") {
      (_, _, value: Double) =>
        (Math.ceil(value) != Math.floor(value)).booleanValue()
    },

    builtin("isEmpty", "container") {
      (_, _, container: Val) =>
        container match {
          case Val.Null => true
          case Val.Str(s) => s.isEmpty.booleanValue()
          case Val.Arr(s) => s.isEmpty.booleanValue()
          case s: Val.Obj => s.getVisibleKeys().isEmpty.booleanValue()
          case i => throw Error.Delegate("Expected String, Array, or Object, got: " + i.prettyName)
        }
    },

    builtin("isEven", "num") {
      (_, _, num: Double) =>
        (num % 2) == 0
    },

    builtin("isInteger", "value") {
      (_, _, value: Double) =>
        (Math.ceil(value) == Math.floor(value)).booleanValue()
    },

    builtin("isOdd", "num") {
      (_, _, num: Double) =>
        (num % 2) != 0
    },

    builtin("joinBy", "array", "sep") {
      (_, _, array: Val.Arr, sep: String) =>
        array.value.map({
          _.force match {
            case Val.Str(x) => x
            case Val.True => "true"
            case Val.False => "false"
            case Val.Num(x) => if (!x.isWhole) x.toString else x.intValue().toString
            case i => throw Error.Delegate("Expected String, Number, or Boolean, got: " + i.prettyName)
          }
        }).mkString(sep)
    },

    builtin("keysOf", "obj") {
      (_, _, obj: Val.Obj) =>
        Val.Arr(obj.getVisibleKeys().keySet.map(item => Val.Lazy(Val.Str(item))).toSeq)
    },

    builtin("lower", "str") {
      (_, _, str: String) =>
        str.toLowerCase();
    },

    builtin("map", "array", "funct") {
      (_, _, array: Val, funct: Applyer) =>
        array match {
          case Val.Arr(seq) =>
            map(seq, funct)
          case Val.Null => Val.Lazy(Val.Null).force
          case i =>throw Error.Delegate("Expected Array, got: " + i.prettyName)
        }
    },

    builtin("mapEntries", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case obj: Val.Obj =>
            mapEntries(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
        }
    },

    builtin("mapObject", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case obj: Val.Obj =>
            mapObject(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
        }
    },

    builtin("match", "string", "regex") {
      (_, _, string: String, regex: String) =>
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        regex.r.findAllMatchIn(string).foreach(
          word => (0 to word.groupCount).foreach(index => out += Val.Lazy(Val.Str(word.group(index))))
        )
        Val.Arr(out.toSeq)
    },

    builtin("matches", "string", "regex") {
      (_, _, string: String, regex: String) =>
        regex.r.matches(string);
    },

    builtin("max", "array") {
      (_, _, array: Val.Arr) =>
        var value = array.value.head
        for (x <- array.value) {
          value.force.prettyName match {
            case "string" =>
              if (value.force.cast[Val.Str].value < x.force.cast[Val.Str].value) {
                value = x
              }
            case "boolean" =>
              if (x.force == Val.Lazy(Val.True).force) {
                value = x
              }
            case "number" =>
              if (value.force.cast[Val.Num].value < x.force.cast[Val.Num].value) {
                value = x
              }
            case i => throw Error.Delegate(
              "Expected Array of type String, Boolean, or Number, got: Array of type " + i)
          }
        }
        value.force
    },

    builtin("maxBy", "array", "funct") {
      (_, _, array: Val.Arr, funct: Applyer) =>
        val seqVal = array.value
          funct.apply(seqVal.head).prettyName match{
          case "string" =>
            seqVal.maxBy(item => funct.apply(item).cast[Val.Str].value).force
          case "boolean" =>
            if(seqVal.forall( item => item.force.prettyName.equals("boolean")))
              if (seqVal.exists(item => item.force == Val.True)) {
                Val.True
              } else { Val.False }
            else throw Error.Delegate("Received a dirty array")
          case "number" =>
            seqVal.maxBy(item => funct.apply(item).cast[Val.Num].value).force
          case i => throw Error.Delegate(
            "Expected Array of type String, Boolean, or Number, got: Array of type " + i)
        }
    },

    builtin("min", "array") {
      (_, _, array: Val.Arr) =>
        var value = array.value.head
        for (x <- array.value) {
          value.force.prettyName match {
            case "string" =>
              if (value.force.cast[Val.Str].value > x.force.cast[Val.Str].value) {
                value = x
              }
            case "boolean" =>
              if (x.force == Val.Lazy(Val.False).force) {
                value = x
              }
            case "number" =>
              if (value.force.cast[Val.Num].value > x.force.cast[Val.Num].value) {
                value = x
              }
            case i => throw Error.Delegate(
              "Expected Array of type String, Boolean, or Number, got: Array of type " + i)
          }
        }
        value.force
    },

    builtin("minBy", "array", "funct") {
      (_, _, array: Val.Arr, funct: Applyer) =>
        val seqVal = array.value
        funct.apply(seqVal.head).prettyName match{
          case "string" =>
            seqVal.minBy(item => funct.apply(item).cast[Val.Str].value).force
          case "boolean" =>
            if(seqVal.forall( item => item.force.prettyName.equals("boolean")))
              if (seqVal.exists(item => item.force == Val.False)) {
                Val.False
              } else { Val.True }
            else throw Error.Delegate("Received a dirty array")
          case "number" =>
            seqVal.minBy(item => funct.apply(item).cast[Val.Num].value).force
          case i => throw Error.Delegate(
            "Expected Array of type String, Boolean, or Number, got: Array of type " + i)
        }
    },

    builtin("orderBy", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case Val.Arr(array) =>
            orderBy(array, funct)
          case obj: Val.Obj =>
            orderBy(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },

    // TODO: add step param
    builtin("range", "begin", "end") {
      (_, _, begin: Int, end: Int) =>
        Val.Arr((begin to end).map(i => Val.Lazy(Val.Num(i))))
    },

    builtin("replace", "string", "regex", "replacement") {
      (_, _, str: String, reg: String, replacement: String) =>
        reg.r.replaceAllIn(str, replacement)
    },

    // moved from dataformats
    builtinWithDefaults("read",
      "data" -> None,
      "mimeType" -> None,
      "params" -> Some(Expr.Null(0))) { (args, ev) =>
      val data = args("data").cast[Val.Str].value
      val mimeType = args("mimeType").cast[Val.Str].value
      val params = if (args("params") == Val.Null) {
        Library.EmptyObj
      } else {
        args("params").cast[Val.Obj]
      }
      read(dataFormats, data, mimeType, params, ev)
    },

    //TODO add read mediatype
    builtin("readUrl", "url") {
      (_, _, url: String) =>
        url match {
          case str if str.startsWith("classpath://") =>
            val source = io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(str.replaceFirst("classpath://", "")))
            val out =
              try {
                source.mkString
              }
              catch {
                case _: NullPointerException => "null"
              }
            Materializer.reverse(ujsonUtils.parse(out));
          case _ =>
            val out = new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next()
            Materializer.reverse(ujsonUtils.parse(out));
        }
    },

    builtin("scan", "str", "regex") {
      (_, _, str: String, regex: String) =>
        Val.Arr(
          regex.r.findAllMatchIn(str).map(item => {
            Val.Lazy(Val.Arr(
              (0 to item.groupCount).map(i => Val.Lazy(Val.Str(item.group(i))))
            ))
          }).toSeq
        )
    },

    builtin("select", "obj", "path") {
      (ev, fs, obj: Val.Obj, path: String) =>
        select(obj,path,ev,fs)
    },

    builtin("sizeOf", "value") {
      (_, _, value: Val) =>
        value match {
          case Val.Str(s) => s.length()
          case s: Val.Obj => s.getVisibleKeys().size
          case Val.Arr(s) => s.size
          case s: Val.Func => s.params.allIndices.size
          case Val.Null => 0
          case i => throw Error.Delegate("Expected Array, String, or Object, got: " + i.prettyName)
        }
    },

    builtin("splitBy", "str", "regex") {
      (_, _, str: String, regex: String) =>
        Val.Arr(regex.r.split(str).toIndexedSeq.map(item => Val.Lazy(Val.Str(item))))
    },

    builtin("startsWith", "str1", "str2") {
      (_, _, str1: String, str2: String) =>
        str1.toUpperCase().startsWith(str2.toUpperCase());
    },

    builtin("toString", "value") {
      (_, _, value: Val) =>
        convertToString(value)
    },

    builtin("trim", "str") {
      (_, _, str: String) =>
        str.trim()
    },

    builtin("typeOf", "value") {
      (_, _, value: Val) =>
        value match {
          case Val.True | Val.False => "boolean"
          case Val.Null => "null"
          case _: Val.Obj => "object"
          case _: Val.Arr => "array"
          case _: Val.Func => "function"
          case _: Val.Num => "number"
          case _: Val.Str => "string"
        }
    },

    builtin("unzip", "array") {
      (_, _, array: Val.Arr) =>
        val size = array.value.map(
          _.force match {
            case Val.Arr(arr) => arr.size
            case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
          }
        ).max
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        for (i <- 0 until size) {
          val current = collection.mutable.Buffer.empty[Val.Lazy]
          for (x <- array.value) {
            current.append(x.force.asInstanceOf[Val.Arr].value(i))
          }
          out.append(Val.Lazy(Val.Arr(current.toSeq)))
        }
        Val.Arr(out.toSeq)
    },

    builtin("upper", "str") {
      (_, _, str: String) =>
        str.toUpperCase()
    },

    builtin0("uuid") {
      (_, _, _) =>
        val n = 36
        val AlphaNumericString = "0123456789" +
          "abcdefghijklmnopqrstuvxyz"
        val sb = new StringBuilder(n)
        for (i <- 0 until n) {
          if (i.equals(8) || i.equals(13) || i.equals(18) || i.equals(23)) {
            sb.append('-')
          }
          else {
            val index = (AlphaNumericString.length * Math.random()).toInt
            sb.append(AlphaNumericString.charAt(index))
          }
        }
        Val.Lazy(Val.Str(sb.toString())).force
    },

    builtin("valuesOf", "obj") {
      (ev, fs, obj: Val.Obj) =>
        Val.Arr(obj.getVisibleKeys().keySet.map(key => Val.Lazy(obj.value(key, -1)(fs, ev))).toSeq)
    },

    // moved from dataformats
    builtinWithDefaults("write",
      "data" -> None,
      "mimeType" -> None,
      "params" -> Some(Expr.Null(0))) { (args, ev) =>
      val data = args("data")
      val mimeType = args("mimeType").cast[Val.Str].value
      val params = if (args("params") == Val.Null) {
        Library.EmptyObj
      } else {
        args("params").cast[Val.Obj]
      }
      write(dataFormats, data, mimeType, params, ev)
    },

    builtin("zip", "array1", "array2") {
      (_, _, array1: Val.Arr, array2: Val.Arr) =>

        val smallArray = if (array1.value.size <= array2.value.size) array1 else array2
        val bigArray = (if (smallArray == array1) array2 else array1).value
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        for ((v, i) <- smallArray.value.zipWithIndex) {
          val current = collection.mutable.Buffer.empty[Val.Lazy]
          if (smallArray == array1) {
            current.append(v)
            current.append(bigArray(i))
          }
          else {
            current.append(bigArray(i))
            current.append(v)
          }
          out.append(Val.Lazy(Val.Arr(current.toSeq)))
        }
        Val.Arr(out.toSeq)
    },

    // funcs below taken from Std
    builtin("isString", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Str]
    },

    builtin("isBoolean", "v") { (_, _, v: Val) =>
      v == Val.True || v == Val.False
    },

    builtin("isNumber", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Num]
    },

    builtin("isObject", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Obj]
    },

    builtin("isArray", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Arr]
    },

    builtin("isFunction", "v") { (_, _, v: Val) =>
      v.isInstanceOf[Val.Func]
    },

    // moved array to first position
    builtin("foldLeft", "arr", "init", "func") { (_, _, arr: Val.Arr, init: Val, func: Applyer) =>
      var current = init
      for (item <- arr.value) {
        val c = current
        current = func.apply(Val.Lazy(c), item)
      }
      current
    },

    // moved array to first position
    // TODO: can we do this without reverse? has to traverse the collection twice
    builtin("foldRight", "arr", "init", "func") { (_, _, arr: Val.Arr, init: Val, func: Applyer) =>
      var current = init
      for (item <- arr.value.reverse) {
        val c = current
        current = func.apply(item, Val.Lazy(c))
      }
      current
    },

    builtin("parseInt", "str") { (_, _, str: String) =>
      str.toInt
    },

    builtin("parseOctal", "str") { (_, _, str: String) =>
      Integer.parseInt(str, 8)
    },

    builtin("parseHex", "str") { (_, _, str: String) =>
      Integer.parseInt(str, 16)
    },

    // migrated from util.libsonnet
    builtin("parseDouble", "str") { (_, _, str: String) =>
      str.toDouble
    },

    builtin("combine", "first", "second") {
      (ev, fs, first: Val, second: Val) =>
        first match {
          case Val.Str(str) =>
            second match {
              case Val.Str(str2) => Val.Lazy(Val.Str(str.concat(str2))).force
              case Val.Num(num) =>
                Val.Lazy(Val.Str(str.concat(
                  if (Math.ceil(num) == Math.floor(num)) {
                    num.toInt.toString
                  } else {
                    num.toString
                  }
                ))).force
              case i => throw Error.Delegate("Expected String or Number, got: " + i.prettyName)
            }
          case Val.Num(num) =>
            val stringNum = if (Math.ceil(num) == Math.floor(num)) {
              num.toInt.toString
            } else {
              num.toString
            }
            second match {
              case Val.Str(str) => Val.Lazy(Val.Str(stringNum.concat(str))).force
              case Val.Num(num2) =>
                Val.Lazy(Val.Str(stringNum.concat(
                  if (Math.ceil(num2) == Math.floor(num2)) {
                    num2.toInt.toString
                  } else {
                    num2.toString
                  }
                ))).force
              case i => throw Error.Delegate("Expected String or Number, got: " + i.prettyName)
            }
          case Val.Arr(arr) =>
            second match {
              case Val.Arr(arr2) => Val.Arr(arr.concat(arr2))
              case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
            }
          case obj: Val.Obj =>
            val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
            second match {
              case secObj: Val.Obj =>
                out.addAll(obj.getVisibleKeys().map {
                  case (sKey, _) => sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(sKey, -1)(fs, ev))
                }).addAll(secObj.getVisibleKeys().map {
                  case (sKey, _) => sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => secObj.value(sKey, -1)(fs, ev))
                })
                new Val.Obj(out, _ => (), None)
              case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
            }
          case i => throw Error.Delegate(
            "Expected Array, Object, Number, or String, got: " + i.prettyName)
        }
    },

    builtin("remove", "collection", "value") {
      (ev, fs, collection: Val, value: Val) =>
        collection match {
          case Val.Arr(arr) =>
            Val.Arr(arr.collect({
              case arrValue if arrValue.force != value => arrValue
            }))
          case obj: Val.Obj =>
            new Val.Obj(
            value match {
              case Val.Str(str) =>
                scala.collection.mutable.Map(
                  obj.getVisibleKeys().keySet.toSeq.collect({
                    case key if key != str =>
                      key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
                  }): _*)
              case Val.Arr(arr) =>
                scala.collection.mutable.Map(
                  obj.getVisibleKeys().keySet.toSeq.collect({
                    case key if !arr.exists(item => item.force.cast[Val.Str] == Val.Str(key)) =>
                      key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
                  }): _*)
              case i => throw Error.Delegate("Expected String or Array, got: " + i.prettyName)
            }, _ => (), None)
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },

    builtin("removeMatch", "first", "second") {
      (ev, fs, first: Val, second: Val) =>
        first match {
          case Val.Arr(arr) =>
            second match {
              case Val.Arr(arr2) =>
                //unfortunately cannot use diff here because of lazy values
                Val.Arr(arr.filter(arrItem => !arr2.exists(arr2Item => arr2Item.force == arrItem.force)))
              case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
            }
          case obj: Val.Obj =>
            second match {
              case obj2: Val.Obj =>
                new Val.Obj(scala.collection.mutable.Map(
                  obj.getVisibleKeys().keySet.toSeq.collect({
                    case key if !(obj2.containsKey(key) && obj.value(key, -1)(fs, ev) == obj2.value(key, -1)(fs, ev)) =>
                      key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
                  }): _*), _ => (), None)
              case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
            }
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },

    builtin("append", "first", "second") {
      (_, _, arr: Val.Arr, second: Val) =>
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        Val.Arr(out.appendAll(arr.value).append(Val.Lazy(second)).toSeq)
    },

    builtin("prepend", "first", "second") {
      (_, _, arr: Val.Arr, second: Val) =>
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        Val.Arr(out.append(Val.Lazy(second)).appendAll(arr.value).toSeq)
    },

    builtin("reverse", "collection") {
      (ev, fs, collection: Val) =>
        collection match {
          case Val.Str(str) => Val.Lazy(Val.Str(str.reverse)).force
          case Val.Arr(arr) => Val.Arr(arr.reverse)
          case obj: Val.Obj =>
            var result: Seq[(String, Val.Obj.Member)] = Seq()
            obj.getVisibleKeys().foreach(entry => result = result.prepended(
              entry._1 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(entry._1, -1)(fs, ev))
            ))
            new Val.Obj(mutable.LinkedHashMap(result: _*), _ => (), None)
          case i => throw Error.Delegate("Expected Array or Object, got: " + i.prettyName)
        }
    },

    builtin("or", "first", "second") {
      (_, _, first: Val, second: Val) =>
        first match {
          case Val.Null => second
          case _ => first
        }
    }
  ).asJava

  override def modules(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Obj] = Map(
    "xml" -> moduleFrom(
      builtinWithDefaults("flattenContents", "element" -> None, "namespaces" -> Some(Expr.Null(0))) {
        (args, ev) =>
          val element = args("element").cast[Val.Obj]
          val namespaces = if (args("namespaces") == Val.Null) {
            Library.EmptyObj
          } else {
            args("namespaces").cast[Val.Obj]
          }

          val wrapperName = "a"
          val wrapperStop = s"</$wrapperName>"

          val wrapperProperties = scala.collection.mutable.Map[String, Val.Obj.Member]()
          wrapperProperties += ("a" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => element))
          val wrapped = new Val.Obj(wrapperProperties, _ => (), None)

          val xmlProperties = scala.collection.mutable.Map[String, Val.Obj.Member]()
          xmlProperties += ("OmitXmlDeclaration" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Str("true")))
          namespaces.foreachVisibleKey((key, _) => {
            xmlProperties += ("NamespaceDeclarations." + key ->
              Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) =>
                namespaces.value(key, -1)(new FileScope(null, Map.empty), ev)))
          })

          val properties = new Val.Obj(xmlProperties, _ => (), None)

          val written = write(dataFormats, wrapped, "application/xml", properties, ev)

          written.substring(written.indexOf(">") + 1, written.length - wrapperStop.length)
      },
    ),
    "datetime" -> moduleFrom(
      builtin0("now") { (_, _, _) => ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) },

      builtin("parse", "datetime", "inputFormat") { (_, _, datetime: Val, inputFormat: String) =>
        var datetimeObj : ZonedDateTime = null
        inputFormat.toLowerCase match {
          case "timestamp" | "epoch" =>
            var inst : Instant = null
            datetime match{
              case Val.Str(item) => inst = Instant.ofEpochSecond(item.toInt.toLong)
              case Val.Num(item) => inst = Instant.ofEpochSecond(item.toLong)
              case _ => throw Error.Delegate("Expected datetime to be a string or number, got: " + datetime.prettyName)
            }
            datetimeObj = java.time.ZonedDateTime.ofInstant(inst, ZoneOffset.UTC)
          case _ =>
            datetimeObj = try{ //will catch any errors if zone data is missing and default to Z
              java.time.ZonedDateTime.parse(datetime.cast[Val.Str].value, DateTimeFormatter.ofPattern(inputFormat))
            } catch {
              case e: DateTimeException =>
                LocalDateTime.parse(datetime.cast[Val.Str].value, DateTimeFormatter.ofPattern(inputFormat)).atZone(ZoneId.of("Z"))
            }
        }
        datetimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("format", "datetime", "outputFormat") { (_, _, datetime: String, outputFormat: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
      },

      builtin("compare", "datetime", "datetwo") { (_, _, datetimeone: String, datetimetwo: String) =>
          val datetimeObj1 = java.time.ZonedDateTime
            .parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val datetimeObj2 = java.time.ZonedDateTime
            .parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

          datetimeObj1.compareTo(datetimeObj2)
      },

      builtin("plus", "datetime", "period") { (_, _, date: String, period: String) =>
        val datetime = java.time.ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        if (period.contains("T")) {
          datetime.plus(Duration.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
          datetime.plus(Period.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
      },

      builtin("minus", "datetime", "period") { (_, _, date: String, period: String) =>
        val datetime = java.time.ZonedDateTime.parse(date, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        if (period.contains("T")) {
          datetime.minus(Duration.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } else {
          datetime.minus(Period.parse(period)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
      },

      builtin("changeTimeZone", "datetime", "timezone") {
        (_, _, datetime: String, timezone: String) =>
          val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val zoneId = ZoneId.of(timezone)
          val newDateTimeObj = datetimeObj.withZoneSameInstant(zoneId)
          newDateTimeObj.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("toLocalDate", "datetime") { (_, _, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
      },

      builtin("toLocalTime", "datetime") { (_, _, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalTime.format(DateTimeFormatter.ISO_LOCAL_TIME)
      },

      builtin("toLocalDateTime", "datetime") { (_, _, datetime: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        datetimeObj.toLocalDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },

      // newly added
      builtin("daysBetween", "datetime", "datetwo") {
        (_, _, datetimeone: String, datetimetwo: String) =>
          val dateone = java.time.ZonedDateTime
            .parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          val datetwo = java.time.ZonedDateTime
            .parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          ChronoUnit.DAYS.between(dateone, datetwo).abs.toDouble;
      },

      builtin("isLeapYear", "datetime") {
        (_, _, datetime: String) =>
          java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
            .toLocalDate.isLeapYear;
      },

      builtin("atBeginningOfDay", "datetime"){
        (_,_,datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusHours(date.getHour)
              .minusMinutes(date.getMinute)
              .minusSeconds(date.getSecond)
              .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfHour", "datetime"){
        (_,_,datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfMonth", "datetime"){
        (_,_,datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusDays(date.getDayOfMonth-1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfWeek", "datetime"){
        (_,_,datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)

          date.minusDays( if(date.getDayOfWeek.getValue == 7) 0 else date.getDayOfWeek.getValue  )
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("atBeginningOfYear", "datetime"){
        (_,_,datetime: String) =>
          val date = java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
          date.minusMonths(date.getMonthValue-1)
            .minusDays(date.getDayOfMonth-1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("date", "obj") {
        (ev,fs,obj: Val.Obj) =>
          //year, month, dayOfMonth, hour, minute, second, nanoSecond, zoneId
          val out = mutable.Map[String, Val]()
          obj.foreachVisibleKey( (key,_) => out.addOne(key, obj.value(key,-1)(fs,ev)))
          java.time.ZonedDateTime.of(
            out.getOrElse("year",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toInt,
            out.getOrElse("month",Val.Lazy(Val.Num(1)).force).cast[Val.Num].value.toInt,
            out.getOrElse("day",Val.Lazy(Val.Num(1)).force).cast[Val.Num].value.toInt,
            out.getOrElse("hour",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toInt,
            out.getOrElse("minute",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toInt,
            out.getOrElse("second",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toInt,
            0, //out.getOrElse("nanosecond",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toInt TODO?
            ZoneId.of(out.getOrElse("timezone",Val.Lazy(Val.Str("Z")).force).cast[Val.Str].value)
          ).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("today") {
        (_,_,_) =>
          val date = java.time.ZonedDateTime.now()
          date.minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("tomorrow") {
        (_,_,_) =>
          val date = java.time.ZonedDateTime.now()
          date.plusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin0("yesterday") {
        (_,_,_) =>
          val date = java.time.ZonedDateTime.now()
          date.minusDays(1)
            .minusHours(date.getHour)
            .minusMinutes(date.getMinute)
            .minusSeconds(date.getSecond)
            .minusNanos(date.getNano).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

    ),

    "period" -> moduleFrom(
      builtin("between", "datetimeone", "datetimetwo") {
        (_,_, datetimeone: String , datetimetwo: String) =>
          Period.between(
            java.time.ZonedDateTime.parse(datetimeone, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate,
            java.time.ZonedDateTime.parse(datetimetwo, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toLocalDate
          ).toString
      },

      builtin("days", "num") {
        (_,_, num: Int ) =>
          Period.ofDays(num).toString
      },

      builtin("duration", "obj") {
        (ev,fs, obj: Val.Obj ) =>
          val out = mutable.Map[String, Val]()
          obj.foreachVisibleKey( (key,_) => out.addOne(key, obj.value(key,-1)(fs,ev)))
          Duration.ZERO
            .plusDays(out.getOrElse("days",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .plusHours(out.getOrElse("hours",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .plusMinutes(out.getOrElse("minutes",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .plusSeconds(out.getOrElse("seconds",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .toString
      },

      builtin("hours", "num") {
        (_,_, num: Int ) =>
          Duration.ofHours(num).toString
      },

      builtin("minutes", "num") {
        (_,_, num: Int ) =>
          Duration.ofMinutes(num).toString
      },

      builtin("months", "num") {
        (_,_, num: Int ) =>
          Period.ofMonths(num).toString
      },

      builtin("period", "obj") {
        (ev,fs, obj: Val.Obj ) =>
          val out = mutable.Map[String, Val]()
          obj.foreachVisibleKey( (key,_) => out.addOne(key, obj.value(key,-1)(fs,ev)))
          Period.ZERO
            .plusYears(out.getOrElse("years",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .plusMonths(out.getOrElse("months",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .plusDays(out.getOrElse("days",Val.Lazy(Val.Num(0)).force).cast[Val.Num].value.toLong)
            .toString
      },

      builtin("seconds", "num") {
        (_,_, num: Int ) =>
          Duration.ofSeconds(num).toString
      },

      builtin("years", "num") {
        (_,_, num: Int ) =>
          Period.ofYears(num).toString
      },
    ),

    "crypto" -> moduleFrom(
      builtin("hash", "value", "algorithm") {
        (_, _, value: String, algorithm: String) =>
          Crypto.hash(value, algorithm)
      },

      builtin("hmac", "value", "secret", "algorithm") {
        (_, _, value: String, secret: String, algorithm: String) =>
          Crypto.hmac(value, secret, algorithm)
      },

      /**
       * Encrypts the value with specified JDK Cipher Transformation and the provided secret. Converts the encryption
       * to a readable format with Base64
       *
       * @builtinParam value The message to be encrypted.
       *    @types [String]
       * @builtinParam secret The secret used to encrypt the original messsage.
       *    @types [String]
       * @builtinParam transformation The string that describes the operation (or set of operations) to be performed on
       * the given input, to produce some output. A transformation always includes the name of a cryptographic algorithm
       * (e.g., AES), and may be followed by a feedback mode and padding scheme. A transformation is of the form:
       * "algorithm/mode/padding" or "algorithm"
       *    @types [String]
       * @builtinReturn Base64 String value of the encrypted message
       *    @types [String]
       * @changed 2.0.3
       */
      builtin0[Val]("encrypt", "value", "secret", "transformation") {
        (vals, ev, fs) =>
          val valSeq = validate(vals, ev, fs, Array(StringRead, StringRead, StringRead))
          val value = valSeq(0).asInstanceOf[String]
          val secret = valSeq(1).asInstanceOf[String]
          val transformation = valSeq(2).asInstanceOf[String]

          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            Val.Str(Base64.getEncoder.encodeToString(cipher.doFinal(value.getBytes)))

          } else {
            // https://stackoverflow.com/a/52571774/4814697
            val rand: SecureRandom = new SecureRandom()
            val iv = new Array[Byte](cipher.getBlockSize)
            rand.nextBytes(iv)

            cipher.init(Cipher.ENCRYPT_MODE,
              new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
              new IvParameterSpec(iv),
              rand)

            // encrypted data:
            val encryptedBytes = cipher.doFinal(value.getBytes)

            // append Initiation Vector as a prefix to use it during decryption:
            val combinedPayload = new Array[Byte](iv.length + encryptedBytes.length)

            // populate payload with prefix IV and encrypted data
            System.arraycopy(iv, 0, combinedPayload, 0, iv.length)
            System.arraycopy(encryptedBytes, 0, combinedPayload, iv.length, encryptedBytes.length)

            Val.Str(Base64.getEncoder.encodeToString(combinedPayload))
          }

      },

      /**
       * Decrypts the Base64 value with specified JDK Cipher Transformation and the provided secret.
       *
       * @builtinParam value The encrypted message to be decrypted.
       *    @types [String]
       * @builtinParam secret The secret used to encrypt the original messsage.
       *    @types [String]
       * @builtinParam algorithm The algorithm used for the encryption.
       *    @types [String]
       * @builtinParam mode The encryption mode to be used.
       *    @types [String]
       * @builtinParam padding The encryption secret padding to be used
       *    @types [String]
       * @builtinReturn Base64 String value of the encrypted message
       *    @types [String]
       * @changed 2.0.3
       */
      builtin0[Val]("decrypt", "value", "secret", "transformation") {
        (vals, ev,fs) =>
          val valSeq = validate(vals, ev, fs, Array(StringRead, StringRead, StringRead))
          val value = valSeq(0).asInstanceOf[String]
          val secret = valSeq(1).asInstanceOf[String]
          val transformation = valSeq(2).asInstanceOf[String]

          val cipher = Cipher.getInstance(transformation)
          val transformTokens = transformation.split("/")

          // special case for ECB because of java.security.InvalidAlgorithmParameterException: ECB mode cannot use IV
          if (transformTokens.length >= 2 && "ECB".equals(transformTokens(1))) {
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase))
            Val.Str(new String(cipher.doFinal(Base64.getDecoder.decode(value))))

          } else {
            // https://stackoverflow.com/a/52571774/4814697
            // separate prefix with IV from the rest of encrypted data//separate prefix with IV from the rest of encrypted data
            val encryptedPayload = Base64.getDecoder.decode(value)
            val iv = new Array[Byte](cipher.getBlockSize)
            val encryptedBytes = new Array[Byte](encryptedPayload.length - iv.length)
            val rand: SecureRandom = new SecureRandom()

            // populate iv with bytes:
            System.arraycopy(encryptedPayload, 0, iv, 0, iv.length)

            // populate encryptedBytes with bytes:
            System.arraycopy(encryptedPayload, iv.length, encryptedBytes, 0, encryptedBytes.length)

            cipher.init(Cipher.DECRYPT_MODE,
              new SecretKeySpec(secret.getBytes, transformTokens(0).toUpperCase),
              new IvParameterSpec(iv),
              rand)

            Val.Str(new String(cipher.doFinal(encryptedBytes)))
          }
      }
    ),

    "jsonpath" -> moduleFrom(
      builtin("select", "json", "path") {
        (ev, _, json: Val, path: String) =>
          Materializer.reverse(ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      }
    ),

    "regex" -> moduleFrom(
      builtin("regexFullMatch", "expr", "str") {
        (_, _, expr: String, str: String) =>
          Materializer.reverse(Regex.regexFullMatch(expr, str))
      },

      builtin("regexPartialMatch", "expr", "str") {
        (_, _, expr: String, str: String) =>
          Materializer.reverse(Regex.regexPartialMatch(expr, str))
      },

      builtin("regexScan", "expr", "str") {
        (_, _, expr: String, str: String) =>
          Materializer.reverse(Regex.regexScan(expr, str))
      },

      builtin("regexQuoteMeta", "str") {
        (_, _, str: String) =>
          Regex.regexQuoteMeta(str)
      },

      builtin("regexReplace", "str", "pattern", "replace") {
        (_, _, str: String, pattern: String, replace: String) =>
          Regex.regexReplace(str, pattern, replace)
      },

      builtinWithDefaults("regexGlobalReplace", "str" -> None, "pattern" -> None, "replace" -> None) { (args, ev) =>
        val replace = args("replace")
        val str = args("str").asInstanceOf[Val.Str].value
        val pattern = args("pattern").asInstanceOf[Val.Str].value

        replace match {
          case replaceStr: Val.Str => Regex.regexGlobalReplace(str, pattern, replaceStr.value)
          case replaceF: Val.Func =>
            val func = new Function[Value, String] {
              override def apply(t: Value): String = {
                val v = Materializer.reverse(t)
                Applyer(replaceF, ev, null).apply(Val.Lazy(v)) match {
                  case resultStr: Val.Str => resultStr.value
                  case _ => throw Error.Delegate("The result of the replacement function must be a String")
                }
              }
            }
            Regex.regexGlobalReplace(str, pattern, func)

          case _ => throw Error.Delegate("'replace' parameter must be either String or Function")
        }
      }
    ),

    "url" -> moduleFrom(
      builtinWithDefaults("encode",
        "data" -> None,
        "encoding" -> Some(Expr.Str(0, "UTF-8"))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val encoding = args("encoding").cast[Val.Str].value

        java.net.URLEncoder.encode(data, encoding)
      },

      builtinWithDefaults("decode",
        "data" -> None,
        "encoding" -> Some(Expr.Str(0, "UTF-8"))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val encoding = args("encoding").cast[Val.Str].value

        java.net.URLDecoder.decode(data, encoding)
      }
    ),

    "math" -> moduleFrom(
      builtin("abs", "num") {
        (_, _, num: Double) =>
          Math.abs(num);
      },

      // See: https://damieng.com/blog/2014/12/11/sequence-averages-in-scala
      // See: https://gist.github.com/gclaramunt/5710280
      builtin("avg", "array") {
        (_, _, array: Val.Arr) =>
          val (sum, length) = array.value.foldLeft((0.0, 0))({
            case ((sum, length), num) =>
              (num.force match {
                case Val.Num(x) => sum + x
                case i => throw Error.Delegate("Expected Array pf Numbers, got: Array of " + i.prettyName)
              }, 1 + length)
          })
          sum / length
      },

      builtin("ceil", "num") {
        (_, _, num: Double) =>
          Math.ceil(num);
      },

      builtin("floor", "num") {
        (_, _, num: Double) =>
          Math.floor(num);
      },

      builtin("mod", "num1", "num2") {
        (_, _, num1: Double, num2: Double) =>
          num1 % num2;
      },

      builtin("pow", "num1", "num2") {
        (_, _, num1: Double, num2: Double) =>
          Math.pow(num1, num2)
      },

      builtin0("random") {
        (_, _, _) =>
          (0.0 + (1.0 - 0.0) * Random.nextDouble()).doubleValue()
      },

      builtin("randomInt", "num") {
        (_, _, num: Int) =>
          (Random.nextInt((num - 0) + 1) + 0).intValue()
      },

      builtinWithDefaults("round",
        "num" -> None,
        "precision" -> Some(Expr.Num(0, 0))) { (args, ev) =>
        val num = args("num").cast[Val.Num].value
        val prec = args("precision").cast[Val.Num].value.toInt

        if (prec == 0) {
          Math.round(num).intValue()
        } else {
          BigDecimal.valueOf(num).setScale(prec, RoundingMode.HALF_UP).doubleValue()
        }
      },

      builtin("sqrt", "num") {
        (_, _, num: Double) =>
          Math.sqrt(num)
      },

      builtin("sum", "array") {
        (_, _, array: Val.Arr) =>
          array.value.foldLeft(0.0)((sum, value) =>
            value.force match {
              case Val.Num(x) => sum + x
              case i => throw Error.Delegate("Expected Array of Numbers, got: Array of " + i.prettyName)
            }
          )
      },

      // funcs below taken from Std but using Java's Math
      builtin("clamp", "x", "minVal", "maxVal") { (_, _, x: Double, minVal: Double, maxVal: Double) =>
        Math.max(minVal, Math.min(x, maxVal))
      },

      builtin("pow", "x", "n") { (_, _, x: Double, n: Double) =>
        Math.pow(x, n)
      },

      builtin("sin", "x") { (_, _, x: Double) =>
        Math.sin(x)
      },

      builtin("cos", "x") { (_, _, x: Double) =>
        Math.cos(x)
      },

      builtin("tan", "x") { (_, _, x: Double) =>
        Math.tan(x)
      },

      builtin("asin", "x") { (_, _, x: Double) =>
        Math.asin(x)
      },

      builtin("acos", "x") { (_, _, x: Double) =>
        Math.acos(x)
      },

      builtin("atan", "x") { (_, _, x: Double) =>
        Math.atan(x)
      },

      builtin("log", "x") { (_, _, x: Double) =>
        Math.log(x)
      },

      builtin("exp", "x") { (_, _, x: Double) =>
        Math.exp(x)
      },

      builtin("mantissa", "x") { (_, _, x: Double) =>
        x * Math.pow(2.0, -((Math.log(x) / Math.log(2)).toInt + 1))
      },

      builtin("exponent", "x") { (_, _, x: Double) =>
        (Math.log(x) / Math.log(2)).toInt + 1
      }
    ),

    "arrays" -> moduleFrom(
      builtin("countBy", "arr", "funct") {
        (_, _, arr: Val.Arr, funct: Applyer) =>
          var total = 0
          for (x <- arr.value) {
            if (funct.apply(x) == Val.True) {
              total += 1
            }
          }
          total
      },

      builtin("divideBy", "array", "size") {
        (_, _, array: Val.Arr, size: Int) =>
          Val.Arr(array.value.sliding(size, size).map(item => Val.Lazy(Val.Arr(item))).toSeq)
      },

      builtin("drop", "arr", "num") {
        (_, _, arr: Val.Arr, num: Int) =>
          Val.Arr(arr.value.drop(num))
      },

      builtin("dropWhile", "arr", "funct") {
        (_, _, arr: Val.Arr, funct: Applyer) =>
          Val.Arr(arr.value.dropWhile(funct.apply(_) == Val.True))
      },

      builtin("duplicates", "array") {
        (_, _, array: Val.Arr) =>
          val out = mutable.Buffer.empty[Val.Lazy]
          array.value.collect({
            case item if array.value.count(_.force == item.force)>=2 &&
                           !out.exists(_.force == item.force) => out.append(item)
          })
          Val.Arr(out.toSeq)
      },

      builtin("every", "value", "funct") {
        (_, _, value: Val, funct: Applyer) =>
          value match {
            case Val.Arr(arr) => Val.bool(arr.forall(funct.apply(_) == Val.True))
            case Val.Null => Val.Lazy(Val.True).force
            case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
          }
      },

      builtin("firstWith", "arr", "funct") {
        (_, _, arr: Val.Arr, funct: Applyer) =>
          val args = funct.f.params.allIndices.size
          if (args == 2)
            arr.value.zipWithIndex.find(item => funct.apply(item._1, Val.Lazy(Val.Num(item._2))) == Val.True).map(_._1).getOrElse(Val.Lazy(Val.Null)).force
          else if (args == 1)
            arr.value.find(funct.apply(_) == Val.True).getOrElse(Val.Lazy(Val.Null)).force
          else {
            throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
          }
      },

      builtin("deepFlatten", "arr") {
        (_, _, arr: Val.Arr) =>
          Val.Arr(deepFlatten(arr.value))
      },

      builtin("indexOf", "container", "value") {
        (_, _, container: Val, value: Val) =>
          container match {
            case Val.Str(str) => Val.Lazy(Val.Num(str.indexOf(value.cast[Val.Str].value))).force
            case Val.Arr(array) => Val.Lazy(Val.Num(array.indexWhere(_.force == value))).force
            case Val.Null => Val.Lazy(Val.Num(-1)).force
            case i => throw Error.Delegate("Expected String or Array, got: " + i.prettyName)
          }
      },

      builtin("indexWhere", "arr", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          array.value.indexWhere(funct.apply(_) == Val.Lazy(Val.True).force)
      },

      builtin0("join", "arrL", "arryR", "functL", "functR") {
        (vals, ev, fs) =>
          //map the input values
          val valSeq = validate(vals, ev, fs, Array(ArrRead, ArrRead, ApplyerRead, ApplyerRead))
          val arrL = valSeq(0).asInstanceOf[Val.Arr]
          val arrR = valSeq(1).asInstanceOf[Val.Arr]
          val functL = valSeq(2).asInstanceOf[Applyer]
          val functR = valSeq(3).asInstanceOf[Applyer]

          val out = collection.mutable.Buffer.empty[Val.Lazy]

          arrL.value.foreach({
            valueL =>
              val compareL = functL.apply(valueL)
              //append all that match the condition
              out.appendAll(arrR.value.collect({
                case valueR if compareL.equals(functR.apply(valueR)) =>
                  val temp = scala.collection.mutable.Map[String, Val.Obj.Member]()
                  temp += ("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueL.force))
                  temp += ("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueR.force))
                  Val.Lazy(new Val.Obj(temp, _ => (), None))
              }))
          })
          Val.Arr(out.toSeq)
      },

      builtin("lastIndexOf", "container", "value") {
        (_, _, container: Val, value: Val) =>
          container match {
            case Val.Str(str) => Val.Lazy(Val.Num(str.lastIndexOf(value.cast[Val.Str].value))).force
            case Val.Arr(array) => Val.Lazy(Val.Num(array.lastIndexWhere(_.force == value))).force
            case Val.Null => Val.Lazy(Val.Num(-1)).force
            case i => throw Error.Delegate("Expected String or Array, got: " + i.prettyName)
          }
      },

      builtin0("leftJoin", "arrL", "arryR", "functL", "functR") {
        (vals, ev, fs) =>
          //map the input values
          val valSeq = validate(vals, ev, fs, Array(ArrRead, ArrRead, ApplyerRead, ApplyerRead))
          val arrL = valSeq(0).asInstanceOf[Val.Arr]
          val arrR = valSeq(1).asInstanceOf[Val.Arr]
          val functL = valSeq(2).asInstanceOf[Applyer]
          val functR = valSeq(3).asInstanceOf[Applyer]

          //make backup array for leftovers
          var leftoversL = arrL.value

          val out = collection.mutable.Buffer.empty[Val.Lazy]

          arrL.value.foreach({
            valueL =>
              val compareL = functL.apply(valueL)
              //append all that match the condition
              out.appendAll(arrR.value.collect({
                case valueR if compareL.equals(functR.apply(valueR)) =>
                  val temp = scala.collection.mutable.Map[String, Val.Obj.Member]()
                  //remove matching values from the leftOvers arrays
                  leftoversL = leftoversL.filter(item => !item.force.equals(valueL.force))

                  temp += ("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueL.force))
                  temp += ("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueR.force))
                  Val.Lazy(new Val.Obj(temp, _ => (), None))
              }))
          })

          out.appendAll(leftoversL.map(
            leftOver =>
              Val.Lazy(new Val.Obj(
                scala.collection.mutable.Map("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => leftOver.force)),
                _ => (), None))
          ))
          Val.Arr(out.toSeq)
      },

      builtin("occurrences", "arr", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          new Val.Obj(
            scala.collection.mutable.Map(
              array.value
                .groupBy(item => convertToString(funct.apply(item)))
                .map(item => item._1 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Lazy(Val.Num(item._2.size)).force)).toSeq:
              _*),_ => (), None)
      },

      builtin0("outerJoin", "arrL", "arryR", "functL", "functR") {
        (vals, ev, fs) =>
          //map the input values
          val valSeq = validate(vals, ev, fs, Array(ArrRead, ArrRead, ApplyerRead, ApplyerRead))
          val arrL = valSeq(0).asInstanceOf[Val.Arr]
          val arrR = valSeq(1).asInstanceOf[Val.Arr]
          val functL = valSeq(2).asInstanceOf[Applyer]
          val functR = valSeq(3).asInstanceOf[Applyer]

          //make backup array for leftovers
          var leftoversL = arrL.value
          var leftoversR = arrR.value

          val out = collection.mutable.Buffer.empty[Val.Lazy]

          arrL.value.foreach({
            valueL =>
              val compareL = functL.apply(valueL)
              //append all that match the condition
              out.appendAll(arrR.value.collect({
                case valueR if compareL.equals(functR.apply(valueR)) =>
                  val temp = scala.collection.mutable.Map[String, Val.Obj.Member]()
                  //remove matching values from the leftOvers arrays
                  leftoversL = leftoversL.filter(item => !item.force.equals(valueL.force))
                  leftoversR = leftoversR.filter(item => !item.force.equals(valueR.force))

                  temp += ("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueL.force))
                  temp += ("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => valueR.force))
                  Val.Lazy(new Val.Obj(temp, _ => (), None))
              }))
          })

          out.appendAll(leftoversL.map(
            leftOver =>
              Val.Lazy(new Val.Obj(
                scala.collection.mutable.Map("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => leftOver.force)),
                _ => (), None))
          ).appendedAll(leftoversR.map(
            leftOver =>
              Val.Lazy(new Val.Obj(
                scala.collection.mutable.Map("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => leftOver.force)),
                _ => (), None)))
          ))
          Val.Arr(out.toSeq)
      },

      builtin("partition", "arr", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
          val part = array.value.partition(funct.apply(_) == Val.True)
          out += ("success" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(part._1)))
          out += ("failure" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(part._2)))
          new Val.Obj(out, _ => (), None)
      },

      builtin("slice", "arr", "start", "end") {
        (_, _, array: Val.Arr, start: Int, end: Int) =>
          //version commented below is slightly slower
          //Val.Arr(array.value.splitAt(start)._2.splitAt(end-1)._1)
          Val.Arr(
            array.value.zipWithIndex.filter({
              case (_, index) => (index >= start) && (index < end)
            }).map(_._1)
          )
      },

      builtin("some", "value", "funct") {
        (_, _, value: Val, funct: Applyer) =>
          value match {
            case Val.Arr(array) =>
              Val.bool(array.exists(item => funct.apply(item) == Val.True))
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
          }
      },

      builtin("splitAt", "array", "index") {
        (_, _, array: Val.Arr, index: Int) =>
          val split = array.value.splitAt(index)
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()

          out += ("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(split._1)))
          out += ("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(split._2)))
          new Val.Obj(out, _ => (), None)
      },

      builtin("splitWhere", "arr", "funct") {
        (_, _, arr: Val.Arr, funct: Applyer) =>
          val split = arr.value.splitAt(arr.value.indexWhere(funct.apply(_) == Val.True))
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()

          out += ("l" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(split._1)))
          out += ("r" -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(split._2)))
          new Val.Obj(out, _ => (), None)
      },

      builtin("sumBy", "array", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          array.value.foldLeft(0.0)((sum, num) => sum + funct.apply(num).asInstanceOf[Val.Num].value)
      },

      builtin("take", "array", "index") {
        (_, _, array: Val.Arr, index: Int) =>
          Val.Arr(array.value.splitAt(index)._1)
      },

      builtin("takeWhile", "array", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          Val.Arr(array.value.takeWhile(item => funct.apply(item) == Val.True))
      }
    ),

    "binaries" -> moduleFrom(
      builtin("fromBase64", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) => Val.Lazy(Val.Str(new String(Base64.getDecoder.decode(x.toString)))).force
            case Val.Str(x) => Val.Lazy(Val.Str(new String(Base64.getDecoder.decode(x)))).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("fromHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Str(x) => Val.Lazy(Val.Str(
              x.toSeq.sliding(2, 2).map(byte => Integer.parseInt(byte.unwrap, 16).toChar).mkString
            )).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("readLinesWith", "value", "encoding") {
        (_, _, value: String, enc: String) =>
          Val.Arr(
            new String(value.getBytes(), enc).split('\n').toIndexedSeq.collect({
              case str => Val.Lazy(Val.Str(str))
            })
          )
      },

      builtin("toBase64", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if (x % 1 == 0) Val.Lazy(Val.Str(new String(Base64.getEncoder.encode(x.toInt.toString.getBytes())))).force
              else Val.Lazy(Val.Str(new String(Base64.getEncoder.encode(x.toString.getBytes())))).force
            case Val.Str(x) => Val.Lazy(Val.Str(new String(Base64.getEncoder.encode(x.getBytes())))).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("toHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) => Val.Lazy(Val.Str(Integer.toString(x.toInt, 16).toUpperCase())).force
            case Val.Str(x) => Val.Lazy(Val.Str(x.getBytes().map(_.toHexString).mkString.toUpperCase())).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("writeLinesWith", "value", "encoding") {
        (_, _, value: Val.Arr, enc: String) =>
          val str = value.value.map(item => item.force.asInstanceOf[Val.Str].value).mkString("\n") + "\n"
          Val.Lazy(Val.Str(new String(str.getBytes, enc))).force
      }
    ),

    "numbers" -> moduleFrom(
      builtin("fromBinary", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if ("[^2-9]".r.matches(x.toString)) {
                throw Error.Delegate("Expected Binary, got: Number")
              }
              else Val.Lazy(Val.Num(BigInt.apply(x.toLong.toString,2).bigInteger.longValue())).force
            case Val.Str(x) => Val.Lazy(Val.Num(BigInt.apply(x, 2).bigInteger.longValue())).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
          }
      },

      builtin("fromHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if ("[^0-9a-f]".r.matches(x.toString.toLowerCase())) {
                throw Error.Delegate("Expected Binary, got: Number")
              }
              else Val.Lazy(Val.Num(BigInt.apply(x.toLong.toString, 16).bigInteger.longValue())).force;
            case Val.Str(x) => Val.Lazy(Val.Num(BigInt.apply(x, 16).bigInteger.longValue())).force;
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
          }
      },

      builtin("fromRadixNumber", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Num(x) => Val.Lazy(Val.Num(BigInt.apply(x.toLong.toString, num).bigInteger.longValue() )).force
            case Val.Str(x) => Val.Lazy(Val.Num(BigInt.apply(x, num).bigInteger.longValue() )).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
            //null not supported in DW function
          }
      },

      builtin("toBinary", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + x.toLong.abs.toBinaryString)).force
              else Val.Lazy(Val.Str(x.toLong.toBinaryString)).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str(x.toLong.abs.toBinaryString)).force
              else Val.Lazy(Val.Str(x.toLong.toBinaryString)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
          }
      },

      builtin("toHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + x.toLong.abs.toHexString)).force
              else Val.Lazy(Val.Str(x.toLong.toHexString)).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str(x.toLong.abs.toHexString)).force
              else Val.Lazy(Val.Str(x.toLong.toHexString)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
          }
      },

      builtin("toRadixNumber", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + BigInt.apply(x.toLong).toString(num))).force
              else Val.Lazy(Val.Str(BigInt.apply(x.toLong).toString(num))).force
            // Val.Lazy(Val.Str(Integer.toString(x.toInt, num))).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str("-" + BigInt.apply(x.toLong).toString(num))).force
              else Val.Lazy(Val.Str(BigInt.apply(x.toLong).toString(num))).force
            case i => throw Error.Delegate("Expected Binary, got: " + i.prettyName)
            //DW functions does not support null
          }
      }
    ),

    "objects" -> moduleFrom(
      builtin("divideBy", "obj", "num") {
        (ev, fs, obj: Val.Obj, num: Int) =>
          val out = collection.mutable.Buffer.empty[Val.Lazy]

          obj.getVisibleKeys().sliding(num, num).foreach({
            map =>
              val currentObject = collection.mutable.Map[String, Val.Obj.Member]()
              map.foreachEntry((key, _) => currentObject += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))))
              out.append(Val.Lazy(new Val.Obj(currentObject, _ => (), None)))
          })
          Val.Arr(out.toSeq)
      },

      builtin("everyEntry", "value", "funct") {
        (ev, fs, value: Val, funct: Applyer) =>
          value match {
            case obj: Val.Obj =>
              val args = funct.f.params.allIndices.size
              if (args == 2)
                Val.bool(obj.getVisibleKeys().toSeq.forall(key => funct.apply(Val.Lazy(obj.value(key._1, -1)(fs, ev)), Val.Lazy(Val.Str(key._1))) == Val.True))
              else if (args == 1)
                Val.bool(obj.getVisibleKeys().toSeq.forall(key => funct.apply(Val.Lazy(obj.value(key._1, -1)(fs, ev))) == Val.True))
              else {
                throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
              }
            case Val.Null => Val.Lazy(Val.True).force
            case i => throw Error.Delegate("Expected Array, got: " + i.prettyName)
          }
      },

      builtin("mergeWith", "valueOne", "valueTwo") {
        (ev, fs, valueOne: Val, valueTwo: Val) =>
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
          valueOne match {
            case obj: Val.Obj =>
              valueTwo match {
                case obj2: Val.Obj =>
                  obj2.foreachVisibleKey(
                    (key, _) => out += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj2.value(key, -1)(fs, ev)))
                  )
                  val keySet = obj2.getVisibleKeys().keySet
                  obj.foreachVisibleKey(
                    (key, _) => if (!keySet.contains(key)) out += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
                  )
                  new Val.Obj(out, _ => (), None)
                case Val.Null => valueOne
                case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
              }
            case Val.Null =>
              valueTwo match {
                case _: Val.Obj => valueTwo
                case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
              }
            case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
          }
      },

      builtin("someEntry", "value", "funct") {
        (ev, fs, value: Val, funct: Applyer) =>
          value match {
            case obj: Val.Obj =>
              Val.bool(obj.getVisibleKeys().exists(
                item => funct.apply(Val.Lazy(obj.value(item._1, -1)(fs, ev)), Val.Lazy(Val.Str(item._1))) == Val.True
              ))
            case Val.Null => Val.Lazy(Val.False).force
            case i => throw Error.Delegate("Expected Object, got: " + i.prettyName)
          }
      },

      builtin("takeWhile", "obj", "funct") {
        (ev, fs, obj: Val.Obj, funct: Applyer) =>
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
          obj.getVisibleKeys().takeWhile(
            item => funct.apply(Val.Lazy(obj.value(item._1, -1)(fs, ev)), Val.Lazy(Val.Str(item._1))) == Val.True
          ).foreachEntry((key, _) => out += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))))

          new Val.Obj(out, _ => (), None)
      },
    ),

    "strings" -> moduleFrom(
      builtin("appendIfMissing", "str1", "str2") {
        (_, _, value: Val, append: String) =>
          value match {
            case Val.Str(str) =>
              var ret = str
              if (!str.endsWith(append)) {
                ret = str + append
              }
              Val.Lazy(Val.Str(ret)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("camelize", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              //regex fo _CHAR
              val regex = "(_+)([0-9A-Za-z])".r("underscore", "letter")

              //Start string at first non underscore, lower case it
              var temp = value.substring("[^_]".r.findFirstMatchIn(value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"${(m group "letter").toUpperCase()}")
              Val.Lazy(Val.Str(temp)).force;

            case Val.Null =>
              Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("capitalize", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case it
              var temp = value.substring("[0-9A-Za-z]".r.findFirstMatchIn(value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toUpper.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s" ${(m group "two").toUpperCase() + (m group "three").toLowerCase()}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"} ${(m group "start").toUpperCase()}")

              Val.Lazy(Val.Str(temp)).force;

            case Val.Null =>
              Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("charCode", "str") {
        (_, _, str: String) =>
          str.codePointAt(0)
      },

      builtin("charCodeAt", "str", "num") {
        (_, _, str: String, num: Int) =>
          str.codePointAt(num)
      },

      builtin("dasherize", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case it
              var temp = value

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"-${(m group "two") + (m group "three").toLowerCase()}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}-${m group "start"}")

              temp = temp.toLowerCase()

              Val.Lazy(Val.Str(temp)).force;

            case Val.Null =>
              Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("fromCharCode", "num") {
        (_, _, num: Int) =>
          String.valueOf(num.asInstanceOf[Char])
      },

      builtin("isAlpha", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              if ("^[A-Za-z]+$".r.matches(value)) {
                true
              }
              else {
                false
              }
            case Val.Null => false
            case Val.Num(_) => false
            case Val.True | Val.False => true
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("isAlphanumeric", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              if ("^[A-Za-z0-9]+$".r.matches(value)) {
                true
              }
              else {
                false
              }
            case Val.Null => false
            case Val.Num(_) => true
            case Val.True | Val.False => true
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("isLowerCase", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              if ("^[a-z]+$".r.matches(value)) {
                true
              }
              else {
                false
              }
            case Val.Null => false
            case Val.Num(_) => false
            case Val.True | Val.False => true
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("isNumeric", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              if ("^[0-9]+$".r.matches(value)) {
                true
              }
              else {
                false
              }
            case Val.Num(_) => true
            case Val.True | Val.False | Val.Null => false
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("isUpperCase", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              if ("^[A-Z]+$".r.matches(value)) {
                true
              }
              else {
                false
              }
            case Val.Num(_) => false
            case Val.True | Val.False | Val.Null => false
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("isWhitespace", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) => value.trim().isEmpty
            case Val.Num(_) => false
            case Val.True | Val.False | Val.Null => false
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("leftPad", "str", "offset") {
        (_, _, str: Val, offset: Int) =>
          str match {
            case Val.Str(value) =>
              Val.Lazy(Val.Str(("%" + offset + "s").format(value))).force
            case Val.True =>
              Val.Lazy(Val.Str(("%" + offset + "s").format("true"))).force
            case Val.False =>
              Val.Lazy(Val.Str(("%" + offset + "s").format("false"))).force
            case Val.Num(x) =>
              //TODO change to use sjsonnet's Format and DecimalFormat
              Val.Lazy(Val.Str(("%" + offset + "s").format(new DecimalFormat("0.#").format(x)))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("ordinalize", "num") {
        (_, _, num: Val) =>
          (num match { //convert number value to string
            case Val.Null => "null"
            case Val.Str(value) =>
              if ("^[0-9]+$".r.matches(value)) {
                value
              }
              else {
                "X"
              }
            case Val.Num(value) => value.toInt.toString
            case _ => throw Error.Delegate("Expected Number, got: " + num.prettyName)
          }) match { //convert string number to ordinalized string number
            case "null" => Val.Lazy(Val.Null).force
            case "X" => throw Error.Delegate("Expected Number, got: " + num.prettyName)
            case str =>
              if (str.endsWith("11") || str.endsWith("12") || str.endsWith("13")) {
                Val.Lazy(Val.Str(str + "th")).force
              }
              else {
                if (str.endsWith("1")) {
                  Val.Lazy(Val.Str(str + "st")).force
                }
                else if (str.endsWith("2")) {
                  Val.Lazy(Val.Str(str + "nd")).force
                }
                else if (str.endsWith("3")) {
                  Val.Lazy(Val.Str(str + "rd")).force
                }
                else {
                  Val.Lazy(Val.Str(str + "th")).force
                }
              }
          }
      },

      builtin("pluralize", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Str(str) =>
              val comparator = str.toLowerCase()
              val specialSList = List("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
              if (specialSList.contains(comparator)) {
                Val.Lazy(Val.Str(str + "s")).force
              }
              else if (comparator.isEmpty) Val.Lazy(Val.Str("")).force
              else {
                if (comparator.endsWith("y")) {
                  Val.Lazy(Val.Str(str.substring(0, str.length - 1) + "ies")).force
                }
                else if (comparator.endsWith("x")) {
                  Val.Lazy(Val.Str(str + "es")).force
                }
                else {
                  Val.Lazy(Val.Str(str + "s")).force
                }
              }
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected Number, got: " + i.prettyName)
          }
      },

      builtin("prependIfMissing", "str1", "str2") {
        (_, _, value: Val, append: String) =>
          value match {
            case Val.Str(str) =>
              var ret = str
              if (!str.startsWith(append)) {
                ret = append + str
              }
              Val.Lazy(Val.Str(ret)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("repeat", "str", "num") {
        (_, _, str: String, num: Int) =>
          var ret = ""
          for (_ <- 0 until num) {
            ret += str
          }
          Val.Lazy(Val.Str(ret)).force
      },

      builtin("rightPad", "str", "offset") {
        (_, _, value: Val, offset: Int) =>
          value match {
            case Val.Str(str) =>
              Val.Lazy(Val.Str(str.padTo(offset, ' '))).force
            case Val.Num(x) =>
              //TODO change to use sjsonnet's Format and DecimalFormat
              Val.Lazy(Val.Str(new DecimalFormat("0.#").format(x).padTo(offset, ' '))).force
            case Val.True =>
              Val.Lazy(Val.Str("true".padTo(offset, ' '))).force
            case Val.False =>
              Val.Lazy(Val.Str("false".padTo(offset, ' '))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("singularize", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Str(s) =>
              if (s.endsWith("ies"))
                Val.Lazy(Val.Str(s.substring(0, s.length - 3) + "y")).force
              else if (s.endsWith("es"))
                Val.Lazy(Val.Str(s.substring(0, s.length - 2))).force
              else
                Val.Lazy(Val.Str(s.substring(0, s.length - 1))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("substringAfter", "value", "sep") {
        (_, _, value: Val, sep: String) =>
          value match {
            case Val.Str(s) =>
              Val.Lazy(Val.Str(s.substring(
                s.indexOf(sep) match {
                  case -1 => s.length
                  case i => if (sep.equals("")) i else i + 1
                }
              ))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("substringAfterLast", "value", "sep") {
        (_, _, value: Val, sep: String) =>
          value match {
            case Val.Str(s) =>
              val split = s.split(sep)
              if (sep.equals("")) Val.Lazy(Val.Str("")).force
              else if (split.length == 1) Val.Lazy(Val.Str("")).force
              else Val.Lazy(Val.Str(split(split.length - 1))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("substringBefore", "value", "sep") {
        (_, _, value: Val, sep: String) =>
          value match {
            case Val.Str(s) =>
              Val.Lazy(Val.Str(s.substring(0,
                s.indexOf(sep) match {
                  case -1 => 0
                  case i => i
                }
              ))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("substringBeforeLast", "value", "sep") {
        (_, _, value: Val, sep: String) =>
          value match {
            case Val.Str(s) =>
              Val.Lazy(Val.Str(s.substring(0,
                s.lastIndexOf(sep) match {
                  case -1 => 0
                  case i => i
                }
              ))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("underscore", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) =>
              //regex fo _CHAR
              val regex = "([_\\s-]+)([0-9A-Za-z])([A-Z]+|)".r("one", "two", "three")
              val middleRegex = "([a-z])([A-Z])".r("end", "start")

              //Start string at first non underscore, lower case it
              var temp = value.substring("[0-9A-Za-z]".r.findFirstMatchIn(value).map(_.start).toList.head)
              temp = temp.replaceFirst(temp.charAt(0).toString, temp.charAt(0).toLower.toString)

              //replace and uppercase
              temp = regex.replaceAllIn(temp, m => s"_${(m group "two") + (m group "three")}")
              temp = middleRegex.replaceAllIn(temp, m => s"${m group "end"}_${m group "start"}")

              Val.Lazy(Val.Str(temp.toLowerCase)).force;

            case Val.Null =>
              Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("unwrap", "value", "wrapper") {
        (_, _, value: Val, wrapper: String) =>
          value match {
            case Val.Str(str) =>
              val starts = str.startsWith(wrapper)
              val ends = str.endsWith(wrapper)
              if (starts && ends) Val.Lazy(Val.Str(str.substring(0 + wrapper.length, str.length - wrapper.length))).force
              else if (starts) Val.Lazy(Val.Str(str.substring(0 + wrapper.length, str.length) + wrapper)).force
              else if (ends) Val.Lazy(Val.Str(wrapper + str.substring(0, str.length - wrapper.length))).force
              else Val.Lazy(Val.Str(str)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("withMaxSize", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Str(str) =>
              if (str.length <= num) Val.Lazy(Val.Str(str)).force
              else Val.Lazy(Val.Str(str.substring(0, num))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("wrapIfMissing", "value", "wrapper") {
        (_, _, value: Val, wrapper: String) =>
          value match {
            case Val.Str(str) =>
              val ret = new StringBuilder(str)
              if (!str.startsWith(wrapper)) ret.insert(0, wrapper)
              if (!str.endsWith(wrapper)) ret.append(wrapper)
              Val.Lazy(Val.Str(ret.toString())).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      },

      builtin("wrapWith", "value", "wrapper") {
        (_, _, value: Val, wrapper: String) =>
          value match {
            case Val.Str(str) => Val.Lazy(Val.Str(wrapper + str + wrapper)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw Error.Delegate("Expected String, got: " + i.prettyName)
          }
      }
    )
  ).asJava

  def read(dataFormats: DataFormatService, data: String, mimeType: String, params: Val.Obj, ev: EvalScope): Val = {
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val doc = new DefaultDocument(data, new MediaType(supert, subt, paramsAsJava))

    val plugin = dataFormats.thatCanRead(doc)
      .orElseThrow(() => Error.Delegate("No suitable plugin found for mime type: " + mimeType))

    Materializer.reverse(plugin.read(doc))
  }

  def write(dataFormats: DataFormatService, json: Val, mimeType: String, params: Val.Obj, ev: EvalScope): String = {
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val mediaType = new MediaType(supert, subt, paramsAsJava)

    val plugin = dataFormats.thatCanWrite(mediaType, classOf[String])
      .orElseThrow(() => Error.Delegate("No suitable plugin found for mime type: " + mimeType))

    plugin.write(Materializer.apply(json)(ev), mediaType, classOf[String]).getContent
  }

  private def distinctBy(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size

    Val.Arr(
      if (args == 2) { // 2 args
        array.zipWithIndex.distinctBy(item => funct.apply(item._1, Val.Lazy(Val.Num(item._2)))).map(_._1)
      }
      else if (args == 1) { // 1 arg
        array.distinctBy(item => funct.apply(item))
      }
      else {
        throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
      }
    )
  }

  private def distinctBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size

    new Val.Obj(
      if (args == 2) { // 2 args
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.toSeq.distinctBy(outKey =>
            funct.apply(
              Val.Lazy(obj.value(outKey, -1)(fs, ev)),
              Val.Lazy(Val.Str(outKey))
            )).collect(key => key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
          ): _*)
      }
      else if (args == 1) { //1 arg
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.toSeq.distinctBy(outKey =>
            funct.apply(Val.Lazy(obj.value(outKey, -1)(fs, ev)))
          ).collect(key => key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
        ): _*)
      }
      else {
        throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
      }
    ,_ => (), None)
  }

  private def filter(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    Val.Arr(
      if (args == 2) {
        //The three options are below, classic index for loop seems to be the fastest
        /*array.view.zipWithIndex.filter({
          case (item, index) => funct.apply(item, Val.Lazy(Val.Num(index))) == Val.True
        }).map(_._1).toSeq*/
        val out = collection.mutable.Buffer.empty[Val.Lazy]
        for(index <- array.indices){
          val item = array(index)
          if (funct.apply(array(index), Val.Lazy(Val.Num(index))) == Val.True){
            out.append(item)
          }
        }
        out.toSeq
        /*array.indices.collect({
          case index if funct.apply(array(index), Val.Lazy(Val.Num(index))) == Val.True => array(index)
        })*/
      } else if (args == 1)
        array.filter(lazyItem => funct.apply(lazyItem).equals(Val.True))
      else {
        throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
      }
    )
  }

  private def filterObject(obj: Val.Obj, func: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = func.f.params.allIndices.size
    new Val.Obj(
      if (args == 3) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.zipWithIndex.filter({
            case (key,index) => func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))) == Val.True
          }).map(_._1).collect(key => key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
          ).toSeq: _*)
      }
      else if (args == 2) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().view.keySet
            .filter(key => func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))) == Val.True)
            .collect(key => key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))).toSeq: _*)
      }
      else if (args == 1) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().view.keySet
            .filter(key => func.apply(Val.Lazy(obj.value(key, -1)(fs, ev))) == Val.True)
            .collect(key => key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))).toSeq: _*)
      }
      else {
        throw Error.Delegate("Expected embedded function to have between 1 and 3 parameters, received: " + args)
      }, _ => (), None) // end of new object to return
  }

  private def flatMap(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    if (args == 2) { // 2 args
      array.foreach(
        _.force match {
          case Val.Arr(inner) =>
            for(ind <- inner.indices){
              out.append(Val.Lazy(funct.apply(inner(ind), Val.Lazy(Val.Num(ind)))))
            }
          case i => throw Error.Delegate("Expected Array of Arrays, got: Array of " + i.prettyName)
        }
      )
    }
    else if (args == 1) { //  1 arg
      array.foreach(
        _.force match {
          case Val.Arr(inner) => out.appendAll(inner.map(it => Val.Lazy(funct.apply(it))))
          case i => throw Error.Delegate("Expected Array of Arrays, got: Array of " + i.prettyName)
        }
      )
    }
    else {
      throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
    Val.Arr(out.toSeq)
  }

  private def groupBy(s: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    val out = mutable.Map[String, mutable.IndexedBuffer[Val.Lazy]]()
    if (args == 2) {
      for( index <- s.indices){
        val item = s(index)
        val key = convertToString(funct.apply(item, Val.Lazy(Val.Num(index))))
        out.getOrElseUpdate(key, mutable.IndexedBuffer[Val.Lazy]()).addOne(item)
      }
    } else if (args == 1) {
      s.foreach({ item =>
        val key = convertToString(funct.apply(item))
        out.getOrElseUpdate(key, mutable.IndexedBuffer[Val.Lazy]()).addOne(item)
      })
    }
    else {
      throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
    new Val.Obj(out.map(keyVal => (keyVal._1, Library.memberOf(Val.Arr(keyVal._2.toIndexedSeq)))), _ => (), None)
  }

  private def groupBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    val out = mutable.Map[String, mutable.LinkedHashMap[String, Val.Obj.Member]]()
    if (args == 2) {
      obj.foreachVisibleKey((key,_) =>{
        val item = obj.value(key, -1)(fs, ev)
        val functKey = convertToString(funct.apply(Val.Lazy(item), Val.Lazy(Val.Str(key))))
        out.getOrElseUpdate(functKey, mutable.LinkedHashMap[String, Val.Obj.Member]()).addOne(key, Library.memberOf(item))
      })
    }
    else if (args == 1) {
      obj.foreachVisibleKey((key,_)=>{
        val item = obj.value(key, -1)(fs, ev)
        val functKey = convertToString(funct.apply(Val.Lazy(item)))
        out.getOrElseUpdate(functKey, mutable.LinkedHashMap[String, Val.Obj.Member]()).addOne(key, Library.memberOf(item))
      })
    }
    else {
      throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }

    new Val.Obj(out.map(keyVal => (keyVal._1, Library.memberOf(new Val.Obj(keyVal._2, _ => (), None)))), _ => (), None)

  }

  private def map(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    Val.Arr(
      if (args == 2) { //2 args
        array.zipWithIndex.map {
          case (item, index) => Val.Lazy(funct.apply(item, Val.Lazy(Val.Num(index))))
        }
      } else if (args == 1) { // 1 arg
        array.map(item => Val.Lazy(funct.apply(item)))
      }
      else {
        throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
      }
    )
  }

  private def mapObject(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    if (args.equals(3)) {
      for (((key, _), index) <- obj.getVisibleKeys().zipWithIndex) {
        funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))) match {
          case s: Val.Obj =>
            out.addAll(s.getVisibleKeys().map {
              case (sKey, _) => sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev))
            })
          case i => Error.Delegate("Function must return an Object, got: " + i.prettyName)
        }
      }
      new Val.Obj(out, _ => (), None)
    }
    else if (args.equals(2)) {
      for ((key, _) <- obj.getVisibleKeys()) {
        funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))) match {
          case s: Val.Obj =>
            out.addAll(s.getVisibleKeys().map {
              case (sKey, _) => sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev))
            })
          case i => Error.Delegate("Function must return an Object, got: " + i.prettyName)
        }
      }
      new Val.Obj(out, _ => (), None)
    }
    else if (args.equals(1)) {
      for ((key, _) <- obj.getVisibleKeys()) {
        funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev))) match {
          case s: Val.Obj =>
            out.addAll(s.getVisibleKeys().map {
              case (sKey, _) => sKey -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => s.value(sKey, -1)(fs, ev))
            })
          case i => Error.Delegate("Function must return an Object, got: " + i.prettyName)
        }
      }
      new Val.Obj(out, _ => (), None)
    }
    else {
      throw Error.Delegate("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }
  }

  // TODO: change zipWithIndex to indexed for loop
  private def orderBy(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    if (args == 2) {
      Val.Arr(
        array.zipWithIndex.sortBy(
          it => funct.apply(it._1, Val.Lazy(Val.Num(it._2)))
        )(ord = ValOrdering).map(_._1))
    }
    else if (args == 1) {
      Val.Arr(array.sortBy(it => funct.apply(it))(ord = ValOrdering))
    }
    else {
      throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
  }

  // TODO: we're traversing the object twice, needed?
  private def orderBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    var out = scala.collection.mutable.LinkedHashMap.empty[String, Val.Obj.Member]
    for ((item, _) <- obj.getVisibleKeys()) {
      out += (item -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(item, -1)(fs, ev)))
    }
    if (args == 2) {
      new Val.Obj(
        scala.collection.mutable.LinkedHashMap(
          out.toSeq.sortBy(
            item => funct.apply(Val.Lazy(obj.value(item._1, -1)(fs, ev)), Val.Lazy(Val.Str(item._1)))
          )(ord = ValOrdering): _*), _ => (), None)
    }
    else if (args == 1) {
      new Val.Obj(
        scala.collection.mutable.LinkedHashMap(
          out.toSeq.sortBy(
            item => funct.apply(Val.Lazy(obj.value(item._1, -1)(fs, ev)))
          )(ord = ValOrdering): _*), _ => (), None)
    }
    else {
      throw Error.Delegate("Expected embedded function to have 1 or 2 parameters, received: " + args)
    }
  }

  private def mapEntries(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    if (args.equals(3)) {
      out.appendAll(obj.getVisibleKeys().keySet.zipWithIndex.map(
        item => Val.Lazy(funct.apply(Val.Lazy(obj.value(item._1, -1)(fs, ev)), Val.Lazy(Val.Str(item._1)), Val.Lazy(Val.Num(item._2))))
      ))
    }
    else if (args.equals(2)) {
      out.appendAll(obj.getVisibleKeys().keySet.map(
        item => Val.Lazy(funct.apply(Val.Lazy(obj.value(item, -1)(fs, ev)), Val.Lazy(Val.Str(item))))
      ))
    }
    else if (args.equals(1)) {
      out.appendAll(obj.getVisibleKeys().keySet.map(
        item => Val.Lazy(funct.apply(Val.Lazy(obj.value(item, -1)(fs, ev))))
      ))
    }
    else {
      throw Error.Delegate("Expected embedded function to have between 1 and 3 parameters, received: " + args)
    }

    Val.Arr(out.toSeq)
  }

  private def deepFlatten(array: Seq[Val.Lazy]): Seq[Val.Lazy] = {
    array.foldLeft(mutable.Buffer.empty[Val.Lazy])((agg, curr) =>{
      curr.force match {
        case Val.Arr(inner) => agg.appendAll(deepFlatten(inner))
        case _ => agg.append(curr)
      }
    }).toSeq
  }

  private def select(obj: Val.Obj, path: String, ev: EvalScope, fs: FileScope): Val = {
    val arr = path.split("\\.", 2)
    try {
      val objVal = obj.value(arr(0), -1)(fs, ev)
      if (arr.length > 1) {
        objVal match {
          case x: Val.Obj => select(x, arr(1), ev, fs)
          case _ =>  Val.Lazy(Val.Null).force
        }
      }
      else {
        objVal
      }
    } catch {
      case _: Error =>
        Val.Lazy(Val.Null).force
    }
  }

  private def convertToString(value: Val): String = {
    value match {
      case x: Val.Num =>
        val tmp = x.value
        if(tmp.ceil == tmp.floor) tmp.longValue.toString
        else tmp.toString
      case x: Val.Str => x.value
      case Val.Null => "null"
      case Val.True => "true"
      case Val.False => "false"
    }
  }
}

// this assumes that we're comparing same Vals of the same type
object ValOrdering extends Ordering[Val] {
  def compare(x: Val, y: Val): Int =
    x match {
      case Val.Num(value) => Ordering.Double.TotalOrdering.compare(value, y.asInstanceOf[Val.Num].value)
      case Val.Str(value) => Ordering.String.compare(value, y.asInstanceOf[Val.Str].value)
      // TODO: need to convert the Val.Bool to an actual boolean
      case bool: Val.Bool => Ordering.Boolean.compare(x.asInstanceOf, y.asInstanceOf)
      case unsupported: Val => throw Error.Delegate("Expected embedded function to return a String, Number, or Boolean, received: " + unsupported.prettyName)
  }
}


// DEPRECATED
object DSUppercase extends Library {
  override def namespace() = "DS"

  override def libsonnets(): java.util.Set[String] = Set("util").asJava

  // no root functions in the old version
  override def functions(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Func] = Map().asJava

  override def modules(dataFormats: DataFormatService, header: Header): java.util.Map[String, Val.Obj] = Map(
    "ZonedDateTime" -> moduleFrom(
      builtin0("now") { (vals, ev, fs) => Instant.now().toString() },

      builtin("offset", "datetime", "period") { (ev, fs, v1: String, v2: String) =>
        // NOTE: DEMO ONLY (in particular, missing proper error handling)
        val datetime = java.time.ZonedDateTime.parse(v1, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val period = Period.parse(v2)
        datetime.plus(period).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      },

      builtin("format", "datetime", "inputFormat", "outputFormat") {
        (ev, fs, datetime: String, inputFormat: String, outputFormat: String) =>
          val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(inputFormat))
          datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
      },

      builtin0("compare", "datetime1", "format1", "datetime2", "format2") {
        (vals, ev, fs) =>
          val strValSeq = validate(vals, ev, fs, Array(StringRead, StringRead, StringRead, StringRead))
          val datetime1 = strValSeq(0).asInstanceOf[String]
          val format1 = strValSeq(1).asInstanceOf[String]
          val datetime2 = strValSeq(2).asInstanceOf[String]
          val format2 = strValSeq(3).asInstanceOf[String]

          val datetimeObj1 = java.time.ZonedDateTime.parse(datetime1, DateTimeFormatter.ofPattern(format1))
          val datetimeObj2 = java.time.ZonedDateTime.parse(datetime2, DateTimeFormatter.ofPattern(format2))
          datetimeObj1.compareTo(datetimeObj2)
      },

      builtin("changeTimeZone", "datetime", "format", "timezone") {
        (ev, fs, datetime: String, format: String, timezone: String) =>
          val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
          val zoneId = ZoneId.of(timezone)
          val newDateTimeObj = datetimeObj.withZoneSameInstant(zoneId)
          newDateTimeObj.format(DateTimeFormatter.ofPattern(format))
      },

      builtin("toLocalDate", "datetime", "format") { (ev, fs, datetime: String, format: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
        datetimeObj.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
      },

      builtin("toLocalTime", "datetime", "format") { (ev, fs, datetime: String, format: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
        datetimeObj.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
      },

      builtin("toLocalDateTime", "datetime", "format") { (ev, fs, datetime: String, format: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
        datetimeObj.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      }
    ),

    "Formats" -> moduleFrom(
      builtinWithDefaults("read",
        "data" -> None,
        "mimeType" -> None,
        "params" -> Some(Expr.Null(0))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val mimeType = args("mimeType").cast[Val.Str].value
        val params = if (args("params") == Val.Null) {
          Library.EmptyObj
        } else {
          args("params").cast[Val.Obj]
        }
        DSLowercase.read(dataFormats, data, mimeType, params, ev)
      },
      builtinWithDefaults("write",
        "data" -> None,
        "mimeType" -> None,
        "params" -> Some(Expr.Null(0))) { (args, ev) =>
        val data = args("data")
        val mimeType = args("mimeType").cast[Val.Str].value
        val params = if (args("params") == Val.Null) {
          Library.EmptyObj
        } else {
          args("params").cast[Val.Obj]
        }
        DSLowercase.write(dataFormats, data, mimeType, params, ev)
      },

    ),

    "LocalDateTime" -> moduleFrom(
      builtin0("now") { (vs, extVars, wd) =>
        val datetimeObj = java.time.LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
        datetimeObj.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },

      builtin("offset", "datetime", "period") { (ev, fs, v1: String, v2: String) =>
        // NOTE: DEMO ONLY (in particular, missing proper error handling)
        val datetime = java.time.LocalDateTime.parse(v1, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val period = Period.parse(v2)
        datetime.plus(period).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      },

      builtin("format", "datetime", "inputFormat", "outputFormat") {
        (ev, fs, datetime: String, inputFormat: String, outputFormat: String) =>
          val datetimeObj = java.time.LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern(inputFormat))
          datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
      },

      builtin0("compare", "datetime1", "format1", "datetime2", "format2") {
        (vals, ev, fs) =>
          val strValSeq = validate(vals, ev, fs, Array(StringRead, StringRead, StringRead, StringRead))
          val datetime1 = strValSeq(0).asInstanceOf[String]
          val format1 = strValSeq(1).asInstanceOf[String]
          val datetime2 = strValSeq(2).asInstanceOf[String]
          val format2 = strValSeq(3).asInstanceOf[String]

          val datetimeObj1 = java.time.LocalDateTime.parse(datetime1, DateTimeFormatter.ofPattern(format1))
          val datetimeObj2 = java.time.LocalDateTime.parse(datetime2, DateTimeFormatter.ofPattern(format2))
          datetimeObj1.compareTo(datetimeObj2)
      }
    ),

    "Crypto" -> moduleFrom(
      builtin("hash", "value", "algorithm") {
        (ev, fs, value: String, algorithm: String) =>
          Crypto.hash(value, algorithm)
      },
      builtin("hmac", "value", "secret", "algorithm") {
        (ev, fs, value: String, secret: String, algorithm: String) =>
          Crypto.hmac(value, secret, algorithm)
      },
      builtin("encrypt", "value", "password") {
        (ev, fs, value: String, password: String) =>
          Crypto.encrypt(value, password)
      },
      builtin("decrypt", "value", "password") {
        (ev, fs, value: String, password: String) =>
          Crypto.decrypt(value, password)
      },
    ),

    "JsonPath" -> moduleFrom(
      builtin("select", "json", "path") {
        (ev, fs, json: Val, path: String) =>
          Materializer.reverse(ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      },
    ),

    "Regex" -> moduleFrom(
      builtin("regexFullMatch", "expr", "str") {
        (ev, fs, expr: String, str: String) =>
          Materializer.reverse(Regex.regexFullMatch(expr, str))
      },
      builtin("regexPartialMatch", "expr", "str") {
        (ev, fs, expr: String, str: String) =>
          Materializer.reverse(Regex.regexPartialMatch(expr, str))
      },
      builtin("regexScan", "expr", "str") {
        (ev, fs, expr: String, str: String) =>
          Materializer.reverse(Regex.regexScan(expr, str))
      },
      builtin("regexQuoteMeta", "str") {
        (ev, fs, str: String) =>
          Regex.regexQuoteMeta(str)
      },
      builtin("regexReplace", "str", "pattern", "replace") {
        (ev, fs, str: String, pattern: String, replace: String) =>
          Regex.regexReplace(str, pattern, replace)
      },
      builtinWithDefaults("regexGlobalReplace", "str" -> None, "pattern" -> None, "replace" -> None) { (args, ev) =>
        val replace = args("replace")
        val str = args("str").asInstanceOf[Val.Str].value
        val pattern = args("pattern").asInstanceOf[Val.Str].value

        replace match {
          case replaceStr: Val.Str => Regex.regexGlobalReplace(str, pattern, replaceStr.value)
          case replaceF: Val.Func => {
            val func = new Function[Value, String] {
              override def apply(t: Value): String = {
                val v = Materializer.reverse(t)
                Applyer(replaceF, ev, null).apply(Val.Lazy(v)) match {
                  case resultStr: Val.Str => resultStr.value
                  case _ => throw new Error.Delegate("The result of the replacement function must be a String")
                }
              }
            }
            Regex.regexGlobalReplace(str, pattern, func)
          }

          case _ => throw new Error.Delegate("'replace' parameter must be either String or Function")
        }
      },
    ),

    "URL" -> moduleFrom(
      builtinWithDefaults("encode",
        "data" -> None,
        "encoding" -> Some(Expr.Str(0, "UTF-8"))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val encoding = args("encoding").cast[Val.Str].value

        java.net.URLEncoder.encode(data, encoding)
      },
      builtinWithDefaults("decode",
        "data" -> None,
        "encoding" -> Some(Expr.Str(0, "UTF-8"))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val encoding = args("encoding").cast[Val.Str].value

        java.net.URLDecoder.decode(data, encoding)
      },
    )
  ).asJava
}