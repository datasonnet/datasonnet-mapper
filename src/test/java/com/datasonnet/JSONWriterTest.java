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
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JSONWriterTest {

    @Test
    public void testJSONWriter() {

        Mapper mapper = new Mapper("{ \n" +
                " str: 'value', \n" +
                " arr: [1, 2, 3], \n" +
                " obj: {}, \n" +
                " num: 9, \n" +
                " 'null': null \n" +
                "}");

        Document<String> mapped = mapper.transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);
        assertEquals(MediaTypes.APPLICATION_JSON, mapped.getMediaType());

        String expected = "{\"str\":\"value\",\"arr\":[1,2,3],\"obj\":{},\"num\":9,\"null\":null}";
        assertEquals(expected.trim(), mapped.getContent().trim());
    }

    @Test
    public void testNull() {
        Mapper mapper = new Mapper("null");
        Document<String> mapped = mapper.transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertEquals("null", mapped.getContent().trim());
    }
}
