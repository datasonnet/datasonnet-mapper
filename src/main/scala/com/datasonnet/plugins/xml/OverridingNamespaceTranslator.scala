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

import java.util

import org.xml.sax.helpers.NamespaceSupport

import scala.collection.mutable

class OverridingNamespaceTranslator(private val fixed: Map[String, String]) extends NamespaceSupport {
  private val writeTranslated = new NamespaceSupport() {
    for ((uri, prefix) <- fixed) declarePrefix(prefix, uri)
  }
  private val usedAsSubstitutes: mutable.Set[String] = new mutable.HashSet()

  override def pushContext(): Unit = {
    super.pushContext()
    writeTranslated.pushContext()
  }

  override def popContext(): Unit = {
    super.popContext()
    writeTranslated.popContext()
  }

  private def findNewPrefix(prefix: String): String = {
    LazyList.from(1).map(prefix + _).dropWhile((candidate:String) =>
      // present currently, or
      // reserved, or
      // used as a substitute before
      // (these restrictions are overly conservative, but simpler to implement)
      writeTranslated.getURI(candidate) != null || fixed.contains(candidate) || usedAsSubstitutes.contains(candidate)
    ).head
  }

  override def declarePrefix(prefix: String, uri: String): Boolean = {
    // if in fixed, we're done, we always use that, and it was declared up front, but otherwise...
    if(!fixed.contains(uri)) {
      val currentWriteURI = writeTranslated.getURI(prefix)

      // yes, this is O(N), but the number of values will always be very small
      val translatedPrefix = if (fixed.values.find(_ == prefix).isDefined || usedAsSubstitutes.contains(prefix)) {
        // this prefix conflicts
        val newPrefix = findNewPrefix(prefix)
        usedAsSubstitutes.add(newPrefix)
        newPrefix
      } else {
        prefix
      }
      writeTranslated.declarePrefix(translatedPrefix, uri)
    }
    super.declarePrefix(prefix, uri)
  }

  // always get by uri from translation
  override def getPrefix(uri: String): String = writeTranslated.getPrefix(uri)
  override def getPrefixes(uri: String) = writeTranslated.getPrefixes(uri)

  // we need this for root namespace detection to work properly
  override def getURI(prefix: String): String = writeTranslated.getURI(prefix)

  override def processName(qName: String, parts: Array[String], isAttribute: Boolean): Array[String] = {
    val result = super.processName(qName, parts, isAttribute)  // modifies parts, returns it or null
    if (!"".equals(parts(0))) { // namespace present
      // Currently this will be slower than NamespaceSupport, because that uses extensive caching.
      // This is a candidate for future optimization, though the overall impact is probably minimal.
      val newPrefix = this.getPrefix(parts(0))
      // the null check is because getPrefix is not symmetrical with declarePrefix. The docs are very clear,
      // so I suspect this bites many people.
      parts(2) = (if (newPrefix == null) parts(1) else newPrefix + ":" + parts(1)).intern()
    }
    result  // to return null, the problem value, if the previous bit failed
    // if the XML processing is written correctly, this will never be null
  }
}
