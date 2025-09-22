package com.datasonnet.spi

/*-
 * Copyright 2019-2025 the original author or authors.
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

import com.fasterxml.jackson.databind.node._
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.core.{JsonFactory, JsonParser}
import ujson._

import scala.jdk.CollectionConverters.{IteratorHasAsScala, MapHasAsJava, SeqHasAsJava}
import scala.util.control.TailCalls.{TailRec, done, tailcall}

object ujsonUtils {
  private val DIGIT_THRESHOLD = 15
  // Create JsonFactory with configuration for large numbers
  private val jsonFactory = new JsonFactory()

  private val mapper: ObjectMapper = new ObjectMapper(jsonFactory)
    .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
    .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS)

  private val nodeFactory = JsonNodeFactory.withExactBigDecimals(true)

  def strValueOf(str: String): Str = Str(str)

  def stringValueOf(value: ujson.Value): String = String.valueOf(value.value)

  def parse(jsonData: String): Value = {
    val processedJson = replaceLargeNumbers(jsonData)
    ujson.read(ujson.Readable.fromString(processedJson))
  }
  //
  //  private def quoteLargeIntegers(json: String): String = {
  //    // Match integers that exceed JavaScript's safe integer range (2^53 - 1)
  //    // This prevents precision loss when parsed as double
  //    json.replaceAll("(:[\\s\\n\\r]*)(\\d{16,})([\\s\\n\\r]*[,\\]\\}])", "\"__LARGE_INT__$1\"")
  //  }

  def replaceLargeNumbers(jsonString: String): String = {
    try {
      val rootNode = mapper.readTree(jsonString)
      val processedNode = processNode(rootNode)
      mapper.writeValueAsString(processedNode)
    } catch {
      case e: Exception =>
        throw new Exception(s"Failed to parse JSON: ${e.getMessage}", e)
    }
  }

  private def processNode(node: JsonNode): JsonNode = {
    node match {
      case objNode: ObjectNode =>
        val newNode = mapper.createObjectNode()
        objNode.fields().asScala.foreach { entry =>
          newNode.set[JsonNode](entry.getKey, processNode(entry.getValue))
        }
        newNode

      case arrNode: ArrayNode =>
        val newNode = mapper.createArrayNode()
        arrNode.elements().asScala.foreach { element =>
          newNode.add(processNode(element))
        }
        newNode

      case numNode: NumericNode =>
        val numberStr = numNode.asText()
        val digitsOnly = numberStr.filter(_.isDigit)

        if (digitsOnly.length > DIGIT_THRESHOLD) {
          new TextNode(s"__LARGE_INT__$numberStr")
        } else {
          numNode
        }

      case _ =>
        node
    }
  }

  def read(s: Readable, trace: Boolean = false): Value.Value = ujson.read(s, trace)

  def write(t: Value.Value,
            indent: Int = -1,
            escapeUnicode: Boolean = false): String = ujson.write(t, indent, escapeUnicode)

  def writeTo(t: Value.Value,
              out: java.io.Writer,
              indent: Int = -1,
              escapeUnicode: Boolean = false): Unit = ujson.writeTo(t, out, indent, escapeUnicode)

  def javaObjectFrom(node: ujson.Value): java.lang.Object = node match {
    case Null => null
    case Bool(value) => value.asInstanceOf[java.lang.Boolean]
    case Num(value) =>
      val num = value.doubleValue()
      if (Math.ceil(num) == Math.floor(num)) {
        val longValue = value.longValue()
        if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE)
          java.lang.Integer.valueOf(value.intValue)
        else
          java.lang.Long.valueOf(longValue)
      } else
        java.lang.Double.valueOf(value.doubleValue())
    case Str(value) =>
      if (value.startsWith("__LARGE_INT__")) {
        val numberStr = value.substring("__LARGE_INT__".length)
        try {
          java.lang.Long.valueOf(numberStr)
        } catch {
          case _: NumberFormatException =>
            // If it doesn't fit in Long, use BigInteger
            new java.math.BigInteger(numberStr)
        }
      } else {
        value
      }
    case Obj(value) => value.map(keyVal => (keyVal._1, javaObjectFrom(keyVal._2))).asJava
    case Arr(value) => value.map(javaObjectFrom).asJava
  }

  // Stack safe implementation using Trampolines for deeply nested objects
  // See: https://stackoverflow.com/a/37585818/4814697
  // https://stackoverflow.com/a/55047640/4814697
  def deepJavaObjectFrom(node: Value): java.lang.Object = {
    def tailrecObjFrom(node: Value): TailRec[java.lang.Object] = {
      node match {
        case Null => done(null)
        case Bool(value) => done(value.asInstanceOf[java.lang.Boolean])
        case Num(value) => done(value.asInstanceOf[java.lang.Double])
        case Str(value) =>
          if (value.startsWith("__LARGE_INT__")) {
            val numberStr = value.substring("__LARGE_INT__".length)
            done(try {
              java.lang.Long.valueOf(numberStr)
            } catch {
              case _: NumberFormatException =>
                new java.math.BigInteger(numberStr)
            })
          } else {
            done(value)
          }
        case Obj(value) => value.toList.foldRight(done(List.empty[(String, java.lang.Object)])) {
          (keyVal, tailrecEntrySet) =>
            for {
              entrySet <- tailrecEntrySet
              javaObj <- tailcall(tailrecObjFrom(keyVal._2))
            } yield entrySet.prepended((keyVal._1, javaObj))
        }.map(list => list.toMap.asJava)
        case Arr(value) => value.foldRight(done(List.empty[java.lang.Object])) {
          (item, tailrecList) =>
            for {
              list <- tailrecList
              javaObj <- tailcall(tailrecObjFrom(item))
            } yield list.::(javaObj)
        }.map(list => list.asJava)
      }
    }

    tailrecObjFrom(node).result
  }
}
