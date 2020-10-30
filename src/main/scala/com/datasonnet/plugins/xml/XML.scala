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
    new XMLLoader {
      override val parser: SAXParser = p
    }

  def writeXML(sb: java.io.Writer, root: (String, ujson.Obj), effParams: EffectiveParams): Unit = {
    // TODO: get charset from params
    if (!effParams.omitDeclaration) sb.append("<?xml version='" + effParams.version + "' encoding='" + Charset.defaultCharset().displayName() + "'?>")
    new BadgerFishWriter(effParams).serialize(root, sb).toString
  }
}
