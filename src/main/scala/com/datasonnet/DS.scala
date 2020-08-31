package com.datasonnet

import java.net.URL
import java.math.{BigDecimal, RoundingMode}
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, Period, ZoneId, ZoneOffset}
import java.util.function.Function
import java.util.{Base64, Scanner}

import com.datasonnet
import com.datasonnet.document.{DefaultDocument, MediaType}
import com.datasonnet.spi.{DataFormatService, Library, ujsonUtils}
import sjsonnet.Expr.Member.Visibility
import sjsonnet.ReadWriter.{ApplyerRead, ArrRead, StringRead}
import sjsonnet.Std.{builtin, builtinWithDefaults, _}
import sjsonnet.{Applyer, Error, EvalScope, Expr, FileScope, Materializer, Val}
import ujson.Value

import scala.util.Random

object DS extends Library {

  override def namespace() = "ds"

  override def libsonnets(): Set[String] = Set("util")

  override def functions(dataFormats: DataFormatService): Map[String, Val.Func] = Map(
    builtin("contains", "container", "value") {
      (_, _, container: Val, value: Val) =>
        container match {
          // See: scala.collection.IterableOnceOps.exists
          case Val.Arr(array) =>
            array.exists(_.force == value)
          case Val.Str(s) =>
            value.cast[Val.Str].value.r.findAllMatchIn(s).nonEmpty;
          case _ => throw new IllegalArgumentException(
            "Expected Array or String, got: " + container.prettyName);
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
          case Val.Arr(array) =>
            filter(array, funct)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw new IllegalArgumentException(
            "Expected Array , got: " + i.prettyName);
        }
    },

    builtin("filterObject", "obj", "func") {
      (ev, fs, value: Val, func: Applyer) =>
        value match {
          case obj: Val.Obj =>
            filterObject(obj, func, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case i => throw new IllegalArgumentException(
            "Expected Object, got: " + i.prettyName);
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
          case _ => throw new IllegalArgumentException(
            "Expected Array or String, got: " + container.prettyName);
        }
    },

    builtin("flatMap", "array", "funct") {
      (_, _, array: Val, funct: Applyer) =>
        array match {
          case Val.Arr(s) =>
            flatMap(s, funct)
          case Val.Null => Val.Lazy(Val.Null).force
          case _ => throw new IllegalArgumentException(
            "Expected Array, got: " + array.prettyName);
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
                case _ => throw new IllegalArgumentException(
                  "Expected Array, got: " + innerArray.force.prettyName);
              }
            }
            Val.Arr(out.toSeq)
          case Val.Null => Val.Lazy(Val.Null).force
          case _ => throw new IllegalArgumentException(
            "Expected Array, got: " + array.prettyName);
        }
    },

    builtin("distinctBy", "container", "funct") {
      (ev, fs, container: Val, funct: Applyer) =>
        container match {
          case Val.Arr(arr) =>
            distinctBy(arr, funct)
          case obj: Val.Obj =>
            distinctBy(obj, funct, ev, fs)
          case i => throw new IllegalArgumentException(
            "Expected Array or Object, got: " + i.prettyName);
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
          case _ => throw new IllegalArgumentException(
            "Expected Array or Object, got: " + container.prettyName);
        }
    },

    builtin("isBlank", "value") {
      (_, _, value: Val) =>
        value match {
          case Val.Str(s) => s.trim().isEmpty
          case Val.Null => true
          case _ => throw new IllegalArgumentException(
            "Expected String, got: " + value.prettyName);
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
          case _ => throw new IllegalArgumentException(
            "Expected String, Array, or Object, got: " + container.prettyName);
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
            case i => throw new IllegalArgumentException(
              "Expected String, Number, Boolean, got: " + i.prettyName);
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
          case _ => throw new IllegalArgumentException(
            "Expected Array, got: " + array.prettyName);
        }
    },

    builtin("mapEntries", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case obj: Val.Obj =>
            mapEntries(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case _ => throw new IllegalArgumentException(
            "Expected Object, got: " + value.prettyName);
        }
    },

