package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sjsonnet.Val;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

public class DWCoreTest {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private String lib = "DW" +".";
    private String pack = "Core";

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

        /* TODO
        mapper = new Mapper(lib+pack+".contains(\"Hello World\" , \"World\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".contains(\"Hello World\" , REGEX)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
         */
    }

    /* TODO
    @Test
    void testDW_distinctBy(){
        Mapper mapper = new Mapper(lib+pack+".distinctBy({\"name\": \"Jake\"}, function(key,value) key ==\"name\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }
    */

    @Test
    void testDW_endsWith(){
        Mapper mapper = new Mapper(lib+pack+".endsWith(\"Hello world\", \"World\")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }


    @Test
    void testDW_filter(){
        Mapper mapper = new Mapper(lib+pack+".filter([0,1,2,3,4,5], function(value) value >= 3)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);
    }

    /* TODO
    @Test
    void testDW_filterObject(){
        Mapper mapper = new Mapper(lib+pack+".filterObject()", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);
    }
    */

    @Test
    void testDW_find(){
        Mapper mapper = new Mapper(lib+pack+".find([1,2,3,4,2,5], 2)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,4]", value);

        //TODO
    }

    /*TODO
    @Test
    void testDW_flatMap(){
        Mapper mapper = new Mapper(lib+pack+".find([1,2,3,4,2,5], 2)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,4]", value);
    }
     */

    @Test
    void testDW_flatten(){
        Mapper mapper = new Mapper(lib+pack+".flatten([ [0.0, 0], [1,1], [2,3], [5,8] ])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,0,1,1,2,3,5,8]", value);
    }

    @Test
    void testDW_floor(){
        Mapper mapper = new Mapper(lib+pack+".floor(1.9)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    /*TODO
    @Test
    void testDW_groupBy(){
        Mapper mapper = new Mapper(lib+pack+".groupBy(null)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }
    */

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
    }

    @Test
    void testDW_isInteger(){
        Mapper mapper = new Mapper(lib+pack+".isInteger(1.5)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".isInteger(1.0)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    /*TODO
    @Test
    void testDW_isLeapYear(){
        Mapper mapper = new Mapper(lib+pack+".isEven(2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }
    */

    @Test
    void testDW_isOdd(){
        Mapper mapper = new Mapper(lib+pack+".isOdd(1)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
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

        mapper = new Mapper(lib+pack+".map([1,2], function(item,index) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2]", value);

        mapper = new Mapper(lib+pack+".map(null, function(item,index) item)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    /*TODO
    @Test
    void testDW_mapObject(){
        Mapper mapper = new Mapper(lib+pack+".mapObject()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("hello world", value);
    }
     */

    @Test
    void testDW_match(){
        Mapper mapper = new Mapper(lib+pack+".match(\"me@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[me@mulesoft.com,me]", value);
    }

    @Test
    void testDW_matches(){
        Mapper mapper = new Mapper(lib+pack+".matches(\"me@mulesoft.com\", \"([a-z]*)@([a-z]*).com\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }


    @Test
    void testDW_max(){
        Mapper mapper = new Mapper(lib+pack+".max([1,2,3,4,5])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

        mapper = new Mapper(lib+pack+".max([\"a\",\"b\"])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("b", value);

        mapper = new Mapper(lib+pack+".max([true,false])\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDW_maxBy(){
        Mapper mapper = new Mapper(lib+pack+".maxBy([1,2,3,4,5], function(item) item)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("5", value);

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

    /*TODO
    @Test
    void testDW_orderBy() {
        Mapper mapper = new Mapper(lib + pack + ".mod(3,2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }

    TODO
    @Test
    void testDW_pluck() {
        Mapper mapper = new Mapper(lib + pack + ".mod(3,2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);
    }
    */

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

    /*TODO
    @Test
    void testDW_read() {
        Mapper mapper = new Mapper(lib + pack + ".read()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }
    */

    /*TODO
    @Test
    void testDW_readURL() {
        Mapper mapper = new Mapper(lib + pack + ".read()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }
    */

    /*TODO
    @Test
    void testDW_reduce() {
        Mapper mapper = new Mapper(lib + pack + ".read()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }
    */

    /*TODO
    @Test
    void testDW_replace() {
        Mapper mapper = new Mapper(lib + pack + ".read()\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);
    }
    */


    @Test
    void testDW_round() {
        Mapper mapper = new Mapper(lib + pack + ".round(1.5)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    /*TODO
    @Test
    void testDW_scan() {
        Mapper mapper = new Mapper(lib + pack + ".round(1.5)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }
    */

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

    /*TODO
    @Test
    void testDW_splitBy() {
        Mapper mapper = new Mapper(lib + pack + ".startsWith(\"Hello World\", \"Hello\")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }*/

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
    void testDW_zip() {
        Mapper mapper = new Mapper(lib + pack + ".zip([1,2,3,4,5], [\"a\",\"b\"])\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,a],[2,b]]", value);
    }


}
