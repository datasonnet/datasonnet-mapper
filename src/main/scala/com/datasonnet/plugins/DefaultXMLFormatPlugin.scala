package com.datasonnet.plugins

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

import java.io._
import java.net.URL
import java.nio.charset.Charset

import com.datasonnet.document
import com.datasonnet.document.{DefaultDocument, MediaType, MediaTypes}
import com.datasonnet.plugins.xml.XML
import com.datasonnet.spi.{AbstractDataFormatPlugin, PluginException}
import ujson.Value

import scala.collection.mutable
import scala.jdk.CollectionConverters.MapHasAsScala

// See: http://wiki.open311.org/JSON_and_XML_Conversion/#the-badgerfish-convention
// http://www.sklar.com/badgerfish/
// http://dropbox.ashlock.us/open311/json-xml/
object DefaultXMLFormatPlugin extends AbstractDataFormatPlugin {
  private val XMLNS_KEY = "xmlns"
  val DEFAULT_NS_KEY = "$"
  private val DEFAULT_DS_NS_SEPARATOR = ":"
  private val DEFAULT_DS_ATTRIBUTE_KEY_PREFIX = "@"
  private val DEFAULT_DS_ORDERING_KEY = "~"
  private val DEFAULT_DS_TEXT_KEY_PREFIX = "$"
  private val DEFAULT_DS_VERSION = "1.0"
  private val DEFAULT_DS_CDATA_KEY_PREFIX = "#"
  private val DEFAULT_DS_MIXED_CONTENT = "both"

  val DS_NS_SEPARATOR = "namespaceseparator"
  val DS_ATTRIBUTE_KEY_PREFIX = "attributecharacter"
  val DS_ORDERING_KEY = "orderingkey"
  val DS_TEXT_KEY_PREFIX = "textvaluekey"
  val DS_CDATA_KEY_PREFIX = "cdatavaluekey"
  // anything that starts with NamespaceDeclarations.
  val DS_NAMESPACE_DECLARATIONS = "namespacedeclarations\\..*"
  val DS_ROOT_ELEMENT = "rootelement"
  val DS_OMIT_DECLARATION = "omitxmldeclaration"
  val DS_VERSION = "xmlversion"

  val DS_AUTO_EMPTY = "autoemptyelements"
  val DS_NULL_AS_EMPTY = "nullasemptyelement"

  supportedTypes.add(MediaTypes.APPLICATION_XML)
  supportedTypes.add(MediaTypes.TEXT_XML)
  supportedTypes.add(new MediaType("application", "*+xml"))

  writerParams.add(AbstractDataFormatPlugin.DS_PARAM_INDENT)
  writerParams.add(DS_NS_SEPARATOR)
  writerParams.add(DS_ATTRIBUTE_KEY_PREFIX)
  writerParams.add(DS_TEXT_KEY_PREFIX)
  writerParams.add(DS_CDATA_KEY_PREFIX)
  writerParams.add(DS_ORDERING_KEY)
  writerParams.add(DS_NAMESPACE_DECLARATIONS)
  writerParams.add(DS_ROOT_ELEMENT)
  writerParams.add(DS_OMIT_DECLARATION)
  writerParams.add(DS_VERSION)
  writerParams.add(DS_AUTO_EMPTY)
  writerParams.add(DS_NULL_AS_EMPTY)

  readerParams.add(DS_NS_SEPARATOR)
  readerParams.add(DS_ATTRIBUTE_KEY_PREFIX)
  readerParams.add(DS_TEXT_KEY_PREFIX)
  readerParams.add(DS_CDATA_KEY_PREFIX)
  readerParams.add(DS_ORDERING_KEY)
  readerParams.add(DS_NAMESPACE_DECLARATIONS)

