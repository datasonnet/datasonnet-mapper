package com.datasonnet

import java.time.format.DateTimeFormatter
import java.time.{Instant, Period, ZoneId, ZoneOffset, ZonedDateTime}

import com.datasonnet.wrap.Library.builtin
import com.datasonnet.wrap.Library.builtin0
import com.datasonnet.wrap.Library.library
import sjsonnet.{Materializer, Val}

/*
ZonedDateTime
DateTime
Date

offset (datetime, period, input format, output format)
format (datetime, input format, output format)

periodBetween - TBD

compare (date1, format1, date2, format2) - returns 1 if date1 > date2, -1 if date1 < date2, 0 if date1 == date2)

now

convertZone

 */

object PortX {

  val ZonedDateTime = library(
    builtin0("now") { (vs, extVars, wd) => Instant.now().toString() },

    builtin("offset", "datetime", "period") { (wd, extVars, v1: String, v2: String) =>
      // NOTE: DEMO ONLY (in particular, missing proper error handling)
      val datetime = java.time.ZonedDateTime.parse(v1, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      val period = Period.parse(v2)
      datetime.plus(period).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    },

    builtin("format", "datetime", "inputFormat", "outputFormat") {
      (wd, extVars, datetime: String, inputFormat: String, outputFormat: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(inputFormat))
        datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
    },

    builtin("compare", "datetime1", "format1", "datetime2", "format2") {
      (wd, extVars, datetime1: String, format1: String, datetime2: String, format2: String) =>
        val datetimeObj1 = java.time.ZonedDateTime.parse(datetime1, DateTimeFormatter.ofPattern(format1))
        val datetimeObj2 = java.time.ZonedDateTime.parse(datetime2, DateTimeFormatter.ofPattern(format2))
        datetimeObj1.compareTo(datetimeObj2)
    },

    builtin("changeTimeZone", "datetime", "format", "timezone") {
      (wd, extVars, datetime: String, format: String, timezone: String) =>
        val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
        val zoneId = ZoneId.of(timezone)
        val newDateTimeObj = datetimeObj.withZoneSameInstant(zoneId)
        newDateTimeObj.format(DateTimeFormatter.ofPattern(format))
    },

    builtin("toLocalDate", "datetime", "format") { (wd, extVars, datetime: String, format: String) =>
      val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
      datetimeObj.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    },

    builtin("toLocalTime", "datetime", "format") { (wd, extVars, datetime: String, format: String) =>
      val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
      datetimeObj.toLocalTime().format(DateTimeFormatter.ISO_LOCAL_TIME)
    },

    builtin("toLocalDateTime", "datetime", "format") { (wd, extVars, datetime: String, format: String) =>
      val datetimeObj = java.time.ZonedDateTime.parse(datetime, DateTimeFormatter.ofPattern(format))
      datetimeObj.toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }
  )

  val LocalDateTime = library(
    builtin0("now") { (vs, extVars, wd) =>
      val datetimeObj = java.time.LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
      datetimeObj.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    },

    builtin("offset", "datetime", "period") { (wd, extVars, v1: String, v2: String) =>
      // NOTE: DEMO ONLY (in particular, missing proper error handling)
      val datetime = java.time.LocalDateTime.parse(v1, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
      val period = Period.parse(v2)
      datetime.plus(period).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    },

    builtin("format", "datetime", "inputFormat", "outputFormat") {
      (wd, extVars, datetime: String, inputFormat: String, outputFormat: String) =>
        val datetimeObj = java.time.LocalDateTime.parse(datetime, DateTimeFormatter.ofPattern(inputFormat))
        datetimeObj.format(DateTimeFormatter.ofPattern(outputFormat))
    },

    builtin("compare", "datetime1", "format1", "datetime2", "format2") {
      (wd, extVars, datetime1: String, format1: String, datetime2: String, format2: String) =>
        val datetimeObj1 = java.time.LocalDateTime.parse(datetime1, DateTimeFormatter.ofPattern(format1))
        val datetimeObj2 = java.time.LocalDateTime.parse(datetime2, DateTimeFormatter.ofPattern(format2))
        datetimeObj1.compareTo(datetimeObj2)
    }
  )

  val CSV = library(
    builtin("read", "csvFile") {
      (wd, extVars, csvFile: String) =>
        Materializer.reverse(ujson.read(com.datasonnet.portx.CSVReader.readCSV(csvFile)))
    },
    builtin("readExt", "csvFile", "useHeader", "quote", "separator", "escape", "newLine") {
      (wd, extVars, csvFile: String, useHeader: Boolean, quote: String, separator: String, escape: String, newLine: String) =>
        Materializer.reverse(ujson.read(com.datasonnet.portx.CSVReader.readCSV(csvFile, useHeader, quote, separator, escape, newLine)))
    },
    builtin("write", "jsonArray") {
      (wd, extVars, jsonArray: Val) =>
        com.datasonnet.portx.CSVWriter.writeCSV(ujson.write(Materializer.apply(jsonArray, extVars, wd)))
    },
    builtin("writeExt", "jsonArray", "useHeader", "quote", "separator", "escape", "newLine") {
      (wd, extVars, jsonArray: Val, useHeader: Boolean, quote: String, separator: String, escape: String, newLine: String) =>
        com.datasonnet.portx.CSVWriter.writeCSV(ujson.write(Materializer.apply(jsonArray, extVars, wd)), useHeader, quote, separator, escape, newLine)
    },
  )

  val Crypto = library(
    builtin("hash", "value", "algorithm") {
      (wd, extVars, value: String, algorithm: String) =>
        com.datasonnet.portx.Crypto.hash(value, algorithm)
    },
    builtin("hmac", "value", "secret", "algorithm") {
      (wd, extVars, value: String, secret: String, algorithm: String) =>
        com.datasonnet.portx.Crypto.hmac(value, secret, algorithm)
    },
    builtin("encrypt", "value", "password") {
      (wd, extVars, value: String, password: String) =>
        com.datasonnet.portx.Crypto.encrypt(value, password)
    },
    builtin("decrypt", "value", "password") {
      (wd, extVars, value: String, password: String) =>
        com.datasonnet.portx.Crypto.decrypt(value, password)
    },
  )

}
