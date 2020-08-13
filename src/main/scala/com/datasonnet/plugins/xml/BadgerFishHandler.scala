package com.datasonnet.plugins.xml

import com.datasonnet.plugins.DefaultXMLFormatPlugin
import com.datasonnet.plugins.DefaultXMLFormatPlugin.EffectiveParams
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

  // root
  badgerStack.push(BadgerFish(ujson.Obj()))

  override def startElement(uri: String,
                            _localName: String,
                            qname: String,
                            attributes: Attributes): Unit = {
    captureText()
    capture = true

    val current = ujson.Obj()

    if (attributes.getLength > 0) {
      val attrs = mutable.ListBuffer[(String, ujson.Str)]()
      val ns = mutable.LinkedHashMap[String, ujson.Str]()

      for (i <- 0 until attributes.getLength) {
        val qname = attributes getQName i
        val value = attributes getValue i

        val (pre, key) = splitName(qname)
        if (pre == "xmlns") {
          ns.put(key, ujson.Str(value))
        } else if (pre == null && qname == "xmlns") {
          ns.put(DefaultXMLFormatPlugin.DEFAULT_NS_KEY, ujson.Str(value))
        } else {
          attrs.addOne((params.attrKeyPrefix + qname, ujson.Str(value)))
        }
      }

      if (ns.nonEmpty) {
        current.value.addOne((params.xmlnsKey, ns))
      }

      if (attrs.nonEmpty) {
        current.value.addAll(attrs.toList)
      }
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
    val newName = qname.replaceFirst(":", params.nsSeparator)
    val current = badgerStack.pop
    val parent = badgerStack.top.obj.value
    if (parent.contains(newName)) {
      parent(newName) match {
        case ujson.Arr(arr) => arr.addOne(current.obj)
        case ujson.Obj(existing) => parent.addOne(newName, ujson.Arr(existing, current.obj))
      }
    } else {
      parent.addOne(newName, current.obj)
    }

    capture = badgerStack.size != 1 // root level
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
    if (idx < 0) (null, s)
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