    builtin("mapObject", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case obj: Val.Obj =>
            mapObject(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case _ => throw new IllegalArgumentException(
            "Expected Object, got: " + value.prettyName);
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
            case i => throw new IllegalArgumentException(
              "Array must be of type string,boolean, or number; got: " + i);
          }
        }
        value.force
    },

    builtin("maxBy", "array", "funct") {
      (_, _, array: Val.Arr, funct: Applyer) =>
        var value = array.value.head
        val compareType = funct.apply(value).prettyName
        for (x <- array.value) {
          compareType match {
            case "string" =>
              if (funct.apply(value).toString < funct.apply(x).toString) {
                value = x
              }
            case "boolean" =>
              if (funct.apply(x) == Val.Lazy(Val.True).force) {
                value = x
              }
            case "number" =>
              if (funct.apply(value).cast[Val.Num].value < funct.apply(x).cast[Val.Num].value) {
                value = x
              }
            case i => throw new IllegalArgumentException(
              "Array must be of type string,boolean, or number; got: " + i);
          }
        }
        value.force
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
            case i => throw new IllegalArgumentException(
              "Array must be of type string,boolean, or number; got: " + i);
          }
        }
        value.force
    },

    builtin("minBy", "array", "funct") {
      (_, _, array: Val.Arr, funct: Applyer) =>
        var value = array.value.head
        val compareType = funct.apply(value).prettyName
        for (x <- array.value) {
          compareType match {
            case "string" =>
              if (funct.apply(value).cast[Val.Str].value > funct.apply(x).cast[Val.Str].value) {
                value = x
              }
            case "boolean" =>
              if (funct.apply(x) == Val.Lazy(Val.False).force) {
                value = x
              }
            case "number" =>
              if (funct.apply(value).cast[Val.Num].value > funct.apply(x).cast[Val.Num].value) {
                value = x
              }
            case i => throw new IllegalArgumentException(
              "Array must be of type string,boolean, or number; got: " + i);
          }
        }
        value.force
    },

    builtin("orderBy", "value", "funct") {
      (ev, fs, value: Val, funct: Applyer) =>
        value match {
          case Val.Arr(array) =>
            orderBy(array, funct)
          case obj: Val.Obj =>
            orderBy(obj, funct, ev, fs)
          case Val.Null => Val.Lazy(Val.Null).force
          case _ => throw new IllegalArgumentException(
            "Expected Array or Object got: " + value.prettyName);
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
        Library.emptyObj
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
            val source = io.Source.fromInputStream(getClass.getClassLoader.getResourceAsStream(str.replaceFirst("classpath://","")))
            val out =
              try { source.mkString }
              catch { case ex: NullPointerException => "null"}
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

    builtin("sizeOf", "value") {
      (_, _, value: Val) =>
        value match {
          case Val.Str(s) => s.length()
          case s: Val.Obj => s.getVisibleKeys().size
          case Val.Arr(s) => s.size
          case s: Val.Func => s.params.allIndices.size
          case Val.Null => 0
          case _ => throw new IllegalArgumentException(
            "Expected Array, String, Object got: " + value.prettyName);
        }
    },

    builtin("splitBy", "str", "regex") {
      (_, _, str: String, regex: String) =>
        Val.Arr(regex.r.split(str).map(item => Val.Lazy(Val.Str(item))))
    },

    builtin("startsWith", "str1", "str2") {
      (_, _, str1: String, str2: String) =>
        str1.toUpperCase().startsWith(str2.toUpperCase());
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
        var size = array.value.map(
          _.force match {
            case Val.Arr(arr) => arr.size
            case i => throw new IllegalArgumentException(
              "Expected Array, got: " + i.prettyName);
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
        Library.emptyObj
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
    builtin("isString", "v"){ (ev, fs, v: Val) =>
      v.isInstanceOf[Val.Str]
    },

    builtin("isBoolean", "v"){ (ev, fs, v: Val) =>
      v == Val.True || v == Val.False
    },

    builtin("isNumber", "v"){ (ev, fs, v: Val) =>
      v.isInstanceOf[Val.Num]
    },

    builtin("isObject", "v"){ (ev, fs, v: Val) =>
      v.isInstanceOf[Val.Obj]
    },

    builtin("isArray", "v"){ (ev, fs, v: Val) =>
      v.isInstanceOf[Val.Arr]
    },

    builtin("isFunction", "v"){ (ev, fs, v: Val) =>
      v.isInstanceOf[Val.Func]
    },

    // moved array to first position
    builtin("foldLeft", "arr", "func", "init"){ (ev, fs, arr: Val.Arr, func: Applyer, init: Val) =>
      var current = init
      for(item <- arr.value){
        val c = current
        current = func.apply(Val.Lazy(c), item)
      }
      current
    },

    // moved array to first position
    builtin("foldRight", "arr", "func", "init"){ (ev, fs, arr: Val.Arr, func: Applyer, init: Val) =>
      var current = init
      for(item <- arr.value.reverse){
        val c = current
        current = func.apply(item, Val.Lazy(c))
      }
      current
    },

    builtin("parseInt", "str"){ (ev, fs, str: String) =>
      str.toInt
    },

    builtin("parseOctal", "str"){ (ev, fs, str: String) =>
      Integer.parseInt(str, 8)
    },

    builtin("parseHex", "str"){ (ev, fs, str: String) =>
      Integer.parseInt(str, 16)
    },

    // migrated from util.libsonnet
    builtin("parseDouble", "str"){ (ev, fs, str: String) =>
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
                  if(Math.ceil(num) == Math.floor(num)){num.toInt.toString} else {num.toString}
                ))).force
              case i => throw new IllegalArgumentException(
                "Expected String, Number, got: " + i.prettyName)
            }
          case Val.Num(num) =>
            val stringNum = if(Math.ceil(num) == Math.floor(num)){num.toInt.toString} else {num.toString}
            second match {
              case Val.Str(str) => Val.Lazy(Val.Str(stringNum.concat(str))).force
              case Val.Num(num2) =>
                Val.Lazy(Val.Str(stringNum.concat(
                  if(Math.ceil(num2) == Math.floor(num2)){num2.toInt.toString} else {num2.toString}
                ))).force
              case i => throw new IllegalArgumentException(
                "Expected String, Number, got: " + i.prettyName)
            }
          case Val.Arr(arr) =>
            second match {
              case Val.Arr(arr2) => Val.Arr(arr.concat(arr2))
              case i => throw new IllegalArgumentException(
                "Expected Array, got: " + i.prettyName)
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
              case i => throw new IllegalArgumentException(
                "Expected Object, got: " + i.prettyName)
            }
          case i => throw new IllegalArgumentException(
            i.prettyName + " is not a valid type.")
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
            value match {
              case Val.Str(str) =>
                new Val.Obj(scala.collection.mutable.Map(
                  obj.getVisibleKeys().keySet.toSeq.collect({
                    case key if key != str =>
                      key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
                  }): _*), _ => (), None)
              case i => throw new IllegalArgumentException(
                "Expected String, got: " + i.prettyName)
            }
          case i => throw new IllegalArgumentException(
            "Expected Array or Object, got: " + i.prettyName)
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
              case i => throw new IllegalArgumentException(
                "Expected Array, got: " + i.prettyName)
            }
          case obj: Val.Obj =>
            second match {
              case obj2: Val.Obj =>
                new Val.Obj(scala.collection.mutable.Map(
                  obj.getVisibleKeys().keySet.toSeq.collect({
                    case key if !(obj2.containsKey(key) && obj.value(key, -1)(fs, ev) == obj2.value(key, -1)(fs, ev)) =>
                      key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
                  }): _*), _ => (), None)
              case i => throw new IllegalArgumentException(
                "Expected Object, got: " + i.prettyName)
            }
          case i => throw new IllegalArgumentException(
            "Expected Array or Object, got: " + i.prettyName)
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
    }
  )

  override def modules(dataFormats: DataFormatService): Map[String, Val.Obj] = Map(
    "datetime" -> moduleFrom(
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
      },

      // newly added
      builtin("daysBetween", "datetime", "datetwo") {
        (_, _, datetimeone: String, datetimetwo: String) =>
          val dateone = java.time.ZonedDateTime
            .parse(datetimeone, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
          val datetwo = java.time.ZonedDateTime
            .parse(datetimetwo, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
          Val.Num(ChronoUnit.DAYS.between(dateone, datetwo)).value.abs;
      },

      builtin("isLeapYear", "datetime") {
        (_, _, datetime: String) =>
          java.time.ZonedDateTime
            .parse(datetime, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSVV"))
            .toLocalDate.isLeapYear;
      }

    ),

    "localdatetime" -> moduleFrom(
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

    "crypto" -> moduleFrom(
      builtin("hash", "value", "algorithm") {
        (ev, fs, value: String, algorithm: String) =>
          Crypto.hash(value, algorithm)
      },

      builtin("hmac", "value", "secret", "algorithm") {
        (ev, fs, value: String, secret: String, algorithm: String) =>
          datasonnet.Crypto.hmac(value, secret, algorithm)
      }
    ),

    "jsonpath" -> moduleFrom(
      builtin("select", "json", "path") {
        (ev, fs, json: Val, path: String) =>
          Materializer.reverse(ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      }
    ),

    "regex" -> moduleFrom(
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
                  case _  => throw new Error.Delegate("The result of the replacement function must be a String")
                }
              }
            }
            Regex.regexGlobalReplace(str, pattern, func)
          }

          case _ => throw new Error.Delegate("'replace' parameter must be either String or Function")
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
                case i => throw new IllegalArgumentException(
                  "Expected Array of Numbers got: Array of " + i.prettyName)
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

      builtin("randomint", "num") {
        (_, _, num: Int) =>
          (Random.nextInt((num - 0) + 1) + 0).intValue()
      },

      builtinWithDefaults("round",
        "num" -> None,
      "precision" -> Some(Expr.Num(0, 0))){ (args, ev) =>
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
              case i => throw new IllegalArgumentException(
                "Expected Array of Numbers, got: " + i)
            }
          )
      },

      // funcs below taken from Std but using Java's Math
      builtin("clamp", "x", "minVal", "maxVal"){ (ev, fs, x: Double, minVal: Double, maxVal: Double) =>
        Math.max(minVal, Math.min(x, maxVal))
      },

      builtin("pow", "x", "n"){ (ev, fs, x: Double, n: Double) =>
        Math.pow(x, n)
      },

      builtin("sin", "x"){ (ev, fs, x: Double) =>
        Math.sin(x)
      },

      builtin("cos", "x"){ (ev, fs, x: Double) =>
        Math.cos(x)
      },

      builtin("tan", "x"){ (ev, fs, x: Double) =>
        Math.tan(x)
      },

      builtin("asin", "x"){ (ev, fs, x: Double) =>
        Math.asin(x)
      },

      builtin("acos", "x"){ (ev, fs, x: Double) =>
        Math.acos(x)
      },

      builtin("atan", "x"){ (ev, fs, x: Double) =>
        Math.atan(x)
      },

      builtin("log", "x"){ (ev, fs, x: Double) =>
        Math.log(x)
      },

      builtin("exp", "x"){ (ev, fs, x: Double) =>
        Math.exp(x)
      },

      builtin("mantissa", "x"){ (ev, fs, x: Double) =>
        val value = x
        val exponent = (Math.log(value) / Math.log(2)).toInt + 1
        val mantissa = value * Math.pow(2.0, -exponent)
        mantissa
      },

      builtin("exponent", "x"){ (ev, fs, x: Double) =>
        val value = x
        val exponent = (Math.log(value) / Math.log(2)).toInt + 1
        val mantissa = value * Math.pow(2.0, -exponent)
        exponent
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

      builtin("every", "value", "funct") {
        (_, _, value: Val, funct: Applyer) =>
          value match {
            case Val.Arr(arr) => Val.bool(arr.forall(funct.apply(_) == Val.True))
            case Val.Null => Val.Lazy(Val.True).force
            case i => throw new IllegalArgumentException(
              "Expected Array, got: " + i.prettyName)
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
            throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
          }
      },

      builtin("indexOf", "array", "value") {
        (_, _, array: Val.Arr, value: Val) =>
          array.value.indexWhere(_.force == value)
      },

      builtin("indexWhere", "arr", "funct") {
        (_, _, array: Val.Arr, funct: Applyer) =>
          array.value.indexWhere(funct.apply(_) == Val.Lazy(Val.True).force)
      },

      builtin0("join", "arrL", "arryR", "functL", "functR"){
        (vals, ev,fs) =>
          //map the input values
          val valSeq = validate(vals, ev, fs, Array(ArrRead, ArrRead, ApplyerRead, ApplyerRead))
          val arrL = valSeq(0).asInstanceOf[Val.Arr]
          val arrR = valSeq(1).asInstanceOf[Val.Arr]
          val functL = valSeq(2).asInstanceOf[Applyer]
          val functR = valSeq(3).asInstanceOf[Applyer]

          val out = collection.mutable.Buffer.empty[Val.Lazy]

          arrL.value.foreach({
            valueL => val compareL = functL.apply(valueL)
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

      builtin0("leftJoin", "arrL", "arryR", "functL", "functR"){
        (vals, ev,fs) =>
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
            valueL => val compareL = functL.apply(valueL)
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

      builtin0("outerJoin", "arrL", "arryR", "functL", "functR"){
        (vals, ev,fs) =>
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
            valueL => val compareL = functL.apply(valueL)
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
            case i => throw new IllegalArgumentException(
              "Expected Array, got: " + i.prettyName);
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
            case x => throw new IllegalArgumentException(
              "Expected String, got: " + x.prettyName);
          }
      },

      builtin("fromHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Str(x) => Val.Lazy(Val.Str(
              x.toSeq.sliding(2, 2).map(byte => Integer.parseInt(byte.unwrap, 16).toChar).mkString
            )).force
            case x => throw new IllegalArgumentException(
              "Expected String, got: " + x.prettyName);
          }
      },

      builtin("readLinesWith", "value", "encoding") {
        (_, _, value: String, enc: String) =>
          Val.Arr(
            new String(value.getBytes(), enc).split('\n').collect({
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
            case x => throw new IllegalArgumentException(
              "Expected String, got: " + x.prettyName);
          }
      },

      builtin("toHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) => Val.Lazy(Val.Str(Integer.toString(x.toInt, 16).toUpperCase())).force
            case Val.Str(x) => Val.Lazy(Val.Str(x.getBytes().map(_.toHexString).mkString.toUpperCase())).force
            case x => throw new IllegalArgumentException(
              "Expected String, got: " + x.prettyName);
          }
      },

      builtin("writeLinesWith", "value", "encoding") {
        (_, _, value: Val.Arr, enc: String) =>
          val str = value.value.map(item => item.force.asInstanceOf[Val.Str].value).mkString("\n") + "\n"
          Val.Lazy(Val.Str(new String(str.getBytes, enc))).force
      }
    ),

    // TODO currently limited to 32 bit value
    "numbers" -> moduleFrom(
      builtin("fromBinary", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if ("[^2-9]".r.matches(x.toString)) {
                throw new IllegalArgumentException(
                  "Expected Binary, got: Number")
              }
              else Val.Lazy(Val.Num(Integer.parseInt(x.toInt.toString, 2))).force
            //Val.Lazy(Val.Num( java.lang.Long.parseLong(x.toLong.toString,2))).force
            case Val.Str(x) => Val.Lazy(Val.Num(Integer.parseInt(x, 2))).force;
            //Val.Lazy(Val.Num( java.lang.Long.parseLong(x,2))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
          }
      },

      builtin("fromHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if ("[^0-9a-f]".r.matches(x.toString.toLowerCase())) {
                throw new IllegalArgumentException(
                  "Expected Binary, got: Number")
              }
              else Val.Lazy(Val.Num(Integer.parseInt(x.toInt.toString.toLowerCase(), 16))).force;
            case Val.Str(x) => Val.Lazy(Val.Num(Integer.parseInt(x.toLowerCase(), 16))).force;
            case Val.Null => Val.Lazy(Val.Null).force
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
          }
      },

      builtin("fromRadixNumber", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Num(x) => Val.Lazy(Val.Num(Integer.parseInt(x.toInt.toString.toLowerCase(), num))).force;
            case Val.Str(x) => Val.Lazy(Val.Num(Integer.parseInt(x.toLowerCase(), num))).force;
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
            //null not supported in DW function
          }
      },

      builtin("toBinary", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + x.toInt.abs.toBinaryString)).force
              else Val.Lazy(Val.Str(x.toInt.toBinaryString)).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str(x.toInt.abs.toBinaryString)).force
              else Val.Lazy(Val.Str(x.toInt.toBinaryString)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
          }
      },

      builtin("toHex", "value") {
        (_, _, value: Val) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + x.toInt.abs.toHexString)).force
              else Val.Lazy(Val.Str(x.toInt.toHexString)).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str(x.toInt.abs.toHexString)).force
              else Val.Lazy(Val.Str(x.toInt.toHexString)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
          }
      },

      builtin("toRadixNumber", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Num(x) =>
              if (x < 0) Val.Lazy(Val.Str("-" + Integer.toString(x.toInt.abs, num))).force
              else Val.Lazy(Val.Str(Integer.toString(x.toInt, num))).force
            case Val.Str(x) =>
              if (x.startsWith("-")) Val.Lazy(Val.Str("-" + Integer.toString(x.toInt.abs, num))).force
              else Val.Lazy(Val.Str(Integer.toString(x.toInt, num))).force
            case x => throw new IllegalArgumentException(
              "Expected Binary, got: " + x.prettyName);
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
                throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
              }
            case Val.Null => Val.Lazy(Val.True).force
            case i => throw new IllegalArgumentException(
              "Expected Array, got: " + i.prettyName);
          }
      },

      builtin("mergeWith", "valueOne", "valueTwo") {
        (ev, fs, valueOne: Val, valueTwo: Val) =>
          val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
          valueOne match {
            case obj: Val.Obj =>
              valueTwo match {
                case obj2: Val.Obj =>
                  obj2.getVisibleKeys().foreachEntry(
                    (key, _) => out += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj2.value(key, -1)(fs, ev)))
                  )
                  val keySet = obj2.getVisibleKeys().keySet
                  obj.getVisibleKeys().foreachEntry(
                    (key, _) => if (!keySet.contains(key)) out += (key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
                  )
                  new Val.Obj(out, _ => (), None)
                case Val.Null => valueOne
                case i => throw new IllegalArgumentException(
                  "Expected Object, got: " + i.prettyName);
              }
            case Val.Null =>
              valueTwo match {
                case _: Val.Obj => valueTwo
                case i => throw new IllegalArgumentException(
                  "Expected Object, got: " + i.prettyName);
              }
            case i => throw new IllegalArgumentException(
              "Expected Object, got: " + i.prettyName);
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
            case i => throw new IllegalArgumentException(
              "Expected Object, got: " + i.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + value.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
          }
      },

      builtin("isWhitespace", "str") {
        (_, _, str: Val) =>
          str match {
            case Val.Str(value) => value.trim().isEmpty
            case Val.Num(_) => false
            case Val.True | Val.False | Val.Null => false
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + str.prettyName)
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
            case _ => throw new IllegalArgumentException(
              "Expected Number, got: " + num.prettyName)
          }) match { //convert string number to ordinalized string number
            case "null" => Val.Lazy(Val.Null).force
            case "X" => throw new IllegalArgumentException(
              "Expected Number, got: " + num.prettyName)
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
            case _ => throw new IllegalArgumentException(
              "Expected Number, got: " + value.prettyName)
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + value.prettyName);
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
            case _ => throw new IllegalArgumentException(
              "Expected String, got: " + value.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case _ => throw new IllegalArgumentException(
              "Expected String got: " + str.prettyName);
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
          }
      },

      builtin("withMaxSize", "value", "num") {
        (_, _, value: Val, num: Int) =>
          value match {
            case Val.Str(str) =>
              if (str.length <= num) Val.Lazy(Val.Str(str)).force
              else Val.Lazy(Val.Str(str.substring(0, num))).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
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
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
          }
      },

      builtin("wrapWith", "value", "wrapper") {
        (_, _, value: Val, wrapper: String) =>
          value match {
            case Val.Str(str) => Val.Lazy(Val.Str(wrapper + str + wrapper)).force
            case Val.Null => Val.Lazy(Val.Null).force
            case i => throw new IllegalArgumentException(
              "Expected String, got: " + i.prettyName)
          }
      }
    )
  )

  private def read(dataFormats: DataFormatService, data: String, mimeType: String, params: Val.Obj, ev: EvalScope): Val = {
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val doc = new DefaultDocument(data, new MediaType(supert, subt, paramsAsJava))

    val plugin = dataFormats.thatAccepts(doc)
      .orElseThrow(() => Error.Delegate("No suitable plugin found for mime type: " + mimeType))

    Materializer.reverse(plugin.read(doc))
  }

  private def write(dataFormats: DataFormatService, json: Val, mimeType: String, params: Val.Obj, ev: EvalScope): String = {
    val Array(supert, subt) = mimeType.split("/", 2)
    val paramsAsJava = ujsonUtils.javaObjectFrom(ujson.read(Materializer.apply(params)(ev)).obj).asInstanceOf[java.util.Map[String, String]]
    val mediaType = new MediaType(supert, subt, paramsAsJava)

    val plugin = dataFormats.thatProduces(mediaType, classOf[String])
      .orElseThrow(() => Error.Delegate("No suitable plugin found for mime type: " + mimeType))

    plugin.write(Materializer.apply(json)(ev), mediaType, classOf[String]).getContent
  }

  private def distinctBy(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    val out = collection.mutable.Buffer.empty[Val.Lazy]

    if (args == 2) { // 2 args
      array.zipWithIndex.foreach(
        item =>
          if (!out.zipWithIndex.map { // out array does not contain item
            case (outItem, outIndex) => funct.apply(outItem, Val.Lazy(Val.Num(outIndex)))
          }.contains(funct.apply(item._1, Val.Lazy(Val.Num(item._2))))) {
            out.append(item._1)
          }
      )
    }
    else if (args == 1) { // 1 arg
      array.foreach(
        item =>
          if (!out.map(funct.apply(_)).contains(funct.apply(item))) {
            out.append(item)
          }
      )
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }

    Val.Arr(out.toSeq)
  }

  private def distinctBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()

    if (args == 2) { // 2 args
      obj.getVisibleKeys().keySet.foreach(
        key => {
          val outObj = new Val.Obj(out, _ => (), None)
          if (!outObj.getVisibleKeys().keySet.map(outKey =>
            funct.apply(
              Val.Lazy(outObj.value(outKey, -1)(fs, ev)),
              Val.Lazy(Val.Str(outKey))
            )
          ).contains(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))))) {
            out.+=(key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
          }
        }
      )
    }
    else if (args == 1) { //1 arg
      obj.getVisibleKeys().keySet.foreach(
        key => {
          val outObj = new Val.Obj(out, _ => (), None)
          if (!outObj.getVisibleKeys().keySet.map(outKey =>
            funct.apply(Val.Lazy(outObj.value(outKey, -1)(fs, ev)))
          ).contains(funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev))))) {
            out.+=(key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev)))
          }
        }
      )
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }

    new Val.Obj(out, _ => (), None)
  }

  private def filter(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    Val.Arr(
      if (args == 2)
        array.zipWithIndex.filter({
          case (lazyItem, index) => funct.apply(lazyItem, Val.Lazy(Val.Num(index))) == Val.True
        }).map(_._1)
      else if (args == 1)
        array.filter(lazyItem => funct.apply(lazyItem) == Val.True)
      else {
        throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
      }
    )
  }

  private def filterObject(obj: Val.Obj, func: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = func.f.params.allIndices.size
    new Val.Obj(
      if (args == 3) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.zipWithIndex.toSeq.collect({
            case (key, index) if func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)), Val.Lazy(Val.Num(index))) == Val.True =>
              key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
          }): _*)
      }
      else if (args == 2) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.toSeq.collect({
            case key if func.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key))) == Val.True =>
              key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
          }): _*)
      }
      else if (args == 1) {
        scala.collection.mutable.Map(
          obj.getVisibleKeys().keySet.toSeq.collect({
            case key if func.apply(Val.Lazy(obj.value(key, -1)(fs, ev))) == Val.True =>
              key -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key, -1)(fs, ev))
          }): _*)
      }
      else {
        throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2 or 3, but got: " + args)
      }, _ => (), None) // end of new object to return
  }

  private def flatMap(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    val out = collection.mutable.Buffer.empty[Val.Lazy]
    if (args == 2) { // 2 args
      for (v <- array) {
        v.force match {
          case Val.Arr(inner) =>
            out.appendAll(inner.zipWithIndex.map({
              case (it, ind) => Val.Lazy(funct.apply(it, Val.Lazy(Val.Num(ind))))
            }))
          case _ => throw new IllegalArgumentException(
            "Expected Array of Arrays, got: Array of " + v.force.prettyName);
        }
      }
    }
    else if (args == 1) { //  1 arg
      for (v <- array) {
        v.force match {
          case Val.Arr(inner) =>
            out.appendAll(inner.map(it => Val.Lazy(funct.apply(it))))
          case _ => throw new IllegalArgumentException(
            "Expected Array of Arrays, got: Array of " + v.force.prettyName);
        }
      }
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }
    Val.Arr(out.toSeq)
  }

  private def groupBy(s: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    if (args == 2) {
      for ((item, index) <- s.zipWithIndex) {

        val key = funct.apply(item, Val.Lazy(Val.Num(index)))
        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(key.cast[Val.Str].value)) {

          val array = collection.mutable.Buffer.empty[Val.Lazy]
          array.appendAll(s.zipWithIndex.collect({
            case (item2, index2) if key == funct.apply(item2, Val.Lazy(Val.Num(index2))) =>
              item2
          }))
          out += (key.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(array.toSeq)))
        }
      }
    } else if (args == 1) {
      for (item <- s) {

        val key = funct.apply(item)
        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(key.cast[Val.Str].value)) {

          val array = collection.mutable.Buffer.empty[Val.Lazy]
          array.appendAll(s.collect({
            case item2 if key == funct.apply(item2) =>
              item2
          }))
          out += (key.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => Val.Arr(array.toSeq)))
        }
      }
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }

    new Val.Obj(out, _ => (), None)
  }

  private def groupBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val out = scala.collection.mutable.Map[String, Val.Obj.Member]()
    val args = funct.f.params.allIndices.size
    if (args == 2) {
      for ((key, _) <- obj.getVisibleKeys()) {
        val functKey = funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)), Val.Lazy(Val.Str(key)))

        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(functKey.cast[Val.Str].value)) {

          val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
          currentObj.addAll(obj.getVisibleKeys().collect({
            case (key2, _) if functKey == funct.apply(Val.Lazy(obj.value(key2, -1)(fs, ev)), Val.Lazy(Val.Str(key2))) =>
              key2 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key2, -1)(fs, ev))
          }))
          out += (functKey.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => new Val.Obj(currentObj, _ => (), None)))
        }
      }
    }
    else if (args == 1) {
      for ((key, _) <- obj.getVisibleKeys()) {
        val functKey = funct.apply(Val.Lazy(obj.value(key, -1)(fs, ev)))

        if (!new Val.Obj(out, _ => (), None)
          .getVisibleKeys()
          .contains(functKey.cast[Val.Str].value)) {

          val currentObj = scala.collection.mutable.Map[String, Val.Obj.Member]()
          currentObj.addAll(obj.getVisibleKeys().collect({
            case (key2, _) if functKey == funct.apply(Val.Lazy(obj.value(key2, -1)(fs, ev))) =>
              key2 -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(key2, -1)(fs, ev))
          }))
          out += (functKey.cast[Val.Str].value -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => new Val.Obj(currentObj, _ => (), None)))
        }
      }
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }

    new Val.Obj(out, _ => (), None)
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
        throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
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
          case i => throw new IllegalArgumentException(
            "Function must return an object, got: " + i.prettyName);
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
          case i => throw new IllegalArgumentException(
            "Function must return an object, got: " + i.prettyName);
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
          case i => throw new IllegalArgumentException(
            "Function must return an object, got: " + i.prettyName);
        }
      }
      new Val.Obj(out, _ => (), None)
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2 or 3, but got: " + args)
    }
  }

  private def orderBy(array: Seq[Val.Lazy], funct: Applyer): Val = {
    val args = funct.f.params.allIndices.size
    if (args == 2) {
      Val.Arr(
        array.zipWithIndex.sortBy(
          it => funct.apply(it._1, Val.Lazy(Val.Num(it._2))).toString
        ).map(_._1))
    }
    else if (args == 1) {
      Val.Arr(array.sortBy(it => funct.apply(it).toString))
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
    }
  }

  private def orderBy(obj: Val.Obj, funct: Applyer, ev: EvalScope, fs: FileScope): Val = {
    val args = funct.f.params.allIndices.size
    var out = scala.collection.mutable.Map.empty[String, Val.Obj.Member]
    for ((item, _) <- obj.getVisibleKeys()) {
      out += (item -> Val.Obj.Member(add = false, Visibility.Normal, (_, _, _, _) => obj.value(item, -1)(fs, ev)))
    }
    if (args == 2) {
      new Val.Obj(
        scala.collection.mutable.Map(
          out.toSeq.sortWith {
            case ((it1, _), (it2, _)) =>
              funct.apply(Val.Lazy(Val.Str(it1)), Val.Lazy(obj.value(it1, -1)(fs, ev))).toString >
                funct.apply(Val.Lazy(Val.Str(it2)), Val.Lazy(obj.value(it2, -1)(fs, ev))).toString
          }: _*),
        _ => (), None)
    }
    else if (args == 1) {
      new Val.Obj(
        scala.collection.mutable.Map(
          out.toSeq.sortWith {
            case ((it1, _), (it2, _)) =>
              funct.apply(Val.Lazy(Val.Str(it1))).toString >
                funct.apply(Val.Lazy(Val.Str(it2))).toString
          }: _*),
        _ => (), None)
    }
    else {
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2, but got: " + args)
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
      throw new IllegalArgumentException("Incorrect number of arguments in the provided function. Expected 1 or 2 or 3, but got: " + args)
    }

    Val.Arr(out.toSeq)
  }

}
