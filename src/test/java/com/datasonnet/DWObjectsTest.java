package com.datasonnet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DWObjectsTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Objects";

    @Test
    void testDWObjects_divideBy(){
        String input="{\"a\": 1, " +
                       "\"b\" : true, " +
                       "\"c\" : 2, " +
                       "\"d\" : false, " +
                       "\"e\" : 3}";
        String compare="[{a:1,b:true},{c:2,d:false},{e:3}]";
        Mapper mapper = new Mapper(lib+pack+".divideBy(" + input + ", 2)", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);

        compare="[{a:1,b:true,c:2},{d:false,e:3}]";
        mapper = new Mapper(lib+pack+".divideBy(" + input + ", 3)", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);
    }

    @Test
    void testDWObjects_entrySet(){
        String input="{\"test1\":\"x\",\"test2\":{\"inTest3\":\"x\",\"inTest4\":{}},\"test10\":[{},{}]}";
        String compare="[{value:x,key:test1},{value:{inTest3:x,inTest4:{}},key:test2},{value:[{},{}],key:test10}]";
        Mapper mapper = new Mapper(lib+pack+".entrySet(" + input + ")", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals(compare, value);
    }

    @Test
    void testDWObjects_everyEntry(){
        Mapper mapper = new Mapper(lib+pack+".everyEntry({\"a\":\"\",\"b\":\"123\"}, function(value) std.isString(value))", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".everyEntry({\"a\":\"\",\"b\":\"123\"}, function(value,key) key ==\"a\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);

        mapper = new Mapper(lib+pack+".everyEntry({\"b\":\"\",\"b\":\"123\"}, function(value,key) key ==\"b\")", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".everyEntry(null, function(value) std.isString(value))", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib+pack+".everyEntry(null, function(value) std.isString(value))", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);
    }

    @Test
    void testDWObjects_keySet(){
        Mapper mapper = new Mapper(lib+pack+".keySet({ \"a\" : true, \"b\" : 1})\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[a,b]", value);
    }

    @Test
    void testDWObjects_mergeWith(){
        String obj1, obj2;
        obj1 = "{\"a\": true, \"b\": 1}";
        obj2 = "{\"a\": false, \"c\": \"Test\"}";
        Mapper mapper = new Mapper(lib+pack+".mergeWith(" + obj1 + ", " + obj2 + ")\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:false,b:1,c:Test}", value);

        mapper = new Mapper(lib+pack+".mergeWith(" + obj1 + ", null)\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:true,b:1}", value);

        mapper = new Mapper(lib+pack+".mergeWith(null, " + obj2 + ")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:false,c:Test}", value);
    }
}
