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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilLibraryTest {

    @Test
    void testDuplicates() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibDuplicatesTest.json");

        Mapper mapper = new Mapper("ds.util.duplicates(payload.primitive)");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[\"hello\",\"world\"]");

        mapper = new Mapper("ds.util.duplicates(payload.complex, function(x) x.language.name)");
        mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[{\"language\":{\"name\":\"Java8\",\"version\":\"1.8.0\"}}]");

        mapper = new Mapper("ds.util.duplicates(payload.moreComplex, function(x) std.substr(x.language.version, 0, 3))");
        mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[{\"language\":{\"name\":\"Java1.8\",\"version\":\"1.8_152\"}}]");
    }

    @Test
    void testRemoveFields() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibRemoveFieldsTest.json");
        testDS("utilLibRemoveFieldsTest.ds", jsonData);
    }

    @Test
    void testReverse() throws Exception {
        String jsonData = "[\"a\",\"b\",\"c\",\"d\"]";
        Mapper mapper = new Mapper("ds.reverse(payload)");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "[\"d\",\"c\",\"b\",\"a\"]");
    }

    @Test
    void testGroupBy() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibGroupByTest.json");
        testDS("utilLibGroupByTest.ds", jsonData);
    }

    @Test
    void testRound() throws Exception {
        testDS("utilLibRoundTest.ds", "{}");
    }

    @Test
    void testCounts() throws Exception {
        testDS("utilLibCountsTest.ds", "{}");
    }

    @Test
    void testMapToObject() throws Exception {
        testDS("utilLibMapToObjectTest.ds", "{}");
    }

    private void testDS(String dsFileName, String input) throws Exception {
        String ds = TestResourceReader.readFileAsString(dsFileName);

        Mapper mapper = new Mapper(ds);
        String mappedJson = mapper.transform(new DefaultDocument<String>(input, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "true");
    }
}
