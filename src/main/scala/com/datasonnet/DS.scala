package com.datasonnet

import java.time.format.DateTimeFormatter
import java.time.{Instant, Period, ZoneId, ZoneOffset}
import java.util.function.Function

import com.datasonnet
import com.datasonnet.document.{DefaultDocument, MediaType}
import com.datasonnet.spi.{DataFormatService, Library, ujsonUtils}
import sjsonnet.ReadWriter.StringRead
import sjsonnet.Std.{builtinWithDefaults, _}
import sjsonnet.{Applyer, Error, EvalScope, Expr, Materializer, Val}
import ujson.Value

object DS extends Library {

  override def namespace() = "DS"

  override def libsonnets(): Set[String] = Set("Util")

  override def functions(dataFormats: DataFormatService): Map[String, Val.Func] = Map(
    builtin0("test") { (vals, ev, fs) => "test val" }
  )

  override def modules(dataFormats: DataFormatService): Map[String, Val.Obj] = Map(
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
          Library.emptyObj
        } else {
          args("params").cast[Val.Obj]
        }
        read(dataFormats, data, mimeType, params, ev)
      },
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
                  case _  => throw new Error.Delegate("The result of the replacement function must be a String")
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
}
