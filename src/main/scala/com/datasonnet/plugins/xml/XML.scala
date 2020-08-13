package com.datasonnet.plugins.xml

import java.io.{File, FileInputStream, InputStream, Reader, StringReader}
import java.nio.charset.Charset

import com.datasonnet.plugins.DefaultXMLFormatPlugin.EffectiveParams
import javax.xml.parsers.SAXParser
import org.xml.sax.InputSource

object Source {
  def fromFile(file: File) = new InputSource(new FileInputStream(file))

  def fromInputStream(is: InputStream): InputSource = new InputSource(is)
  def fromReader(reader: Reader): InputSource = new InputSource(reader)
  def fromString(string: String): InputSource = fromReader(new StringReader(string))
}

// See {@link scala.xml.XML}
object XML extends XMLLoader {

  /** Returns an XMLLoader whose load* methods will use the supplied SAXParser. */
  def withSAXParser(p: SAXParser): XMLLoader =
    new XMLLoader { override val parser: SAXParser = p }

  def writeXML(sb: java.io.Writer, root: (String, ujson.Obj), effParams: EffectiveParams): Unit = {
    // TODO: get charset from params
    if (!effParams.omitDeclaration) sb.append("<?xml version='" + effParams.version + "' encoding='" + Charset.defaultCharset().displayName() + "'?>")
    BadgerFishWriter.serialize(root, sb, effParams).toString
  }
}
