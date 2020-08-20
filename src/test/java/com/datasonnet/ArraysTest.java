package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ArraysTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String lib = "ds" +".";
    private final String pack = "arrays";

    @Test
    void testArrays_countBy() {
        Mapper mapper = new Mapper(lib + pack + ".countBy([1,2,3,4,5], function(it) it > 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }

    @Test
    void testArrays_divideBy() {
        Mapper mapper = new Mapper(lib + pack + ".divideBy([1,2,3,4,5], 2)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,2],[3,4],[5]]", value);

        mapper = new Mapper(lib + pack + ".divideBy([1,2,3,4,5], 3)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,2,3],[4,5]]", value);
    }

    @Test
    void testArrays_drop() {
        Mapper mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 2)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

        mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 1)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2,3,4,5]", value);

        mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 10)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[]", value);
    }

    @Test
    void testArrays_dropWhile() {
        Mapper mapper = new Mapper(lib + pack + ".dropWhile([1,2,3,4,5], function(item) item < 3)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);
    }

    @Test
    void testArrays_every() {
        Mapper mapper = new Mapper(lib + pack + ".every([1,1,1], function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".every(null, function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".every([1,2,1], function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testArrays_firstWith() {
        Mapper mapper = new Mapper(lib + pack + ".firstWith([1,2,3], function(item) (item % 2) == 0)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".firstWith([1,2,3], function(item) (item % 10) == 0)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testArrays_indexOf() {
        Mapper mapper = new Mapper(lib + pack + ".indexOf([1,2,3,4,5,3], 3)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexOf([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], \"Julian\")\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testArrays_indexWhere() {
        Mapper mapper = new Mapper(lib + pack + ".indexWhere([1,2,3,4,5,3], function(item) item == 3)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexWhere([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], function(item) item == \"Julian\")\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }


    @Test
    void testArrays_partition() {
        Mapper mapper = new Mapper(lib + pack + ".partition([0,1,2,3,4,5], function(item) ((item % 2) ==0) )\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{success:[0,2,4],failure:[1,3,5]}", value);
    }

    @Test
    void testArrays_slice() {
        long start = System.currentTimeMillis();
        Mapper mapper = new Mapper(lib + pack + ".slice([0,1,2,3,4,5], 1, 5)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,4]", value);

        mapper = new Mapper(lib + pack + ".slice([0,1,2,3,3,3], 1, 5)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,3]", value);

        System.out.println("Elapsed Time: " + (System.currentTimeMillis() - start));
    }

    @Test
    void testArrays_some() {
        Mapper mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) (item % 2) == 0)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) (item % 2) == 1)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) item == 3)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) item == 4)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testArrays_splitAt() {
        Mapper mapper = new Mapper(lib + pack + ".splitAt([\"A\",\"B\",\"C\"], 2)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[C],l:[A,B]}", value);

        mapper = new Mapper(lib + pack + ".splitAt([\"A\",\"B\",\"C\"], 1)\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[B,C],l:[A]}", value);
    }

    @Test
    void testArrays_splitWhere() {
        Mapper mapper = new Mapper(lib + pack + ".splitWhere([\"A\",\"B\",\"C\",\"D\"], function(item) item==\"B\")\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[B,C,D],l:[A]}", value);

        mapper = new Mapper(lib + pack + ".splitWhere([\"A\",\"B\",\"C\",\"D\"], function(item) item==\"C\")\n", new ArrayList<>(), new HashMap<>(),true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[C,D],l:[A,B]}", value);
    }

    @Test
    void testArrays_sumBy() {
        Mapper mapper = new Mapper(lib + pack + ".sumBy([{a:1},{a:2},{a:3}], function(item) item.a)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("6", value);
    }

    @Test
    void testArrays_take() {
        Mapper mapper = new Mapper(lib + pack + ".take([\"A\",\"B\",\"C\"], 2)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[A,B]", value);
    }

    @Test
    void testArrays_takeWhile() {
        Mapper mapper = new Mapper(lib + pack + ".takeWhile([0,1,2,1], function(item) item <= 1)\n", new ArrayList<>(), new HashMap<>(),true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1]", value);
    }

}