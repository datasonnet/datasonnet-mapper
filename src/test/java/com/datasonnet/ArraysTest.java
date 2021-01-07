package com.datasonnet;

/*-
 * Copyright 2019-2021 the original author or authors.
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

public class ArraysTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final String lib = "ds" + ".";
    private final String pack = "arrays";

    @Test
    void testArrays_countBy() {
        Mapper mapper = new Mapper(lib + pack + ".countBy([1,2,3,4,5], function(it) it > 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }

    @Test
    void testArrays_divideBy() {
        Mapper mapper = new Mapper(lib + pack + ".divideBy([1,2,3,4,5], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,2],[3,4],[5]]", value);

        mapper = new Mapper(lib + pack + ".divideBy([1,2,3,4,5], 3)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[[1,2,3],[4,5]]", value);
    }

    @Test
    void testArrays_drop() {
        Mapper mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);

        mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 1)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[2,3,4,5]", value);

        mapper = new Mapper(lib + pack + ".drop([1,2,3,4,5], 10)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[]", value);
    }

    @Test
    void testArrays_dropWhile() {
        Mapper mapper = new Mapper(lib + pack + ".dropWhile([1,2,3,4,5], function(item) item < 3)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[3,4,5]", value);
    }

    @Test
    void testArrays_duplicates() {
        Mapper mapper = new Mapper(lib + pack + ".duplicates([1,2,3,4,5,3,2,1])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3]", value);
    }

    @Test
    void testArrays_every() {
        Mapper mapper = new Mapper(lib + pack + ".every([1,1,1], function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".every(null, function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".every([1,2,1], function(item) item == 1)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testArrays_firstWith() {
        Mapper mapper = new Mapper(lib + pack + ".firstWith([1,2,3], function(item) (item % 2) == 0)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".firstWith([1,2,3], function(item) (item % 10) == 0)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);
    }

    @Test
    void testArrays_deepFlatten() {
        Mapper mapper = new Mapper(lib + pack + ".deepFlatten([[1,2,3,[1,2]], [null,\"a\"]])\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,1,2,null,a]", value);
    }

    @Test
    void testArrays_indexOf() {
        Mapper mapper = new Mapper(lib + pack + ".indexOf([1,2,3,4,5,3], 3)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexOf([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], \"Julian\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexOf(null, 10)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".indexOf([1,2,3], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + pack + ".indexOf([1,2,3], 5)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".indexOf([1,2,3,2], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + pack + ".indexOf(\"Hello\", \"l\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexOf(\"Hello\", \"x\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);
    }

    @Test
    void testArrays_indexWhere() {
        Mapper mapper = new Mapper(lib + pack + ".indexWhere([1,2,3,4,5,3], function(item) item == 3)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".indexWhere([\"Mariano\", \"Leandro\", \"Julian\", \"Julian\"], function(item) item == \"Julian\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);
    }

    @Test
    void testArrays_join() {
        Mapper mapper = new Mapper(lib + pack + ".join([{\"id\":1,\"v\":\"a\"},{\"id\":1,\"v\":\"b\"}],[{\"id\":1,\"v\":\"c\"}], function(item) item.id,function(item) item.id)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{r:{id:1,v:c},l:{id:1,v:a}},{r:{id:1,v:c},l:{id:1,v:b}}]", value);

    }

    @Test
    void testArrays_lastIndexOf() {
        Mapper mapper = new Mapper(lib + pack + ".lastIndexOf(null, 10)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".lastIndexOf([1,2,3], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1", value);

        mapper = new Mapper(lib + pack + ".lastIndexOf([1,2,3], 5)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".lastIndexOf([1,2,3,2], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib + pack + ".lastIndexOf(\"Hello\", \"l\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib + pack + ".lastIndexOf(\"Hello\", \"x\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);
    }

    @Test
    void testArrays_leftJoin() {
        Mapper mapper = new Mapper(lib + pack + ".leftJoin([{\"id\":1,\"v\":\"a\"},{\"id\":1,\"v\":\"b\"},{\"id\":2,\"v\":\"d\"}],[{\"id\":1,\"v\":\"c\"},{\"id\":3,\"v\":\"e\"}], function(item) item.id,function(item) item.id)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{r:{id:1,v:c},l:{id:1,v:a}},{r:{id:1,v:c},l:{id:1,v:b}},{l:{id:2,v:d}}]", value);

    }

    @Test
    void testArrays_occurrences() {
        Mapper mapper = new Mapper(lib + pack + ".occurrences([1,2,3,4,3,2,1,6], function(item) item)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{1:2,2:2,3:2,4:1,6:1}", value);

    }

    @Test
    void testArrays_outerJoin() {
        Mapper mapper = new Mapper(lib + pack + ".outerJoin([{\"id\":1,\"v\":\"a\"},{\"id\":1,\"v\":\"b\"},{\"id\":2,\"v\":\"d\"}],[{\"id\":1,\"v\":\"c\"},{\"id\":3,\"v\":\"e\"}], function(item) item.id,function(item) item.id)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[{r:{id:1,v:c},l:{id:1,v:a}},{r:{id:1,v:c},l:{id:1,v:b}},{l:{id:2,v:d}},{r:{id:3,v:e}}]", value);

    }


    @Test
    void testArrays_partition() {
        Mapper mapper = new Mapper(lib + pack + ".partition([0,1,2,3,4,5], function(item) ((item % 2) ==0) )\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{success:[0,2,4],failure:[1,3,5]}", value);
    }

    @Test
    void testArrays_slice() {
        long start = System.currentTimeMillis();
        Mapper mapper = new Mapper(lib + pack + ".slice([0,1,2,3,4,5], 1, 5)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,4]", value);

        mapper = new Mapper(lib + pack + ".slice([0,1,2,3,3,3], 1, 5)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[1,2,3,3]", value);
    }

    @Test
    void testArrays_some() {
        Mapper mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) (item % 2) == 0)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) (item % 2) == 1)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) item == 3)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("true", value);

        mapper = new Mapper(lib + pack + ".some([1,2,3], function(item) item == 4)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("false", value);
    }

    @Test
    void testArrays_splitAt() {
        Mapper mapper = new Mapper(lib + pack + ".splitAt([\"A\",\"B\",\"C\"], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[C],l:[A,B]}", value);

        mapper = new Mapper(lib + pack + ".splitAt([\"A\",\"B\",\"C\"], 1)\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[B,C],l:[A]}", value);
    }

    @Test
    void testArrays_splitWhere() {
        Mapper mapper = new Mapper(lib + pack + ".splitWhere([\"A\",\"B\",\"C\",\"D\"], function(item) item==\"B\")\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[B,C,D],l:[A]}", value);

        mapper = new Mapper(lib + pack + ".splitWhere([\"A\",\"B\",\"C\",\"D\"], function(item) item==\"C\")\n", new ArrayList<>(), new HashMap<>(), true);
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("{r:[C,D],l:[A,B]}", value);
    }

    @Test
    void testArrays_sumBy() {
        Mapper mapper = new Mapper(lib + pack + ".sumBy([{a:1},{a:2},{a:3}], function(item) item.a)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("6", value);
    }

    @Test
    void testArrays_take() {
        Mapper mapper = new Mapper(lib + pack + ".take([\"A\",\"B\",\"C\"], 2)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[A,B]", value);
    }

    @Test
    void testArrays_takeWhile() {
        Mapper mapper = new Mapper(lib + pack + ".takeWhile([0,1,2,1], function(item) item <= 1)\n", new ArrayList<>(), new HashMap<>(), true);
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[0,1]", value);
    }

}