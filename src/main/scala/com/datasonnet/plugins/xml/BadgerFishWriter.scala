package com.datasonnet.plugins.xml

/*-
 * Copyright 2019-2020 the original author or authors.
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

import java.io.{StringWriter, Writer}

import com.datasonnet.plugins.DefaultXMLFormatPlugin.EffectiveParams

// See {@link scala.xml.Utility.serialize}
object BadgerFishWriter {
  // TODO: write docs and notice and coverage
  // taken from scala.xml.Utility
  object Escapes {
    /**
     * For reasons unclear escape and unescape are a long ways from
     * being logical inverses.
     */
    val pairs = Map(
      "lt" -> '<',
      "gt" -> '>',
      "amp" -> '&',
      "quot" -> '"',
      "apos"  -> '\''
    )
    val escMap = (pairs - "apos") map { case (s, c) => c -> ("&%s;" format s) }
    val unescMap = pairs
  }

  import Escapes.escMap

  def serialize(root: (String, ujson.Obj), sb: Writer = new StringWriter(),
                params: EffectiveParams): Writer = {
    sb.append('<')
    sb.append(root._1.replace(params.nsSeparator, ":"))

    val (attrs, children) = root._2.value.partition(entry => entry._1.startsWith(params.attrKeyPrefix) && !entry._1.equals(params.xmlnsKey))

    attrs.foreach {
      attr =>
        sb append ' '
        sb append attr._1.substring(1)
        sb append '='
        appendQuoted(attr._2.str, sb)
    }

    if (root._2.value.contains(params.xmlnsKey)) {
      root._2.value(params.xmlnsKey).obj.foreach {
        case (key, value) =>
          sb append " xmlns%s=\"%s\"".format(
            if (key != null && !key.equals("$")) ":" + key else "",
            if (value.str != null) value.str else ""
          )
      }
    }

    if (children.isEmpty && params.autoEmpty) {
      sb append "/>"
    } else {
      // children, so use long form: <xyz ...>...</xyz>
      sb.append('>')

      children.foreach {
        child =>
          if (child._1.equals(params.xmlnsKey)) {
            // no op
          } else if (child._1.startsWith(params.textKeyPrefix)) {
            escapeText(child._2.str, sb)
          } else if (child._1.startsWith(params.cdataKeyPrefix)) {
            // taken from scala.xml.PCData
            sb append "<![CDATA[%s]]>".format(child._2.str.replaceAll("]]>", "]]]]><![CDATA[>"))
          } else child._2 match {
            case obj: ujson.Obj => serialize((child._1, obj), sb, params)
            case ujson.Arr(arr) => arr.foreach(arrItm => serialize((child._1, arrItm.obj), sb, params))
            case ujson.Null => if (params.nullAsEmpty) serialize((child._1, ujson.Obj((params.textKeyPrefix, ""))), sb, params)
            case num: ujson.Num => serialize((child._1, ujson.Obj((params.textKeyPrefix, String.valueOf(num)))), sb, params)
            case any: ujson.Value => serialize((child._1, ujson.Obj((params.textKeyPrefix, String.valueOf(any.value)))), sb, params)
          }
      }

      sb.append("</")
      sb.append(root._1.replace(params.nsSeparator, ":"))
      sb.append('>')
    }

    sb
  }

  def appendQuoted(s: String, sb: Writer): Writer = {
    val ch = if (s contains '"') '\'' else '"'
    sb.append(ch).append(s).append(ch)
  }

  /**
   * Appends escaped string to `s`.
   */
  final def escape(text: String, s: Writer): Writer = {
    // Implemented per XML spec:
    // http://www.w3.org/International/questions/qa-controls
    text.iterator.foldLeft(s) { (s, c) =>
      escMap.get(c) match {
        case Some(str)                             => s append str
        case _ if c >= ' ' || "\n\r\t".contains(c) => s append c
        case _ => s // noop
      }
    }
  }

  /**
   * Appends escaped string to `s`, but not &quot;.
   */
  final def escapeText(text: String, s: Writer): Writer = {
    val escTextMap = escMap - '"' // Remove quotes from escMap
    text.iterator.foldLeft(s) { (s, c) =>
      escTextMap.get(c) match {
        case Some(str)                             => s append str
        case _ if c >= ' ' || "\n\r\t".contains(c) => s append c
        case _ => s // noop
      }
    }
  }
}
