package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DWArraysTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Arrays";

    @Test
    void testDWArrays_countBy() {
        Mapper mapper = new Mapper(lib + pack + ".countBy([1,2,3,4,5], function(it) it > 2)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }

    @Test
    void testDWArrays_indexOf() {
        Mapper mapper = new Mapper(lib + pack + ".indexOf([1,2,3,4,5,3], 3)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexOf([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], \"Julian\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testDWArrays_indexWhere() {
        Mapper mapper = new Mapper(lib + pack + ".indexWhere([1,2,3,4,5,3], function(item) item == 3)\n", new ArrayList<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexWhere([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], function(item) item == \"Julian\")\n", new ArrayList<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }
}
