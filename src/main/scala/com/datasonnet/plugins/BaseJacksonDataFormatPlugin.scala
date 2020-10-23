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

import com.datasonnet.spi.AbstractDataFormatPlugin
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.{ArrayNode, BooleanNode, NullNode, NumericNode, ObjectNode, TextNode}
import ujson.Value

import scala.jdk.CollectionConverters.IteratorHasAsScala

abstract class BaseJacksonDataFormatPlugin extends AbstractDataFormatPlugin {
  protected def ujsonFrom(jsonNode: JsonNode): Value = jsonNode match {
    case b: BooleanNode => ujson.Bool(b.booleanValue())
    case n: NumericNode => ujson.Num(n.numberValue().doubleValue())
    case s: TextNode => ujson.Str(s.textValue())
    case o: ObjectNode => ujson.Obj.from(o.fields.asScala.map(entry => (entry.getKey, ujsonFrom(entry.getValue))))
    case a: ArrayNode => ujson.Arr.from(a.elements.asScala.map(ujsonFrom))
    case _: NullNode => ujson.Null
    case _ => throw new IllegalArgumentException("Jackson node " + jsonNode + " not supported!")
  }
}
