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

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumbersTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" + ".";
    private final String pack = "numbers";

    @Test
    void testNumbers_fromBinary() {
        Mapper mapper = new Mapper(lib + pack + ".fromBinary(\"-10\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-2", value);

        mapper = new Mapper(lib + pack + ".fromBinary(\"1111111111111111111111111111111\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2147483647", value);

        mapper = new Mapper(lib + pack + ".fromBinary(11)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib + pack + ".fromBinary(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".fromBinary(\"100\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4", value);

        mapper = new Mapper(lib + pack + ".fromBinary(\"1111111111111111111111111111111111111111111111111111111111111\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2305843009213693952", value);


    }

    @Test
    void testNumbers_fromHex() {
        Mapper mapper = new Mapper(lib + pack + ".fromHex(\"-1\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"3e3aeb\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4078315", value);

        mapper = new Mapper(lib + pack + ".fromHex(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".fromHex(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"f\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);


        mapper = new Mapper(lib + pack + ".fromHex(\"FFFFFFFFF\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68719476735", value);

    }

    @Test
    void testNumbers_fromRadixNumber() {
        Mapper mapper = new Mapper(lib + pack + ".fromRadixNumber(10, 2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".fromRadixNumber(\"ff\", 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("255", value);


        mapper = new Mapper(lib + pack + ".fromRadixNumber(\"FFFFFFFFF\", 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68719476735", value);
    }

    @Test
    void testNumbers_toBinary() {
        Mapper mapper = new Mapper(lib + pack + ".toBinary(-2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-10", value);

        mapper = new Mapper(lib + pack + ".toBinary(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".toBinary(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".toBinary(2)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);


        mapper = new Mapper(lib + pack + ".toBinary(5294967295)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("100111011100110101100100111111111", value);

    }

    @Test
    void testNumbers_toHex() {
        Mapper mapper = new Mapper(lib + pack + ".toHex(-1)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".toHex(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".toHex(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".toHex(15)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("f", value);


        mapper = new Mapper(lib + pack + ".toHex(68719476735)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("FFFFFFFFF".toLowerCase(), value);

    }

    @Test
    void testNumbers_toRadixNumber() {
        Mapper mapper = new Mapper(lib + pack + ".toRadixNumber(2, 2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        mapper = new Mapper(lib + pack + ".toRadixNumber(255, 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("ff", value);


        mapper = new Mapper(lib + pack + ".toRadixNumber(68719476735, 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("fffffffff", value);

    }


}