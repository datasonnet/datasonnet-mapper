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

import java.io._
import java.net.URL

import com.datasonnet.plugins.DefaultXMLFormatPlugin.EffectiveParams
import com.datasonnet.plugins.xml.Source.{fromFile, fromInputStream, fromString}
import javax.xml.parsers.{SAXParser, SAXParserFactory}
import org.xml.sax.InputSource

// See {@link scala.xml.factory.XMLLoader}
trait XMLLoader {
  /* Override this to use a different SAXParser. */
  def parser: SAXParser = {
    val factory = SAXParserFactory.newInstance
    factory.setNamespaceAware(true)

    // Safer parsing settings to avoid certain class of XML attacks
    // See https://github.com/scala/scala-xml/issues/17
    factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false)
    factory.setFeature("http://xml.org/sax/features/external-general-entities", false)
    factory.setXIncludeAware(false)

    factory.newSAXParser
  }

  /**
   * Loads XML from the given InputSource, using the supplied parser.
   * The methods available in scala.xml.XML use the XML parser in the JDK.
   */
  def loadXML(source: InputSource, parser: SAXParser, params: EffectiveParams): ujson.Obj = {
    val adapter = new BadgerFishHandler(params)

    parser.getXMLReader.setProperty("http://xml.org/sax/properties/lexical-handler", adapter)
    parser.parse(source, adapter)
    adapter.result
  }

  /** Loads XML from the given file, file descriptor, or filename. */
  def loadFile(file: File, params: EffectiveParams): ujson.Obj = loadXML(fromFile(file), parser, params)

  /** loads XML from given InputStream, Reader, sysID, InputSource, or URL. */
  def load(is: InputStream, params: EffectiveParams): ujson.Obj = loadXML(fromInputStream(is), parser, params)

  def load(url: URL, params: EffectiveParams): ujson.Obj = loadXML(fromInputStream(url.openStream()), parser, params)

  /** Loads XML from the given String. */
  def loadString(string: String, params: EffectiveParams): ujson.Obj = loadXML(fromString(string), parser, params)
}