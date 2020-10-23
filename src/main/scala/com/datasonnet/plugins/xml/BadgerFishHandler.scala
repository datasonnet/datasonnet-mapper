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

import com.datasonnet.plugins.DefaultXMLFormatPlugin
import com.datasonnet.plugins.DefaultXMLFormatPlugin.{DEFAULT_NS_KEY, EffectiveParams}
import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.{Attributes, SAXParseException}

import scala.collection.mutable

// See {@link scala.xml.parsing.FactoryAdapter}
class BadgerFishHandler(params: EffectiveParams) extends DefaultHandler2 {
  def result: ujson.Obj = badgerStack.top.obj

  val buffer = new StringBuilder()
  val badgerStack = new mutable.Stack[BadgerFish]
  // ignore text until after first element starts
  var capture: Boolean = false

  private var needNewContext = true
  private val namespaceParts = new Array[String](3)  // keep reusing a single array
  private val namespaces = new OverridingNamespaceTranslator(params.declarations)
  private var currentNS: mutable.LinkedHashMap[String, ujson.Str] = mutable.LinkedHashMap()

  // root
  badgerStack.push(BadgerFish(ujson.Obj()))

  override def startPrefixMapping(prefix: String, uri: String): Unit = {
    if(needNewContext) {
      namespaces.pushContext()
      needNewContext = false
      if(currentNS.nonEmpty) currentNS = currentNS.empty
    }
    namespaces.declarePrefix(prefix, uri)
    val newPrefix = namespaces.getPrefix(uri)
    currentNS.put(if (newPrefix == null) DEFAULT_NS_KEY else newPrefix, ujson.Str(uri))
  }

  override def startElement(uri: String,
                            _localName: String,
                            qname: String,
                            attributes: Attributes): Unit = {
    if (needNewContext) namespaces.pushContext()
    needNewContext = true

    captureText()
    capture = true

    val current = ujson.Obj()
    if (currentNS.nonEmpty) {
      current.value.addOne((params.xmlnsKey, currentNS))
      currentNS = currentNS.empty
    }

    if (attributes.getLength > 0) {
      val attrs = mutable.ListBuffer[(String, ujson.Str)]()

      for (i <- 0 until attributes.getLength) {
        val qname = attributes getQName i
        val value = attributes getValue i
        val translated = processName(qname, true)

        attrs.addOne((params.attrKeyPrefix + translated, ujson.Str(value)))
      }
      current.value.addAll(attrs.toList)
    }

    badgerStack.push(BadgerFish(current))
  }

  override def characters(ch: Array[Char], offset: Int, length: Int): Unit = {
    if (capture) buffer.appendAll(ch, offset, length)
  }

  override def startCDATA(): Unit = {
    captureText()
  }

  override def endCDATA(): Unit = {
    if (buffer.nonEmpty) {
      val idx = badgerStack.top.cdataIdx
      badgerStack.top.obj.value.addOne(params.cdataKeyPrefix + idx, ujson.Str(buffer.toString))
      badgerStack.top.cdataIdx = idx + 1
    }

    buffer.clear()
  }

  override def endElement(uri: String, _localName: String, qname: String): Unit = {
    captureText()

    val translated = processName(qname, false)
    val newName = translated.replaceFirst(":", params.nsSeparator)
    val current = badgerStack.pop
    val parent = badgerStack.top.obj.value
    if (parent.contains(newName)) {
      (parent(newName): @unchecked) match {
        // added @unchecked to suppress non-exhaustive match warning, we will only see Arrs or Objs
        case ujson.Arr(arr) => arr.addOne(current.obj)
        case ujson.Obj(existing) => parent.addOne(newName, ujson.Arr(existing, current.obj))
      }
    } else {
      parent.addOne(newName, current.obj)
    }

    capture = badgerStack.size != 1 // root level
    namespaces.popContext()
  }

  private def processName(qname: String, isAttribute: Boolean) = {
    // while processName can return null here, it will only do so if the XML
    // namespace processing is written incorrectly, so if you see this line in a stack trace,
    // go verifying what namespace-related calls have been made
    namespaces.processName(qname, namespaceParts, isAttribute)(2)
  }

  def captureText(): Unit = {
    if (capture && buffer.nonEmpty) {
      val idx = badgerStack.top.txtIdx
      val string = buffer.toString
      // TODO: change to a isNotBlank func
      if (string.trim.nonEmpty) {
        badgerStack.top.obj.value.addOne(params.textKeyPrefix + idx, buffer.toString)
        badgerStack.top.txtIdx = idx + 1
      }
    }

    buffer.clear()
  }

  private def splitName(s: String) = {
    val idx = s indexOf ':'
    if (idx < 0) (s, "")
    else (s take idx, s drop (idx + 1))
  }

  override def warning(ex: SAXParseException): Unit = {}

  override def error(ex: SAXParseException): Unit = printError("Error", ex)

  override def fatalError(ex: SAXParseException): Unit = printError("Fatal Error", ex)

  protected def printError(errtype: String, ex: SAXParseException): Unit =
    Console.withOut(Console.err) {
      val s = "[%s]:%d:%d: %s".format(
        errtype, ex.getLineNumber, ex.getColumnNumber, ex.getMessage)
      Console.println(s)
      Console.flush()
    }

  case class BadgerFish(obj: ujson.Obj, var txtIdx: Int = 1, var cdataIdx: Int = 1)

}
