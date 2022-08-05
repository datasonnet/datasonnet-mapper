package com.datasonnet;

/*-
 * Copyright 2019-2022 the original author or authors.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class CoreTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds";
    private final String mathPack = ".math";
    private final String datetimePack = ".datetime";
    private final String localDateTimePack = ".localdatetime";

    @Test
    void test_abs() {
        Mapper mapper = new Mapper(lib + mathPack + ".abs(-1)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void test_avg() {
        Mapper mapper = new Mapper(lib + mathPack + ".avg([1,2,3,4,5])", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }

    @Test
    void test_ceil() {
        Mapper mapper = new Mapper(lib + mathPack + ".ceil(1.5)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void test_contains() {
        Mapper mapper = new Mapper(lib + ".contains([1,2,3,4,5] , 5)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);


        mapper = new Mapper(lib + ".contains(\"Hello World\" , \"World\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".contains(\"Hello World\" , \"[e-g]\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_daysBetween() {
        Mapper mapper = new Mapper(lib + datetimePack + ".daysBetween(\"2020-07-04T00:00:00.000Z\",\"2020-07-01T00:00:00.000Z\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib + datetimePack + ".daysBetween(\"2020-07-04T23:59:59.000Z\",\"2020-07-04T00:00:00.000Z\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);
    }

    @Test
    void test_distinctBy() {
        Mapper mapper = new Mapper(lib + ".distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item) item)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3,4]", value);

        mapper = new Mapper(lib + ".distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item,index) index )", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3,3,2,1,4]", value);

        mapper = new Mapper(lib + ".distinctBy({\"a\":0, \"b\":1, \"c\":0}, function(value, key) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:0,b:1}", value);

        mapper = new Mapper(lib + ".distinctBy({\"a\":0, \"b\":1, \"c\":0}, function(value) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:0,b:1}", value);
    }

    @Test
    void test_endsWith() {
        Mapper mapper = new Mapper(lib + ".endsWith(\"Hello world\", \"World\")", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_filter() {
        Mapper mapper = new Mapper(lib + ".filter([0,1,2,3,4,5], function(value) value >= 3)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

        mapper = new Mapper(lib + ".filter(null, function(value) value >= 3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + ".filter([0,1,2,3,4,5], function(value,index) index >= 3)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

    }

    @Test
    void test_filterObject() {
        Mapper mapper = new Mapper(lib + ".filterObject({\"a\": 1, \"b\": 2}, function(value,key,index) index == 0)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);

        mapper = new Mapper(lib + ".filterObject({\"a\": 1, \"b\": 2}, function(value) value ==0)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{}", value);

        mapper = new Mapper(lib + ".filterObject({\"a\": 1, \"b\": 2}, function(value,key) key == 'a' )", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);
    }

    @Test
    void test_find() {
        Mapper mapper = new Mapper(lib + ".find([1,2,3,4,2,5], 2)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,4]", value);

        mapper = new Mapper(lib + ".find(\"aba\", \"a\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,2]", value);

        mapper = new Mapper(lib + ".find(\"I heart DataWeave\", \"\\\\w*ea\\\\w*(\\\\b)\")", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2,8]", value);
        /*TODO Regex version may need work, doesnt seem to be 1:1 with DW
         */
    }

    @Test
    void test_flatMap() {
        Mapper mapper = new Mapper(lib + ".flatMap([[3,5],[1,2,5]], function(value) value)", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,5,1,2,5]", value);

        mapper = new Mapper(lib + ".flatMap(null, function(value) value)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + ".flatMap([[3,5],[1,2,5]], function(value,index) value + index)", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,6,1,3,7]", value);
    }

    @Test
    void test_flatten() {
        Mapper mapper = new Mapper(lib + ".flatten([ [0.0, 0], [1,1], [2,3], [5,8] ])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,0,1,1,2,3,5,8]", value);

        mapper = new Mapper(lib + ".flatten(null)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + ".flatten([null])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[null]", value);

        mapper = new Mapper(lib + ".flatten([[null,null],null])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[null,null,null]", value);
    }

    @Test
    void test_floor() {
        Mapper mapper = new Mapper(lib + mathPack + ".floor(1.9)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void test_groupBy() {
        Mapper mapper = new Mapper(lib + ".groupBy([   " +
                "   { \"name\": \"Foo\", \"language\": \"Java\" },\n" +
                "   { \"name\": \"Bar\", \"language\": \"Scala\" },\n" +
                "   { \"name\": \"FooBar\", \"language\": \"Java\" }], function(item) item.language)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{Java:[{name:Foo,language:Java},{name:FooBar,language:Java}],Scala:[{name:Bar,language:Scala}]}", value);

        mapper = new Mapper(lib + ".groupBy([   " +
                "   { \"name\": \"Foo\", \"language\": \"Java\" },\n" +
                "   { \"name\": \"Bar\", \"language\": \"Scala\" },\n" +
                "   { \"name\": \"FooBar\", \"language\": \"Java\" }], function(item, index) std.toString(index))\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{0:[{name:Foo,language:Java}],1:[{name:Bar,language:Scala}],2:[{name:FooBar,language:Java}]}", value);

        mapper = new Mapper(lib + ".groupBy({ \"a\" : \"b\", \"c\" : \"d\", \"e\": \"b\"}, function(value) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:{a:b,e:b},d:{c:d}}", value);

        mapper = new Mapper(lib + ".groupBy({ \"a\" : \"b\", \"c\" : \"d\", \"e\": \"b\"}, function(value,key) key)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:{a:b},c:{c:d},e:{e:b}}", value);

        //string cast validation
        mapper = new Mapper(lib + ".groupBy({ \"a\":1, \"c\" :2, \"e\":3}, function(value) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{1:{a:1},2:{c:2},3:{e:3}}", value);

    }

    @Test
    void test_isBlank() {
        Mapper mapper = new Mapper(lib + ".isBlank(null)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isBlank(\"      \")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_isDecimal() {
        Mapper mapper = new Mapper(lib + ".isDecimal(1.1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isDecimal(0.0)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_isEmpty() {
        Mapper mapper = new Mapper(lib + ".isEmpty(null)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isEmpty([])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isEmpty([1,2])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + ".isEmpty(\"\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isEmpty(\"  \")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + ".isEmpty({})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isEmpty({\"a\":1})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_isEven() {
        Mapper mapper = new Mapper(lib + ".isEven(2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isEven(3)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_isLeapYear() {
        Mapper mapper = new Mapper(lib + datetimePack + ".isLeapYear(\"2020-07-04T21:00:00.000Z\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_isInteger() {
        Mapper mapper = new Mapper(lib + ".isInteger(1.5)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + ".isInteger(1.0)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isInteger(1.9)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }


    @Test
    void test_isOdd() {
        Mapper mapper = new Mapper(lib + ".isOdd(1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".isOdd(2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_joinBy() {
        Mapper mapper = new Mapper(lib + ".joinBy([1.0,2,3.5], \"-\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1-2-3.5", value);

        mapper = new Mapper(lib + ".joinBy([\"a\",\"b\",\"c\"], \"-\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-b-c", value);

        mapper = new Mapper(lib + ".joinBy(['a','b','c'], \"-\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-b-c", value);

        mapper = new Mapper(lib + ".joinBy([true,false,true], \"-\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true-false-true", value);
    }

    @Test
    void test_lower() {
        Mapper mapper = new Mapper(lib + ".lower('Hello World')\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("hello world", value);
    }

    @Test
    void test_map() {
        Mapper mapper = new Mapper(lib + ".map([1,2], function(item,index) {\"obj\": item+index})\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{obj:1},{obj:3}]", value);

        mapper = new Mapper(lib + ".map([1,2], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2]", value);

        mapper = new Mapper(lib + ".map(null, function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void test_mapObject() {
        Mapper mapper = new Mapper(lib + ".mapObject({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) { [value] : { [key]: index} } )\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:{a:0},d:{c:1}}", value);

        mapper = new Mapper(lib + ".mapObject({\"basic\": 9.99, \"premium\": 53, \"vip\": 398.99}, function(value,key) {[key]: (value + 5)} )\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{premium:58,vip:403.99,basic:14.99}", value);

        mapper = new Mapper(lib + ".mapObject({\"basic\": 9.99, \"premium\": 53, \"vip\": 398.99}, function(value) {\"value\": value} )\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{value:398.99}", value);
    }


    @Test
    void test_match() {
        Mapper mapper = new Mapper(lib + ".match(\"me@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[me@mulesoft.com,me,mulesoft]", value);
    }

    @Test
    void test_matches() {
        Mapper mapper = new Mapper(lib + ".matches(\"admin123\", \"a.*\\\\d+\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".matches(\"admin123\", \"b.*\\\\d+\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_max() {
        Mapper mapper = new Mapper(lib + ".max([1,2,5,33,9])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("33", value);

        mapper = new Mapper(lib + ".max([\"a\",\"b\",\"d\",\"c\"])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("d", value);

        mapper = new Mapper(lib + ".max([true,false])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_maxBy() {
        Mapper mapper = new Mapper(lib + ".maxBy([1,2,5,33,9], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("33", value);

        mapper = new Mapper(lib + ".maxBy([\"a\",\"b\"], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("b", value);

        mapper = new Mapper(lib + ".maxBy([true,false], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".maxBy([ { \"a\" : 1 }, { \"a\" : 3 }, { \"a\" : 2 } ], function(item) item.a)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:3}", value);
    }

    @Test
    void test_min() {
        Mapper mapper = new Mapper(lib + ".min([1,2,3,4,5])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + ".min([\"a\",\"b\"])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + ".min([true,false])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void test_minBy() {
        Mapper mapper = new Mapper(lib + ".minBy([1,2,3,4,5], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + ".minBy([\"a\",\"b\"], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib + ".minBy([true,false], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + ".minBy([ { \"a\" : 1 }, { \"a\" : 3 }, { \"a\" : 2 } ], function(item) item.a)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);
    }

    @Test
    void test_mod() {
        Mapper mapper = new Mapper(lib + mathPack + ".mod(3,2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void test_orderBy() {

        Mapper mapper = new Mapper(lib + ".orderBy([0,5,1,3,2,1], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,1,2,3,5]", value);

        mapper = new Mapper(lib + ".orderBy([0,5,1,3,2,1], function(item,ind) ind)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,5,1,3,2,1]", value);

        mapper = new Mapper(lib + ".orderBy([\"b\",\"a\"], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);

        mapper = new Mapper(lib + ".orderBy([{ letter: \"e\" }, { letter: \"d\" }], function(item) item.letter)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{letter:d},{letter:e}]", value);

        mapper = new Mapper(lib + ".orderBy([{ letter: \"e\" }, { letter: \"d\" }], function(item,ind) ind)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{letter:e},{letter:d}]", value);

        mapper = new Mapper(lib + ".orderBy({d:3,a:5,e:2,z:1,c:4}, function(value,key) key)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:5,c:4,d:3,e:2,z:1}", value);

        mapper = new Mapper(lib + ".orderBy({d:3,a:5,e:20,z:1,c:4}, function(value) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{z:1,d:3,c:4,a:5,e:20}", value);

        mapper = new Mapper(lib + ".orderBy({d:3,a:5,e:20,z:1,c:4}, function(value,key) value)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{z:1,d:3,c:4,a:5,e:20}", value);

    }

    @Test
    void test_mapEntries() {
        Mapper mapper = new Mapper(lib + ".mapEntries({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) index )\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1]", value);

        mapper = new Mapper(lib + ".mapEntries({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) { [value] : { [key]: index} }\n)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{b:{a:0}},{d:{c:1}}]", value);

    }

    @Test
    void test_pow() {
        Mapper mapper = new Mapper(lib + mathPack + ".pow(2,3)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("8", value);
    }

    @Test
    void test_random() {
        Mapper mapper = new Mapper(lib + mathPack + ".random()\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        double dblVal = Double.parseDouble(value);
        assertTrue(dblVal >= 0 && dblVal <= 1);
    }

    @Test
    void test_randomInt() {
        Mapper mapper = new Mapper(lib + mathPack + ".randomInt(10)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        double dblVal = Double.parseDouble(value);
        assertTrue(dblVal >= 0 && dblVal <= 10);
    }

    @Disabled
    @Test
    void test_read() {
        Mapper mapper = new Mapper(lib + ".read()\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }

    @Test
    void test_readUrl() {
        Mapper mapper = new Mapper(lib + ".readUrl(\"https://jsonplaceholder.typicode.com/posts/1\").id\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper("ds.readUrl(\"classpath://readUrlTest.json\").message\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Hello World!", value);
    }

    @Test
    void test_foldLeft() {

        Mapper mapper = new Mapper(lib + ".foldLeft([2,3], 0, function(acc,it) it+acc)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + ".foldLeft([1,2,3,4], 0, function(acc,it) acc+it)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        mapper = new Mapper(lib + ".foldLeft([1,2,3,4],\"\", function(acc,it) acc+\"\"+it)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1234", value);

        mapper = new Mapper(lib + ".foldLeft([], null, function(acc,it) acc+it)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void test_replace() {
        Mapper mapper = new Mapper(lib + ".replace(\"123-456-7890\", \".*-\", \"\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("7890", value);

        mapper = new Mapper(lib + ".replace(\"abc123def\", \"[b13e]\", \"-\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-c-2-d-f", value);

        mapper = new Mapper(lib + ".replace(\"admin123\", \"123\", \"ID\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("adminID", value);
    }

    @Test
    void test_round() {
        Mapper mapper = new Mapper(lib + mathPack + ".round(1.5)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void test_scan() {
        Mapper mapper = new Mapper(lib + ".scan(\"anypt@mulesoft.com,max@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[anypt@mulesoft.com,anypt,mulesoft],[max@mulesoft.com,max,mulesoft]]", value);
    }

    @Test
    void test_select() {
        Mapper mapper = new Mapper(lib + ".select({a:{b:{c:10}}}, \"a.b.c\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        mapper = new Mapper(lib + ".select({a:{b:{c:10}}}, \"a.b.d\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void test_sizeOf() {
        Mapper mapper = new Mapper(lib + ".sizeOf([1,2,3,4,5])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + ".sizeOf(\"Hello\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + ".sizeOf({\"a\":0})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + ".sizeOf(null)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);
    }

    @Test
    void test_splitBy() {
        String input = "{" +
                "\"split1\": " + lib + ".splitBy(\"a-b-c\",\"^*.b.\")," +
                "\"split2\": " + lib + ".splitBy(\"hello world\",\"\\\\s\")," +
                "\"split3\": " + lib + ".splitBy(\"no match\",\"^s\")," +
                "\"split4\": " + lib + ".splitBy(\"no match\",\"^n..\")," +
                "\"split5\": " + lib + ".splitBy(\"a1b2c3d4A1B2C3D\",\"^*[0-9A-Z]\")," +
                "\"split6\": " + lib + ".splitBy(\"a-b-c\",\"-\")," +
                "\"split7\": " + lib + ".splitBy(\"hello world\",\"\")," +
                "\"split8\": " + lib + ".splitBy(\"first,middle,last\",\",\")," +
                "\"split9\": " + lib + ".splitBy(\"no split\",\"NO\")" +
                "}";
        String comparison = "{split1:[a,c]," +
                "split2:[hello,world]," +
                "split3:[no match]," +
                "split4:[,match]," +
                "split5:[a,b,c,d]," +
                "split6:[a,b,c]," +
                "split7:[h,e,l,l,o, ,w,o,r,l,d]," +
                "split8:[first,middle,last]," +
                "split9:[no split]}";
        Mapper mapper = new Mapper(input, new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(comparison, value);
    }

    @Test
    void test_sqrt() {
        Mapper mapper = new Mapper(lib + mathPack + ".sqrt(4)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void test_startsWith() {
        Mapper mapper = new Mapper(lib + ".startsWith(\"Hello World\", \"Hello\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void test_sum() {
        Mapper mapper = new Mapper(lib + mathPack + ".sum([1,2,3,4,5])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }

    @Test
    void test_range() {
        Mapper mapper = new Mapper(lib + ".range(0, 3)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3]", value);
    }

    @Test
    void test_toString() {
        Mapper mapper = new Mapper(lib + ".toString(5)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + ".toString(true)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + ".toString(null)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void test_trim() {
        Mapper mapper = new Mapper(lib + ".trim(\"  Hello     World     \")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Hello     World", value);
    }

    @Test
    void test_typeOf() {
        Mapper mapper = new Mapper(lib + ".typeOf([])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("array", value);

        mapper = new Mapper(lib + ".typeOf({})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("object", value);

        mapper = new Mapper(lib + ".typeOf(\"\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("string", value);

        mapper = new Mapper(lib + ".typeOf(function(x) x)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("function", value);

        mapper = new Mapper(lib + ".typeOf(0)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("number", value);
    }

    @Test
    void test_unzip() {
        Mapper mapper = new Mapper(lib + ".unzip([ [0,\"a\",'c'], [1,\"b\",'c'], [2,\"c\",'c'],[ 3,\"d\",'c'] ])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[0,1,2,3],[a,b,c,d],[c,c,c,c]]", value);
    }

    @Test
    void test_upper() {
        Mapper mapper = new Mapper(lib + ".upper(\"HeLlO WoRlD\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("HELLO WORLD", value);
    }

    @Test
    void test_uuid() {
        Mapper mapper = new Mapper(lib + ".uuid()\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(5, value.split("-").length);
        Pattern pattern = Pattern.compile("[^a-f0-9\\-]");
        Matcher match = pattern.matcher(value.toLowerCase());
        assertFalse(match.find());

        logger.info("UUID: " + value);
    }

    @Test
    void test_valuesOf() {
        Mapper mapper = new Mapper(lib + ".valuesOf({ \"a\" : true, \"b\" : 1, \"c\":[], \"d\":\"d\"})\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[true,1,[],d]", value);
    }

    @Test
    void test_zip() {
        Mapper mapper = new Mapper(lib + ".zip([1,2,3,4,5], [\"a\",\"b\"])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,a],[2,b]]", value);
    }

    @Test
    void test_combine() {
        Mapper mapper = new Mapper(lib + ".combine([1],[2])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2]", value);

        mapper = new Mapper(lib + ".combine({a:1},{b:2})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1,b:2}", value);

        mapper = new Mapper(lib + ".combine(1,2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);

        mapper = new Mapper(lib + ".combine(1.2,2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1.22", value);

        mapper = new Mapper(lib + ".combine(\"1\",2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);

        mapper = new Mapper(lib + ".combine(\"1\",\"2\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);
    }


    @Test
    void test_remove() {
        Mapper mapper = new Mapper(lib + ".remove([1,2,1],1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2]", value);

        mapper = new Mapper(lib + ".remove({a:1,b:2},\"a\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:2}", value);

        mapper = new Mapper(lib + ".remove({a:1,b:2,c:3},[\"a\",\"c\"])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:2}", value);
    }

    @Test
    void test_removeMatch() {
        Mapper mapper = new Mapper(lib + ".removeMatch([1,2,1],[1,3])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2]", value);

        mapper = new Mapper(lib + ".removeMatch({a:1,b:2},{a:1,c:3})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:2}", value);
    }

    @Test
    void test_append() {
        Mapper mapper = new Mapper(lib + ".append([1,2,3],4)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,4]", value);
    }

    @Test
    void test_prepend() {
        Mapper mapper = new Mapper(lib + ".prepend([1,2,3],4)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[4,1,2,3]", value);
    }


    @Test
    void test_reverse() {
        Mapper mapper = new Mapper(lib + ".reverse({first: '1', second: '2'})\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{second:2,first:1}", value);

        mapper = new Mapper(lib + ".reverse([1,2,3,4])\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[4,3,2,1]", value);

        mapper = new Mapper(lib + ".reverse(\"Hello\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("olleH", value);
    }

    @Test
    void test_or() {
        Mapper mapper = new Mapper(lib + ".or(null, 'abc')\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abc", value);
    }

    @Test
    void localDateTime_now() {
        Mapper mapper = new Mapper(lib + localDateTimePack + ".now()\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertNotNull(value);
    }

    @Test
    void localDateTime_offset() {
        Mapper mapper = new Mapper(lib + localDateTimePack + ".offset(\"2019-07-22T21:00:00\", \"P1Y1D\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-07-23T21:00:00", value);
    }

    @Test
    void localDateTime_compare() {
        Mapper mapper = new Mapper(lib + localDateTimePack + ".compare(\"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\", \"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\")\n",
                new ArrayList<>(),
                new HashMap<>(),
                true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);
    }

}
