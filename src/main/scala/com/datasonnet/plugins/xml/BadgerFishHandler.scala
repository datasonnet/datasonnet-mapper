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

import com.datasonnet.plugins.DefaultXMLFormatPlugin.{DEFAULT_NS_KEY, EffectiveParams}
import org.xml.sax.ext.DefaultHandler2
import org.xml.sax.{Attributes, SAXParseException}
import ujson.Value

import scala.collection.immutable.Set
import scala.collection.mutable

// See {@link scala.xml.parsing.FactoryAdapter}
class BadgerFishHandler(params: EffectiveParams) extends DefaultHandler2 {
  def result: ujson.Obj = collateObj(badgerStack.top)

  val buffer = new StringBuilder()
  val badgerStack = new mutable.Stack[Element]
  // ignore text until after first element starts
  var capture: Boolean = false

  private var needNewContext = true
  private val namespaceParts = new Array[String](3)  // keep reusing a single array
  private val namespaces = new OverridingNamespaceTranslator(params.declarations)
  private var currentNS: mutable.LinkedHashMap[String, ujson.Str] = mutable.LinkedHashMap()

  // root
  badgerStack.push(Element(ujson.Obj()))

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

    badgerStack.push(Element(current))
  }

  override def characters(ch: Array[Char], offset: Int, length: Int): Unit = {
    if (capture) buffer.appendAll(ch, offset, length)
  }

  override def startCDATA(): Unit = {
    captureText()
  }

  override def endCDATA(): Unit = {
    if (buffer.nonEmpty) {
      badgerStack.top.children = badgerStack.top.children :+ CData(buffer.toString)
    }
    buffer.clear()
  }

  def examineChildren(children: List[Node]): ChildrenCase.Value = {
    val classes = children.map(_.getClass).toSet
    if (classes == Set(classOf[Text])) {
      ChildrenCase.ALL_SIMPLE_TEXT
    } else if (classes subsetOf Set(classOf[Text], classOf[CData])) {
      ChildrenCase.MIXED_TEXT
    } else if (classes == Set(classOf[Element])) {
      if(wellOrdered(children.asInstanceOf[List[Element]])) {
        ChildrenCase.STRUCTURED_CONTENT
      } else {
        ChildrenCase.OUT_OF_ORDER_ELEMENTS
      }
    } else {
      ChildrenCase.MIXED_CONTENT
    }
  }

  def wellOrdered(children: List[Element]): Boolean = {
    val found = mutable.Set[String]()
    var last = ""
    children.map(_.name).forall(v => if (last == v || !found.contains(v)) {
      found.add(v)
      last = v
      true
    } else {
      false
    })
  }

  def mixedNodes(children: List[Node]): IterableOnce[(String, Value)] = {
    val values = mutable.LinkedHashMap[String, Value]()
    children.zipWithIndex.foreach {
      case (node, index) =>
        val index1 = index + 1
        node match {
          case Text(value) => values += (params.textKeyPrefix + index1 -> ujson.Str(value))
          case CData(value) => values += (params.cdataKeyPrefix + index1 -> ujson.Str(value))
          case Element(obj, name, _) => {
            obj.value += (params.orderingKey -> index1)
            if (values.contains(name)) {
              (values(name): @unchecked) match {
                case ujson.Arr(arr) => arr += obj
                case ujson.Obj(existing) => values += (name -> ujson.Arr(existing, obj))
              }
            } else {
              values += (name -> obj)
            }
          }
        }
    }
    values
  }

  def combinedElements(children: List[Element]): IterableOnce[(String, Value)] = {
    val values = mutable.LinkedHashMap[String, Value]()
    // note, do not use groupBy as it does not provide the ordering guarantee we need
    children.foreach(value => {
      if (values.contains(value.name)) {
        (values(value.name): @unchecked) match {
          case ujson.Arr(arr) => arr.addOne(value.obj)
          case ujson.Obj(existing) => values(value.name) = ujson.Arr(existing, value.obj)
        }
      } else {
        values(value.name) = value.obj
      }
    })
    values
  }

  override def endElement(uri: String, _localName: String, qname: String): Unit = {
    captureText()

    val translated = processName(qname, false)
    val newName = translated.replaceFirst(":", params.nsSeparator)
    val current = badgerStack.pop
    val parent = badgerStack.top

    // don't include the current children because we've already handled them
    parent.children = parent.children :+ Element(collateObj(current), newName)

    capture = badgerStack.size != 1 // root level
    namespaces.popContext()
  }

  private def collateObj(current: Element): ujson.Obj = {
    val children = current.children
    val childrenCase = examineChildren(children)

    childrenCase match {
      case ChildrenCase.ALL_SIMPLE_TEXT => {
        // they are all text nodes, so just combine them into one and add that
        val combined = children.asInstanceOf[List[Textual]].map(_.value).mkString("")
        current.obj.value += (params.textKeyPrefix -> ujson.Str(combined))
      }
      case ChildrenCase.MIXED_TEXT => {
        // all textual nodes, so combine them
        val combined = children.asInstanceOf[List[Textual]].map(_.value).mkString("")
        current.obj.value += (params.textKeyPrefix -> ujson.Str(combined))

        // but also preserve them as individual nodes just in case that's of interest
        current.obj.value ++= mixedNodes(children)
      }
      case _ => current.obj.value ++= mixedNodes(children)
    }
    current.obj
  }

  private def processName(qname: String, isAttribute: Boolean) = {
    // while processName can return null here, it will only do so if the XML
    // namespace processing is written incorrectly, so if you see this line in a stack trace,
    // go verifying what namespace-related calls have been made
    namespaces.processName(qname, namespaceParts, isAttribute)(2)
  }

  def captureText(): Unit = {
    if (capture && buffer.nonEmpty) {
      val string = buffer.toString
      // TODO: change to a isNotBlank func
      if (string.trim.nonEmpty) {
        badgerStack.top.children = badgerStack.top.children :+ Text(string)
      }
    }

    buffer.clear()
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



}


object ChildrenCase extends Enumeration {
  val ALL_SIMPLE_TEXT, MIXED_TEXT, OUT_OF_ORDER_ELEMENTS, MIXED_CONTENT, STRUCTURED_CONTENT = Value
}

sealed abstract class Node
abstract class Textual(val value: String) extends Node
case class Text(override val value: String) extends Textual(value)
case class CData(override val value: String) extends Textual(value)
case class Element(obj: ujson.Obj, name: String = "", var children: List[Node] = List()) extends Node