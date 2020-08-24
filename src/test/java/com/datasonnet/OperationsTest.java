package com.datasonnet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class OperationsTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String lib = "ds" +".";
    private final String pack = "ops";

    @Test
    void testOperations_combine() {
        Mapper mapper = new Mapper(lib + pack + ".combine([1],[2])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2]", value);

        mapper = new Mapper(lib + pack + ".combine({a:1},{b:2})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{a:1,b:2}", value);

        mapper = new Mapper(lib + pack + ".combine(1,2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);

        mapper = new Mapper(lib + pack + ".combine(1.2,2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1.22", value);

        mapper = new Mapper(lib + pack + ".combine(\"1\",2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);

        mapper = new Mapper(lib + pack + ".combine(\"1\",\"2\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("12", value);
    }


    @Test
    void testOperations_remove() {
        Mapper mapper = new Mapper(lib + pack + ".remove([1,2,1],1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2]", value);

        mapper = new Mapper(lib + pack + ".remove({a:1,b:2},\"a\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:2}", value);
    }

    @Test
    void testOperations_removeMatch() {
        Mapper mapper = new Mapper(lib + pack + ".removeMatch([1,2,1],[1,3])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2]", value);

        mapper = new Mapper(lib + pack + ".removeMatch({a:1,b:2},{a:1,c:3})\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{b:2}", value);
    }

    @Test
    void testOperations_append() {
        Mapper mapper = new Mapper(lib + pack + ".append([1,2,3],4)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,4]", value);
    }

    @Test
    void testOperations_prepend() {
        Mapper mapper = new Mapper(lib + pack + ".prepend([1,2,3],4)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[4,1,2,3]", value);
    }

}
