package com.datasonnet

import com.datasonnet.wrap.Library.builtin
import com.datasonnet.wrap.Library.library


object PortX {
  val Library = library(
    builtin("timesfive", "a"){ (wd, extVars, v1: Int) =>
      v1 * 5
    }
  )
}
