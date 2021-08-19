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

import com.datasonnet.plugins.DefaultXMLFormatPlugin.EffectiveParams
import org.xml.sax.helpers.NamespaceSupport
import ujson.Value

import java.io.{StringWriter, Writer}

// See {@link scala.xml.Utility.serialize}
class BadgerFishWriter(val params: EffectiveParams) {

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


  val namespaces: NamespaceSupport = new OverridingNamespaceTranslator(params.declarations)
  val namespaceParts = new Array[String](3)  // keep reusing a single array

  def serialize(root: (String, ujson.Obj), sb: Writer = new StringWriter()): Writer = {
    // initialize namespaces for use in later writing
    // we have to do this because even the first thing we write,
    // the element name, might have an overridden namespace
    namespaces.pushContext()
    if (root._2.value.contains(params.xmlnsKey)) {
      root._2.value(params.xmlnsKey).obj.foreach {
        case (key, value) =>
          namespaces.declarePrefix(if (key == "$") "" else key, value.str)
      }
    }

    sb.append('<')
    val qname = processName(root._1, false)
    val element = qname.replace(params.nsSeparator, ":")
    sb.append(element)

    val (attrs, children) = root._2.value.partition(entry => entry._1.startsWith(params.attrKeyPrefix) && !entry._1.equals(params.xmlnsKey))

    attrs.foreach {
      attr =>
        val qname = attr._1.substring(params.attrKeyPrefix.size)
        val translated = namespaces.processName(qname, namespaceParts, true)(2)
        sb append ' '
        sb append translated
        sb append '='
        // TODO this is likely improperly escaped, but verify with a test
        appendQuoted(attr._2.str, sb)
    }

    if (root._2.value.contains(params.xmlnsKey)) {
      root._2.value(params.xmlnsKey).obj.foreach {
        case (key, value) =>
          sb append " xmlns%s=\"%s\"".format(
            if (key != null) {
              val newPrefix = namespaces.getPrefix(value.str)
              // this is the way to check for the root namespace prescribed by the docs
              if(newPrefix == null && namespaces.getURI("") == value.str) "" else ":" + newPrefix
            } else {
              ""  // make clear there's a problem. We should probably throw an exception instead.
            },
            if (value.str != null) value.str else ""  // again should likely exception
          )
      }
    }

    if (children.isEmpty && params.autoEmpty) {
      sb append "/>"
    } else {
      // children, so use long form: <xyz ...>...</xyz>
      sb.append('>')

      children.toSeq
        // selectively flatten arrays
        .foldLeft(collection.mutable.Buffer.empty[(String, Value)])((acc, value) =>
          if (!value._1.equals(params.orderingKey)) {
            value._2 match {
              case ujson.Arr(arr) => acc.addAll(arr.map(it => value._1 -> it))
              case _ => acc.addOne(value)
            }
          } else acc)
        // sort using ordering key in objects, and index substring in text and cdatas
        .sortBy(entry => entry._2 match {
          case ujson.Obj(inner) =>
            val order = inner.get(params.orderingKey)
            if (order.isEmpty) -1 else order.get.num.toInt
          case _ =>
            if (entry._1.last.isDigit) {
              if (entry._1.startsWith(params.textKeyPrefix))
                entry._1.substring(params.textKeyPrefix.length).toInt
              else if (entry._1.startsWith(params.cdataKeyPrefix))
                entry._1.substring(params.cdataKeyPrefix.length).toInt
              else -1
            }
            else -1
        })(Ordering[Int])
        .foreach {
          child =>
            val (key, value) = child
            if (key.equals(params.xmlnsKey)) {
              // no op
            } else if (key.startsWith(params.textKeyPrefix)) {
              escapeText(value.str, sb)
            } else if (key.startsWith(params.cdataKeyPrefix)) {
              // taken from scala.xml.PCData
              sb append "<![CDATA[%s]]>".format(value.str.replaceAll("]]>", "]]]]><![CDATA[>"))
            } else value match {
              case obj: ujson.Obj => serialize((key, obj), sb)
              case ujson.Arr(arr) => arr.foreach(arrItm => serialize((key, arrItm.obj), sb))
              case ujson.Null => if (params.nullAsEmpty) serialize((key, ujson.Obj((params.textKeyPrefix, ""))), sb)
              case num: ujson.Num => serialize((key, ujson.Obj((params.textKeyPrefix, String.valueOf(num)))), sb)
              case any: ujson.Value => serialize((key, ujson.Obj((params.textKeyPrefix, String.valueOf(any.value)))), sb)
            }
        }

      sb.append("</")
      sb.append(element)
      sb.append('>')
    }

    namespaces.popContext()
    sb
  }

  private def processName(qname: String, isAttribute: Boolean) = {
    val processed = namespaces.processName(qname, namespaceParts, isAttribute)
    if (processed == null) {
      // some namespace problem. This should likely throw an exception, but for now return the original
      qname
    } else {
      // success
      processed(2)
    }
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
