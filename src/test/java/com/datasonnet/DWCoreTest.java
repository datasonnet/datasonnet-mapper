package com.datasonnet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

public class DWCoreTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Core";

    @Test
    void testDW_abs(){
        Mapper mapper = new Mapper(lib+pack+".abs(-1)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void testDW_avg(){
        Mapper mapper = new Mapper(lib+pack+".avg([1,2,3,4,5])", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }

    @Test
    void testDW_ceil(){
        Mapper mapper = new Mapper(lib+pack+".ceil(1.5)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testDW_contains(){
        Mapper mapper = new Mapper(lib+pack+".contains([1,2,3,4,5] , 5)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);


        mapper = new Mapper(lib+pack+".contains(\"Hello World\" , \"World\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".contains(\"Hello World\" , \"[e-g]\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
        /* TODO
         */
    }

    @Test
    void testDW_daysBetween(){
        Mapper mapper = new Mapper(lib+pack+".daysBetween(\"2020-07-04T00:00:00.000Z\",\"2020-07-01T00:00:00.000Z\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib+pack+".daysBetween(\"2020-07-04T23:59:59.000Z\",\"2020-07-04T00:00:00.000Z\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);
    }

    @Test
    void testDW_distinctBy(){
        Mapper mapper = new Mapper(lib+pack+".distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item) item)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3,4]", value);

        mapper = new Mapper(lib+pack+".distinctBy([0, 1, 2, 3, 3, 2, 1, 4], function(item,index) index )", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3,3,2,1,4]", value);

        mapper = new Mapper(lib + pack + ".distinctBy({\"a\":0, \"b\":1, \"c\":0}, function(value, key) value)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:0,b:1}", value);

        mapper = new Mapper(lib + pack + ".distinctBy({\"a\":0, \"b\":1, \"c\":0}, function(value) value)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:0,b:1}", value);
    }

    @Test
    void testDW_endsWith(){
        Mapper mapper = new Mapper(lib+pack+".endsWith(\"Hello world\", \"World\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_entriesOf(){
        String input="{\"test1\":\"x\",\"test2\":{\"inTest3\":\"x\",\"inTest4\":{}},\"test10\":[{},{}]}";
        String compare="[{value:x,key:test1},{value:{inTest3:x,inTest4:{}},key:test2},{value:[{},{}],key:test10}]";
        Mapper mapper = new Mapper(lib+pack+".entriesOf(" + input + ")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);
    }

    @Test
    void testDW_filter(){
        Mapper mapper = new Mapper(lib+pack+".filter([0,1,2,3,4,5], function(value) value >= 3)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

        mapper = new Mapper(lib+pack+".filter(null, function(value) value >= 3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib+pack+".filter([0,1,2,3,4,5], function(value,index) index >= 3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

    }

    @Test
    void testDW_filterObject(){
        Mapper mapper = new Mapper(lib+pack+".filterObject({\"a\": 1, \"b\": 2}, function(value,key,index) index == 0)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);

        mapper = new Mapper(lib+pack+".filterObject({\"a\": 1, \"b\": 2}, function(value) value ==0)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{}", value);

        mapper = new Mapper(lib+pack+".filterObject({\"a\": 1, \"b\": 2}, function(value,key) key == 'a' )", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);
    }

    @Test
    void testDW_find(){
        Mapper mapper = new Mapper(lib+pack+".find([1,2,3,4,2,5], 2)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,4]", value);

        mapper = new Mapper(lib+pack+".find(\"aba\", \"a\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,2]", value);

        mapper = new Mapper(lib+pack+".find(\"I heart DataWeave\", \"\\\\w*ea\\\\w*(\\\\b)\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2,8]", value);
        /*TODO Regex version may need work, doesnt seem to be 1:1 with DW
         */
    }

    @Test
    void testDW_flatMap() {
        Mapper mapper = new Mapper(lib + pack + ".flatMap([[3,5],[1,2,5]], function(value) value)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,5,1,2,5]", value);

        mapper = new Mapper(lib + pack + ".flatMap(null, function(value) value)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".flatMap([[3,5],[1,2,5]], function(value,index) value + index)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,6,1,3,7]", value);
    }

    @Test
    void testDW_flatten(){
        Mapper mapper = new Mapper(lib+pack+".flatten([ [0.0, 0], [1,1], [2,3], [5,8] ])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,0,1,1,2,3,5,8]", value);

        mapper = new Mapper(lib+pack+".flatten(null)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib+pack+".flatten([null])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[null]", value);

        mapper = new Mapper(lib+pack+".flatten([[null,null],null])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[null,null,null]", value);
    }

    @Test
    void testDW_floor(){
        Mapper mapper = new Mapper(lib+pack+".floor(1.9)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void testDW_groupBy() {
        Mapper mapper = new Mapper(lib + pack + ".groupBy([   " +
                "   { \"name\": \"Foo\", \"language\": \"Java\" },\n" +
                "   { \"name\": \"Bar\", \"language\": \"Scala\" },\n" +
                "   { \"name\": \"FooBar\", \"language\": \"Java\" }], function(item) item.language)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{Java:[{name:Foo,language:Java},{name:FooBar,language:Java}],Scala:[{name:Bar,language:Scala}]}", value);

        mapper = new Mapper(lib + pack + ".groupBy([   " +
                "   { \"name\": \"Foo\", \"language\": \"Java\" },\n" +
                "   { \"name\": \"Bar\", \"language\": \"Scala\" },\n" +
                "   { \"name\": \"FooBar\", \"language\": \"Java\" }], function(item, index) std.toString(index))\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{0:[{name:Foo,language:Java}],1:[{name:Bar,language:Scala}],2:[{name:FooBar,language:Java}]}", value);

        mapper = new Mapper(lib + pack + ".groupBy({ \"a\" : \"b\", \"c\" : \"d\", \"e\": \"b\"}, function(value) value)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:{a:b,e:b},d:{c:d}}", value);

        mapper = new Mapper(lib + pack + ".groupBy({ \"a\" : \"b\", \"c\" : \"d\", \"e\": \"b\"}, function(value,key) key)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:{a:b},c:{c:d},e:{e:b}}", value);

    }

    @Test
    void testDW_isBlank(){
        Mapper mapper = new Mapper(lib+pack+".isBlank(null)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isBlank(\"      \")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_isDecimal(){
        Mapper mapper = new Mapper(lib+pack+".isDecimal(1.1)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isDecimal(0.0)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isEmpty(){
        Mapper mapper = new Mapper(lib+pack+".isEmpty(null)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isEmpty([])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isEmpty([1,2])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".isEmpty(\"\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isEmpty(\"  \")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".isEmpty({})\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isEmpty({\"a\":1})\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isEven(){
        Mapper mapper = new Mapper(lib+pack+".isEven(2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isEven(3)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isLeapYear(){
        Mapper mapper = new Mapper(lib+pack+".isLeapYear(\"2020-07-04T21:00:00.000Z\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_isInteger(){
        Mapper mapper = new Mapper(lib+pack+".isInteger(1.5)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".isInteger(1.0)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isInteger(1.9)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }


    @Test
    void testDW_isOdd(){
        Mapper mapper = new Mapper(lib+pack+".isOdd(1)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".isOdd(2)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_joinBy(){
        Mapper mapper = new Mapper(lib+pack+".joinBy([1,2,3], \"-\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1-2-3", value);

        mapper = new Mapper(lib+pack+".joinBy([\"a\",\"b\",\"c\"], \"-\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-b-c", value);

        mapper = new Mapper(lib+pack+".joinBy(['a','b','c'], \"-\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-b-c", value);

        mapper = new Mapper(lib+pack+".joinBy([true,false,true], \"-\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true-false-true", value);
    }

    @Test
    void testDW_keysOf(){
        Mapper mapper = new Mapper(lib+pack+".keysOf({ \"a\" : true, \"b\" : 1})\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);
    }

    @Test
    void testDW_lower(){
        Mapper mapper = new Mapper(lib+pack+".lower('Hello World')\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("hello world", value);
    }

    @Test
    void testDW_map(){
        Mapper mapper = new Mapper(lib+pack+".map([1,2], function(item,index) {\"obj\": item+index})\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{obj:1},{obj:3}]", value);

        mapper = new Mapper(lib+pack+".map([1,2], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2]", value);

        mapper = new Mapper(lib+pack+".map(null, function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_mapObject(){
        Mapper mapper = new Mapper(lib+pack+".mapObject({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) { [value] : { [key]: index} } )\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:{a:0},d:{c:1}}", value);

        mapper = new Mapper(lib+pack+".mapObject({\"basic\": 9.99, \"premium\": 53, \"vip\": 398.99}, function(value,key) {[key]: (value + 5)} )\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{premium:58,vip:403.99,basic:14.99}", value);

        mapper = new Mapper(lib+pack+".mapObject({\"basic\": 9.99, \"premium\": 53, \"vip\": 398.99}, function(value) {\"value\": value} )\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{value:398.99}", value);
    }


    @Test
    void testDW_match(){
        Mapper mapper = new Mapper(lib+pack+".match(\"me@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[me@mulesoft.com,me,mulesoft]", value);
    }

    @Test
    void testDW_matches(){
        Mapper mapper = new Mapper(lib+pack+".matches(\"admin123\", \"a.*\\\\d+\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".matches(\"admin123\", \"b.*\\\\d+\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_max(){
        Mapper mapper = new Mapper(lib+pack+".max([1,2,5,33,9])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("33", value);

        mapper = new Mapper(lib+pack+".max([\"a\",\"b\",\"d\",\"c\"])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("d", value);

        mapper = new Mapper(lib+pack+".max([true,false])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_maxBy(){
        Mapper mapper = new Mapper(lib+pack+".maxBy([1,2,5,33,9], function(item) item)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("33", value);

        mapper = new Mapper(lib+pack+".maxBy([\"a\",\"b\"], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("b", value);

        mapper = new Mapper(lib+pack+".maxBy([true,false], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".maxBy([ { \"a\" : 1 }, { \"a\" : 3 }, { \"a\" : 2 } ], function(item) item.a)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:3}", value);
    }

    @Test
    void testDW_min(){
        Mapper mapper = new Mapper(lib+pack+".min([1,2,3,4,5])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib+pack+".min([\"a\",\"b\"])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib+pack+".min([true,false])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_minBy(){
        Mapper mapper = new Mapper(lib+pack+".minBy([1,2,3,4,5], function(item) item)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib+pack+".minBy([\"a\",\"b\"], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a", value);

        mapper = new Mapper(lib+pack+".minBy([true,false], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".minBy([ { \"a\" : 1 }, { \"a\" : 3 }, { \"a\" : 2 } ], function(item) item.a)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1}", value);
    }

    @Test
    void testDW_mod() {
        Mapper mapper = new Mapper(lib + pack + ".mod(3,2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    @Test
    void testDW_namesOf(){
        Mapper mapper = new Mapper(lib+pack+".namesOf({ \"a\" : true, \"b\" : 1})\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);
    }

    @Test
    void testDW_orderBy() {

        Mapper mapper = new Mapper(lib + pack + ".orderBy([0,5,1,3,2,1], function(item) item)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,1,2,3,5]", value);

        mapper = new Mapper(lib + pack + ".orderBy([\"b\",\"a\"], function(item) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);

        mapper = new Mapper(lib + pack + ".orderBy([{ letter: \"e\" }, { letter: \"d\" }], function(item) item.letter)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{letter:d},{letter:e}]", value);
    }

    @Test
    void testDW_pluck() {
        Mapper mapper = new Mapper(lib + pack + ".pluck({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) index )\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1]", value);

        mapper = new Mapper(lib + pack + ".pluck({\"a\":\"b\",\"c\":\"d\"}, function(value,key,index) { [value] : { [key]: index} }\n)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{b:{a:0}},{d:{c:1}}]", value);

    }

    @Test
    void testDW_pow() {
        Mapper mapper = new Mapper(lib + pack + ".pow(2,3)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("8", value);
    }

    @Test
    void testDW_random() {
        Mapper mapper = new Mapper(lib + pack + ".random()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        double dblVal = Double.parseDouble(value);
        assertTrue(dblVal >= 0 && dblVal <= 1);
    }

    @Test
    void testDW_randomint() {
        Mapper mapper = new Mapper(lib + pack + ".randomint(10)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        double dblVal = Double.parseDouble(value);
        assertTrue(dblVal >= 0 && dblVal <= 10);
    }

    @Disabled
    @Test
    void testDW_read() {
        Mapper mapper = new Mapper(lib + pack + ".read()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }

    @Test
    void testDW_readURL() {
        Mapper mapper = new Mapper(lib + pack + ".readUrl(\"https://jsonplaceholder.typicode.com/posts/1\").id\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    //@Disabled
    @Test
    void testDW_reduce() {
        Mapper mapper = new Mapper(lib + pack + ".reduce([2,3], function(it,acc) it+acc, 0)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + pack + ".reduce([1,2,3,4], function(it,acc) acc+it, 0)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        mapper = new Mapper(lib + pack + ".reduce([1,2,3,4], function(it,acc) acc+\"\"+it,\"\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1234", value);

        mapper = new Mapper(lib + pack + ".reduce([], function(it,acc) acc+it, null)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_replace() {
        Mapper mapper = new Mapper(lib + pack + ".replace(\"123-456-7890\", \".*-\", \"\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("7890", value);

        mapper = new Mapper(lib + pack + ".replace(\"abc123def\", \"[b13e]\", \"-\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("a-c-2-d-f", value);

        mapper = new Mapper(lib + pack + ".replace(\"admin123\", \"123\", \"ID\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("adminID", value);
    }

    @Test
    void testDW_round() {
        Mapper mapper = new Mapper(lib + pack + ".round(1.5)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testDW_scan() {
        Mapper mapper = new Mapper(lib + pack + ".scan(\"anypt@mulesoft.com,max@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[anypt@mulesoft.com,anypt,mulesoft],[max@mulesoft.com,max,mulesoft]]", value);
    }

    @Test
    void testDW_sizeOf() {
        Mapper mapper = new Mapper(lib + pack + ".sizeOf([1,2,3,4,5])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + pack + ".sizeOf(\"Hello\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib + pack + ".sizeOf({\"a\":0})\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + pack + ".sizeOf(null)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);
    }

    @Test
    void testDW_splitBy() {
        String input ="{"+
                "\"split1\": " + lib + pack + ".splitBy(\"a-b-c\",\"^*.b.\")," +
                "\"split2\": " + lib + pack + ".splitBy(\"hello world\",\"\\\\s\")," +
                "\"split3\": " + lib + pack + ".splitBy(\"no match\",\"^s\")," +
                "\"split4\": " + lib + pack + ".splitBy(\"no match\",\"^n..\")," +
                "\"split5\": " + lib + pack + ".splitBy(\"a1b2c3d4A1B2C3D\",\"^*[0-9A-Z]\")," +
                "\"split6\": " + lib + pack + ".splitBy(\"a-b-c\",\"-\")," +
                "\"split7\": " + lib + pack + ".splitBy(\"hello world\",\"\")," +
                "\"split8\": " + lib + pack + ".splitBy(\"first,middle,last\",\",\")," +
                "\"split9\": " + lib + pack + ".splitBy(\"no split\",\"NO\")" +
                "}";
        String comparison="{split1:[a,c]," +
                "split2:[hello,world]," +
                "split3:[no match]," +
                "split4:[,match]," +
                "split5:[a,b,c,d]," +
                "split6:[a,b,c]," +
                "split7:[h,e,l,l,o, ,w,o,r,l,d]," +
                "split8:[first,middle,last]," +
                "split9:[no split]}";
        Mapper mapper = new Mapper(input, new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(comparison, value);
    }

    @Test
    void testDW_sqrt() {
        Mapper mapper = new Mapper(lib + pack + ".sqrt(4)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testDW_startsWith() {
        Mapper mapper = new Mapper(lib + pack + ".startsWith(\"Hello World\", \"Hello\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_sum() {
        Mapper mapper = new Mapper(lib + pack + ".sum([1,2,3,4,5])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }

    @Test
    void testDW_to() {
        Mapper mapper = new Mapper(lib + pack + ".to(0, 3)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1,2,3]", value);
    }

    @Test
    void testDW_trim() {
        Mapper mapper = new Mapper(lib + pack + ".trim(\"  Hello     World     \")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Hello     World", value);
    }

    @Test
    void testDW_typeOf() {
        Mapper mapper = new Mapper(lib + pack + ".typeOf([])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("array", value);

        mapper = new Mapper(lib + pack + ".typeOf({})\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("object", value);

        mapper = new Mapper(lib + pack + ".typeOf(\"\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("string", value);

        mapper = new Mapper(lib + pack + ".typeOf(function(x) x)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("function", value);

        mapper = new Mapper(lib + pack + ".typeOf(0)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("number", value);
    }

    @Test
    void testDW_unzip() {
        Mapper mapper = new Mapper(lib + pack + ".unzip([ [0,\"a\",'c'], [1,\"b\",'c'], [2,\"c\",'c'],[ 3,\"d\",'c'] ])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[0,1,2,3],[a,b,c,d],[c,c,c,c]]", value);
    }

    @Test
    void testDW_upper() {
        Mapper mapper = new Mapper(lib + pack + ".upper(\"HeLlO WoRlD\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("HELLO WORLD", value);
    }

    @Test
    void testDW_uuid() {
        Mapper mapper = new Mapper(lib + pack + ".uuid()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(5, value.split("-").length);
    }

    @Test
    void testDW_valuesOf(){
        Mapper mapper = new Mapper(lib+pack+".valuesOf({ \"a\" : true, \"b\" : 1, \"c\":[], \"d\":\"d\"})\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[true,1,[],d]", value);
    }

    @Test
    void testDW_zip() {
        Mapper mapper = new Mapper(lib + pack + ".zip([1,2,3,4,5], [\"a\",\"b\"])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,a],[2,b]]", value);
    }





    /********************************       ARRAYS             *************************************************************/
    @Test
    void testDW_countBy() {
        Mapper mapper = new Mapper(lib + "Arrays.countBy([1,2,3,4,5], function(it) it > 2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }





    /********************************       STRINGS             *************************************************************/
    @Test
    void testDW_appendIfMissing() {
        Mapper mapper = new Mapper(lib + "Strings.appendIfMissing(\"abc\", \"xyz\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);

        mapper = new Mapper(lib + "Strings.appendIfMissing(\"abcxyz\", \"xyz\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abcxyz", value);
    }

    @Test
    void testDW_camelize() {
        Mapper mapper = new Mapper(lib + "Strings.camelize(\"customer_first_name\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + "Strings.camelize(\"_customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + "Strings.camelize(\"_______customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("customerFirstName", value);

        mapper = new Mapper(lib + "Strings.camelize(null)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Disabled
    @Test
    void testDW_capitalize() {
        Mapper mapper = new Mapper(lib + "Strings.capitalize(\"customer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer", value);

        mapper = new Mapper(lib + "Strings.capitalize(\"customer_first_name\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer First Name", value);

        mapper = new Mapper(lib + "Strings.capitalize(\"customer NAME\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Customer Name", value);

        mapper = new Mapper(lib + "Strings.capitalize(null)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testDW_charCode() {
        Mapper mapper = new Mapper(lib + "Strings.charCode(\"Master\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);

        mapper = new Mapper(lib + "Strings.charCode(\"M\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("77", value);
    }

    @Test
    void testDW_charCodeAt() {
        Mapper mapper = new Mapper(lib + "Strings.charCodeAt(\"charCodeAt\", 4)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("67", value);

        mapper = new Mapper(lib + "Strings.charCodeAt(\"charCodeAt\", 8)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("65", value);
    }

    @Test
    void testDW_fromCharCode() {
        Mapper mapper = new Mapper(lib + "Strings.fromCharCode(67)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("C", value);

        mapper = new Mapper(lib + "Strings.fromCharCode(65)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("A", value);
    }

    @Test
    void testDW_isAlpha() {
        Mapper mapper = new Mapper(lib + "Strings.isAlpha(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isAlpha(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + "Strings.isAlpha(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isAlpha(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isAlphanumeric() {
        Mapper mapper = new Mapper(lib + "Strings.isAlphanumeric(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isAlphanumeric(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isAlphanumeric(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isAlphanumeric(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_isLowerCase() {
        Mapper mapper = new Mapper(lib + "Strings.isLowerCase(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isLowerCase(\"ecvt4\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + "Strings.isLowerCase(\"eCvt\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + "Strings.isLowerCase(true)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isLowerCase(45)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testDW_isNumeric() {
        Mapper mapper = new Mapper(lib + "Strings.isNumeric(\"sdfvxer\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib + "Strings.isNumeric(\"5334\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + "Strings.isNumeric(100)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

}
