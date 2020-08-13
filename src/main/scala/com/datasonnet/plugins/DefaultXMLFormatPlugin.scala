package com.datasonnet.plugins

import java.io._
import java.net.URL
import java.nio.charset.Charset

import com.datasonnet.document
import com.datasonnet.document.{DefaultDocument, MediaType, MediaTypes}
import com.datasonnet.plugins.xml.XML
import com.datasonnet.spi.{AbstractDataFormatPlugin, PluginException}
import ujson.Value

import scala.collection.mutable
import scala.jdk.CollectionConverters.{MapHasAsScala, SetHasAsJava}

// See: http://wiki.open311.org/JSON_and_XML_Conversion/#the-badgerfish-convention
// http://www.sklar.com/badgerfish/
// http://dropbox.ashlock.us/open311/json-xml/
object DefaultXMLFormatPlugin extends AbstractDataFormatPlugin {
  private val XMLNS_KEY = "xmlns"
  val DEFAULT_NS_KEY = "$"
  private val DEFAULT_DS_NS_SEPARATOR = ":"
  private val DEFAULT_DS_ATTRIBUTE_KEY_PREFIX = "@"
  private val DEFAULT_DS_TEXT_KEY_PREFIX = "$"
  private val DEFAULT_DS_VERSION = "1.0"
  private val DEFAULT_DS_CDATA_KEY_PREFIX = "#"

  val DS_NS_SEPARATOR = "namespaceseparator"
  val DS_ATTRIBUTE_KEY_PREFIX = "attributecharacter"
  val DS_TEXT_KEY_PREFIX = "textvaluekey"
  val DS_CDATA_KEY_PREFIX = "cdatavaluekey"
  // anything that starts with NamespaceDeclarations.
  val DS_NAMESPACE_DECLARATIONS = "namespacedeclarations\\..*"
  val DS_ROOT_ELEMENT = "rootelement"
  val DS_OMIT_DECLARATION = "omitxmldeclaration"
  val DS_VERSION = "xmlversion"

  val DS_AUTO_EMPTY = "autoemptyelements"
  val DS_NULL_AS_EMPTY = "nullasemptyelement"

  getWriterParams.addAll(Set(
    AbstractDataFormatPlugin.DS_PARAM_INDENT,
    DS_NS_SEPARATOR,
    DS_ATTRIBUTE_KEY_PREFIX,
    DS_TEXT_KEY_PREFIX,
    DS_CDATA_KEY_PREFIX,
    DS_NAMESPACE_DECLARATIONS,
    DS_ROOT_ELEMENT,
    DS_OMIT_DECLARATION,
    DS_VERSION,
    DS_AUTO_EMPTY,
    DS_NULL_AS_EMPTY
  ).asJava)

  getReaderParams.addAll(Set(
    DS_NS_SEPARATOR,
    DS_ATTRIBUTE_KEY_PREFIX,
    DS_TEXT_KEY_PREFIX,
    DS_CDATA_KEY_PREFIX,
    DS_NAMESPACE_DECLARATIONS
  ).asJava)

  getReaderSupportedClasses.addAll(Set(
    classOf[String],
    classOf[java.net.URL],
    classOf[java.io.File],
    classOf[java.io.InputStream]
  ).asJava)

  getWriterSupportedClasses.addAll(Set(
    classOf[String],
    classOf[OutputStream]
  ).asJava)

  override def supportedTypes(): java.util.Set[MediaType] = {
    Set(MediaTypes.APPLICATION_XML,
      MediaTypes.TEXT_XML,
      new MediaType("application", "*+xml")).asJava
  }

  @throws[PluginException]
  override def read(doc: document.Document[_]): Value = {
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

    if (classOf[String].eq(targetType)) {
      val writer = new StringWriter()
      XML.writeXML(writer, inputAsObj.head.asInstanceOf[(String, ujson.Obj)], effectiveParams)

      new DefaultDocument(writer.toString, MediaTypes.APPLICATION_XML).asInstanceOf[document.Document[T]]
    }

    else if (classOf[OutputStream].eq(targetType)) {
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
                             omitDeclaration: Boolean, version: String,
                             xmlnsKey: String, nullAsEmpty: Boolean,
                             autoEmpty: Boolean, declarations: Map[String, String])

  object EffectiveParams {
    def apply(mediaType: MediaType): EffectiveParams = {
      val nsSep = Option(mediaType.getParameter(DS_NS_SEPARATOR)).getOrElse(DEFAULT_DS_NS_SEPARATOR)
      val txtPref = Option(mediaType.getParameter(DS_TEXT_KEY_PREFIX)).getOrElse(DEFAULT_DS_TEXT_KEY_PREFIX)
      val cdataPref = Option(mediaType.getParameter(DS_CDATA_KEY_PREFIX)).getOrElse(DEFAULT_DS_CDATA_KEY_PREFIX)
      val attrPref = Option(mediaType.getParameter(DS_ATTRIBUTE_KEY_PREFIX)).getOrElse(DEFAULT_DS_ATTRIBUTE_KEY_PREFIX)
      val omitDecl = if (mediaType.getParameter(DS_OMIT_DECLARATION) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_OMIT_DECLARATION)) else false
      val ver = Option(mediaType.getParameter(DS_VERSION)).getOrElse(DEFAULT_DS_VERSION)
      val xmlns = Option(mediaType.getParameter(DS_ATTRIBUTE_KEY_PREFIX)).getOrElse(DEFAULT_DS_ATTRIBUTE_KEY_PREFIX) + XMLNS_KEY
      val nullEmpty = if (mediaType.getParameter(DS_NULL_AS_EMPTY) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_NULL_AS_EMPTY)) else false
      val autoEmpty = if (mediaType.getParameter(DS_AUTO_EMPTY) != null) java.lang.Boolean.parseBoolean(mediaType.getParameter(DS_AUTO_EMPTY)) else false
      val declarations: Map[String, String] = mediaType.getParameters.asScala.toList
        .filter(entryVal => entryVal._1.matches(DS_NAMESPACE_DECLARATIONS))
        .map(entryVal => (entryVal._2, entryVal._1.substring(DS_NAMESPACE_DECLARATIONS.length - 3)))
        .toMap

      EffectiveParams(nsSep, txtPref, cdataPref, attrPref, omitDecl, ver, xmlns, nullEmpty, autoEmpty, Map.from(declarations))
    }
  }
}
