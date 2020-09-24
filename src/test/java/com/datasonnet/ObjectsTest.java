package com.datasonnet;

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

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ObjectsTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds";
    private final String pack = ".objects";

    @Test
    void testObjects_divideBy() {
        String input = "{\"a\": 1, " +
                "\"b\" : true, " +
                "\"c\" : 2, " +
                "\"d\" : false, " +
                "\"e\" : 3}";
        String compare = "[{a:1,b:true},{c:2,d:false},{e:3}]";
        Mapper mapper = new Mapper(lib + pack + ".divideBy(" + input + ", 2)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);

        compare = "[{a:1,b:true,c:2},{d:false,e:3}]";
        mapper = new Mapper(lib + pack + ".divideBy(" + input + ", 3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);
    }

    @Test
    void testObjects_entriesOf() {
        String input = "{\"test1\":\"x\",\"test2\":{\"inTest3\":\"x\",\"inTest4\":{}},\"test10\":[{},{}]}";
        String compare = "[{value:x,key:test1},{value:{inTest3:x,inTest4:{}},key:test2},{value:[{},{}],key:test10}]";
        Mapper mapper = new Mapper(lib + ".entriesOf(" + input + ")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);
    }

    @Test
    void testObjects_everyEntry() {
        Mapper mapper = new Mapper(lib + pack + ".everyEntry({\"a\":\"\",\"b\":\"123\"}, function(value) std.isString(value))", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".everyEntry({\"a\":\"\",\"b\":\"123\"}, function(value,key) key ==\"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".everyEntry({\"b\":\"\",\"b\":\"123\"}, function(value,key) key ==\"b\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".everyEntry(null, function(value) std.isString(value))", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".everyEntry(null, function(value) std.isString(value))", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testObjects_keysOf() {
        Mapper mapper = new Mapper(lib + ".keysOf({ \"a\" : true, \"b\" : 1})\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);
    }

    @Test
    void testObjects_mergeWith() {
        String obj1, obj2;
        obj1 = "{\"a\": true, \"b\": 1}";
        obj2 = "{\"a\": false, \"c\": \"Test\"}";
        Mapper mapper = new Mapper(lib + pack + ".mergeWith(" + obj1 + ", " + obj2 + ")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:false,b:1,c:Test}", value);

        mapper = new Mapper(lib + pack + ".mergeWith(" + obj1 + ", null)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:true,b:1}", value);

        mapper = new Mapper(lib + pack + ".mergeWith(null, " + obj2 + ")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:false,c:Test}", value);
    }

    @Test
    void testObjects_someEntry() {
        Mapper mapper = new Mapper(lib + pack + ".someEntry({ \"a\" : true, \"b\" : 1}, function(value,key) value == true)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".someEntry({ \"a\" : true, \"b\" : 1}, function(value,key) value == false)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + pack + ".someEntry({ \"a\" : true, \"b\" : 1}, function(value,key) key == \"a\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);


        mapper = new Mapper(lib + pack + ".someEntry(null, function(value,key) key == \"a\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }


    @Test
    void testObjects_takeWhile() {
        Mapper mapper = new Mapper(lib + pack + ".takeWhile({\"a\":1,\"b\":1,\"c\":5,\"d\":1}, function(value,key) value == 1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1,b:1}", value);

        mapper = new Mapper(lib + pack + ".takeWhile({\"a\":1,\"b\":1,\"c\":5,\"d\":1}, function(value,key) key == \"a\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);
    }

    @Test
    void testObjects_valuesOf() {
        Mapper mapper = new Mapper(lib + ".valuesOf({ \"a\" : true, \"b\" : 1, \"c\":[], \"d\":\"d\"})\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[true,1,[],d]", value);
    }
}
