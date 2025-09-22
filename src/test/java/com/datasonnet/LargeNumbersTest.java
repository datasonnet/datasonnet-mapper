package com.datasonnet;

/*-
 * Copyright 2019-2025 the original author or authors.
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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class LargeNumbersTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    void testLargeNumberHandling() {
        Mapper mapper = new Mapper("payload");
        Document<Long> mapped = mapper.transform(new DefaultDocument<String>("102506060000000002", MediaTypes.APPLICATION_JSON), new HashMap(), MediaTypes.APPLICATION_JAVA, java.lang.Long.class);
        assertEquals(java.lang.Long.class, mapped.getContent().getClass());
        assertEquals("102506060000000002", mapped.getContent().toString());
    }

    @Test
    void testLargeNumbersInStrings() {
        Mapper mapper = new Mapper("payload");
        Document<Map> mapped = mapper.transform(new DefaultDocument<String>("{\n" +
                "  \"longNumberInString\": \"String-12345678901234567890-String\"," +
                "  \"number\": 12345678901234567890,\n" +
                "  \"array\": [ 12345678901234567890, 1234546756754667890, 123 ]\n" +
                "}", MediaTypes.APPLICATION_JSON), new HashMap(), MediaTypes.APPLICATION_JAVA, java.util.HashMap.class);
        Map objValues = mapped.getContent();
        assertEquals("String-12345678901234567890-String", objValues.get("longNumberInString").toString());
        assertEquals(java.math.BigInteger.class, objValues.get("number").getClass());
        assertEquals("12345678901234567890", objValues.get("number").toString());
        assertEquals(java.math.BigInteger.class, ((List)objValues.get("array")).get(0).getClass());
        assertEquals("12345678901234567890", ((List)objValues.get("array")).get(0).toString());
    }
}