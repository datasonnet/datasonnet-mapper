package com.datasonnet

import java.time.format.DateTimeFormatter
import java.time.{Instant, Period, ZoneId, ZoneOffset}
import java.util.function.Function

import com.datasonnet
import com.datasonnet.spi.{DataFormatPlugin, DataFormatService, UnsupportedMimeTypeException, UnsupportedParameterException}
import sjsonnet.Std.builtinWithDefaults
import sjsonnet.{Applyer, Error, EvalScope, Expr, Materializer, Val}
import com.datasonnet.wrap.Library.library
import sjsonnet.ReadWriter.StringRead
import sjsonnet.Std._
import ujson.Value

import scala.util.Failure


object DS {

  val libraries = Map(
    "ZonedDateTime" -> library(
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

    "Formats" -> library(
      builtinWithDefaults("read",
                          "data" -> None,
                          "mimeType" -> None,
                          "params" -> Some(Expr.Null(0))) { (args, ev) =>
        val data = args("data").cast[Val.Str].value
        val mimeType = args("mimeType").cast[Val.Str].value
        val params = if (args("params") == Val.Null) null else args("params").cast[Val.Obj]
        read(data, mimeType, params, ev)
      },
      builtinWithDefaults("write",
        "data" -> None,
        "mimeType" -> None,
        "params" -> Some(Expr.Null(0))) { (args, ev) =>
        val data = args("data")
        val mimeType = args("mimeType").cast[Val.Str].value
        val params = if (args("params") == Val.Null) null else args("params").cast[Val.Obj]
        write(data, mimeType, params, ev)
      },

    ),

    "LocalDateTime" -> library(
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

    "Crypto" -> library(
      builtin("hash", "value", "algorithm") {
        (ev, fs, value: String, algorithm: String) =>
          Crypto.hash(value, algorithm)
      },
      builtin("hmac", "value", "secret", "algorithm") {
        (ev, fs, value: String, secret: String, algorithm: String) =>
          datasonnet.Crypto.hmac(value, secret, algorithm)
      },
      builtin("encrypt", "value", "password") {
        (ev, fs, value: String, password: String) =>
          datasonnet.Crypto.encrypt(value, password)
      },
      builtin("decrypt", "value", "password") {
        (ev, fs, value: String, password: String) =>
          datasonnet.Crypto.decrypt(value, password)
      },
    ),

    "JsonPath" -> library(
      builtin("select", "json", "path") {
        (ev, fs, json: Val, path: String) =>
          Materializer.reverse(ujson.read(JsonPath.select(ujson.write(Materializer.apply(json)(ev)), path)))
      },
    ),

    "Regex" -> library(
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
      },
    )

  )

  def read(data: String, mimeType: String, params: Val.Obj, ev: EvalScope): Val = {
    val plugin = DataFormatService.getInstance().getPluginFor(mimeType)
    if (plugin == null) {
      throw new UnsupportedMimeTypeException("No suitable plugin found for mime type: " + mimeType)
    }
    val javaParams = if (params != null) toJavaParams(ev, params, plugin) else new java.util.HashMap[String, Object]()
    val json = try {
      plugin.getClass.getMethod("read", classOf[String], classOf[java.util.Map[String, Object]])
      plugin.asInstanceOf[DataFormatPlugin[String]].read(data, javaParams)
    } catch {
      case _ => throw new UnsupportedOperationException("The data format plugin for " + mimeType +
        " does not take Strings, which is required for conversions inside DataSonnet code")
    }

    Materializer.reverse(json)
  }

  def write(json: Val, mimeType: String, params: Val.Obj, ev: EvalScope): String = {
    val plugin = DataFormatService.getInstance().getPluginFor(mimeType)
    if (plugin == null) {
      throw new UnsupportedMimeTypeException("No suitable plugin found for mime type: " + mimeType);
    }
    val javaParams = if (params != null) toJavaParams(ev, params, plugin) else new java.util.HashMap[String, Object]()
    val output = plugin.write(Materializer.apply(json)(ev), javaParams, mimeType)
    if (output.canGetContentsAs(classOf[String])) {
      output.getContentsAsString()
    } else {
      throw new UnsupportedOperationException("The data format plugin for " + mimeType +
        " does not return output that can be rendered as a String, which is required for conversions inside" +
        "DataSonnet code")
    }
  }

  def toJavaParams(ev: EvalScope, params: Val.Obj, plugin: DataFormatPlugin[_]): java.util.Map[String, Object] = {

    val scalaParams = ujson.read(Materializer.apply(params)(ev)).obj;
    val javaParams = new java.util.HashMap[String, Object]()
    val supportedParams = plugin.getReadParameters()

    //Convert to Java map
    for ((k, v) <- scalaParams) {
      if (!supportedParams.containsKey(k)) {
        Failure(new UnsupportedParameterException("The parameter " + k + " is not supported by plugin " + plugin.getPluginId))
      }
      javaParams.put(k, toJavaObject(v))
    }

    javaParams
  }

  def toJavaObject(v: ujson.Value): java.lang.Object = {
    if (v.isInstanceOf[ujson.Bool]) {
      new java.lang.Boolean(v.bool)
    } else if (v.isInstanceOf[ujson.Str]) {
      v.str
    } else if (v.isInstanceOf[ujson.Num]) {
      new java.lang.Double(v.num)
    } else if (v.isInstanceOf[ujson.Obj]) {
      val javaMap = new java.util.HashMap[String, Object]()
      for ((k, vv) <- v.obj) {
        javaMap.put(k, toJavaObject(vv))
      }
      javaMap
    } else if (v.isInstanceOf[ujson.Arr]) {
      val javaArr = new java.util.ArrayList[Object]()
      for ((vv) <- v.arr) {
        javaArr.add(toJavaObject(vv))
      }
      javaArr
    } else {
      //TODO support other types or throw an exception???
      null
    }
  }
}
