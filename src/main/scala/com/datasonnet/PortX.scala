package com.datasonnet

import java.time.format.DateTimeFormatter
import java.time.{Instant, ZonedDateTime, Period}

import com.datasonnet.wrap.Library.builtin
import com.datasonnet.wrap.Library.builtin0
import com.datasonnet.wrap.Library.library


object PortX {
  val Time = library(
    builtin0("now"){ (vs, extVars, wd) => Instant.now().toString() },
    builtin("offset", "datetime", "period"){ (wd, extVars, v1: String, v2: String) =>
      // NOTE: DEMO ONLY (in particular, missing proper error handling)
      val datetime = ZonedDateTime.parse(v1, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
      val period = Period.parse(v2)
      datetime.plus(period).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }
  )
}