  readerSupportedClasses.add(classOf[String].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.net.URL].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.io.File].asInstanceOf[java.lang.Class[_]])
  readerSupportedClasses.add(classOf[java.io.InputStream].asInstanceOf[java.lang.Class[_]])

  writerSupportedClasses.add(classOf[String].asInstanceOf[java.lang.Class[_]])
  writerSupportedClasses.add(classOf[OutputStream].asInstanceOf[java.lang.Class[_]])

  @throws[PluginException]
  override def read(doc: document.Document[_]): Value = {
    if (doc.getContent == null) return ujson.Null

    val effectiveParams = EffectiveParams(doc.getMediaType)

    doc.getContent.getClass match {
      case cls if classOf[String].isAssignableFrom(cls) => XML.loadString(doc.getContent.asInstanceOf[String], effectiveParams)
      case cls if classOf[URL].isAssignableFrom(cls) => XML.load(doc.getContent.asInstanceOf[URL], effectiveParams)
      case cls if classOf[File].isAssignableFrom(cls) => XML.loadFile(doc.getContent.asInstanceOf[File], effectiveParams)
      case cls if classOf[InputStream].isAssignableFrom(cls) => XML.load(doc.getContent.asInstanceOf[InputStream], effectiveParams)
      case _ => throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"))
    }
  }

  @throws[PluginException]
  override def write[T](input: Value, mediaType: MediaType, targetType: Class[T]): document.Document[T] = {
    if (!input.isInstanceOf[ujson.Obj]) {
      throw new PluginException("Input for XML writer must be an Object, got " + input.getClass)
    }

    val effectiveParams = EffectiveParams(mediaType)
    var charset = mediaType.getCharset
    if (charset == null) {
      charset = Charset.defaultCharset
    }

    var inputAsObj: mutable.Map[String, Value] = input.obj.asInstanceOf[mutable.Map[String, Value]]

    if (mediaType.getParameters.containsKey(DS_ROOT_ELEMENT)) {
      inputAsObj = mutable.Map((mediaType.getParameter(DS_ROOT_ELEMENT), input))
    }

    if (inputAsObj.keys.size > 1) {
      throw new PluginException("Object must have only one root element")
    }

    if (targetType.isAssignableFrom(classOf[String])) {
      val writer = new StringWriter()
      XML.writeXML(writer, inputAsObj.head.asInstanceOf[(String, ujson.Obj)], effectiveParams)

      new DefaultDocument(writer.toString, MediaTypes.APPLICATION_XML).asInstanceOf[document.Document[T]]
    }

    else if (targetType.isAssignableFrom(classOf[OutputStream])) {
      val out = new BufferedOutputStream(new ByteArrayOutputStream)
      XML.writeXML(new OutputStreamWriter(out, charset), inputAsObj.head.asInstanceOf[(String, ujson.Obj)], effectiveParams)

      new DefaultDocument(out, MediaTypes.APPLICATION_XML).asInstanceOf[document.Document[T]]
    }

    else {
      throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"))
    }
  }

  case class EffectiveParams(nsSeparator: String, textKeyPrefix: String,
                             cdataKeyPrefix: String, attrKeyPrefix: String,
                             orderingKey: String,
                             omitDeclaration: Boolean, version: String,
                             xmlnsKey: String, nullAsEmpty: Boolean,
                             autoEmpty: Boolean, declarations: Map[String, String])

  object EffectiveParams {
    def apply(mediaType: MediaType): EffectiveParams = {
      val nsSep = Option(mediaType.getParameter(DS_NS_SEPARATOR)).getOrElse(DEFAULT_DS_NS_SEPARATOR)
      val txtPref = Option(mediaType.getParameter(DS_TEXT_KEY_PREFIX)).getOrElse(DEFAULT_DS_TEXT_KEY_PREFIX)
      val cdataPref = Option(mediaType.getParameter(DS_CDATA_KEY_PREFIX)).getOrElse(DEFAULT_DS_CDATA_KEY_PREFIX)
      val attrPref = Option(mediaType.getParameter(DS_ATTRIBUTE_KEY_PREFIX)).getOrElse(DEFAULT_DS_ATTRIBUTE_KEY_PREFIX)
      val orderingKey = Option(mediaType.getParameter(DS_ORDERING_KEY)).getOrElse(DEFAULT_DS_ORDERING_KEY)
      val omitDecl = if (mediaType.getParameter(DS_OMIT_DECLARATION) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_OMIT_DECLARATION)) else false
      val ver = Option(mediaType.getParameter(DS_VERSION)).getOrElse(DEFAULT_DS_VERSION)
      val xmlns = Option(mediaType.getParameter(DS_ATTRIBUTE_KEY_PREFIX)).getOrElse(DEFAULT_DS_ATTRIBUTE_KEY_PREFIX) + XMLNS_KEY
      val nullEmpty = if (mediaType.getParameter(DS_NULL_AS_EMPTY) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_NULL_AS_EMPTY)) else false
      val autoEmpty = if (mediaType.getParameter(DS_AUTO_EMPTY) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_AUTO_EMPTY)) else false
      val declarations: Map[String, String] = mediaType.getParameters.asScala.toList
        .filter(entryVal => entryVal._1.matches(DS_NAMESPACE_DECLARATIONS))
        .map(entryVal => (entryVal._2, entryVal._1.substring(DS_NAMESPACE_DECLARATIONS.length - 3)))
        .map(entry => if (entry._2 == "$") (entry._1, "") else entry)
        .toMap

      EffectiveParams(nsSep, txtPref, cdataPref, attrPref, orderingKey, omitDecl, ver, xmlns, nullEmpty, autoEmpty, declarations)
    }
  }
}
