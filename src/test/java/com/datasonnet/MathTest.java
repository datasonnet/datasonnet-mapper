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

public class MathTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" + ".";
    private final String pack = "math";

    @Test
    void testMath_mantissa() {
        Mapper mapper = new Mapper(lib + pack + ".mantissa(2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0.5", value);
    }

    @Test
    void testMath_exponent() {
        Mapper mapper = new Mapper(lib + pack + ".exponent(5)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }
}
