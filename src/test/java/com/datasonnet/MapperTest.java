package com.datasonnet;

/*-
 * Copyright 2019-2022 the original author or authors.
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
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class MapperTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Mapper mapper = new Mapper(jsonnet);
        assertEquals(expected, mapper.transform(new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON)).getContent());
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "ds.datetime.plus(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        Map<String, Document<?>> variables = Collections.singletonMap(variable, new DefaultDocument<>(value, MediaTypes.APPLICATION_JSON));
        Mapper mapper = new Mapper(jsonnet, variables.keySet());
        assertEquals(expected, mapper.transform(new DefaultDocument<String>(json, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent());
    }

    static Stream<String[]> variableProvider() {
        return Stream.of(
                new String[] { "{ [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
                new String[] { "{ \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
        );
    }

    @Test
    void parseErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) DS.time.now() a", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 33"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void parseErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("DS.time.now() a", Collections.emptyList());
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 15"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void noTopLevelFunction() {
        try {
            Mapper mapper = new Mapper("{}", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) payload.foo", Collections.emptyList(), Collections.emptyMap(), false);
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 26"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("payload.foo", Collections.emptyList());
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 8"), "Found message: " + e.getMessage());
        }
    }

    @Disabled
    @Test
    void includedJsonnetLibraryWorks() {
        Mapper mapper = new Mapper("DS.Util.select({a: {b: 5}}, 'a.b')", Collections.emptyList());
        assertEquals("5", mapper.transform("{}"));
    }

    Map<String, Document> stringArgument(String key, String value) {
        return new HashMap<String, Document>() {{
            put(key, new DefaultDocument<String>(value, MediaTypes.TEXT_PLAIN));
        }};
    }

    @Test
    void nonJsonArguments() {
        Mapper mapper = new Mapper("argument", Arrays.asList("argument"));


        Map<String, Document<?>> map = Collections.singletonMap("argument", new DefaultDocument<>("value", MediaTypes.TEXT_PLAIN));

        Document<String> mapped = mapper.transform(new DefaultDocument<String>("{}", MediaTypes.APPLICATION_JSON), map, MediaTypes.TEXT_PLAIN);

        //assertEquals(new DefaultDocument<String>("value", MediaTypes.TEXT_PLAIN), mapped);
        assertEquals("value", mapped.getContent());
        assertEquals(MediaTypes.TEXT_PLAIN, mapped.getMediaType());

    }

    @Test
    void noTopLevelFunctionArgs() {
        try {
            Mapper mapper = new Mapper("function() { test: \'HelloWorld\' } ", Collections.emptyList(), Collections.emptyMap(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function must have at least one argument"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void testFieldsOrder() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("fieldOrder.json");
        String datasonnet = TestResourceReader.readFileAsString("fieldOrder.ds");

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("v2", new DefaultDocument<>("v2value", MediaTypes.TEXT_PLAIN));
        variables.put("v1", new DefaultDocument<>("v1value", MediaTypes.TEXT_PLAIN));

        Mapper mapper = new Mapper(datasonnet, variables.keySet());


        String mapped = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());

        datasonnet = "/** DataSonnet\n" +
                     "version=2.0\n" +
                     "preserveOrder=false\n*/\n" + datasonnet;

        mapper = new Mapper(datasonnet, variables.keySet());


        mapped = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();

        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
    }

    @Test
    void testNullAndEmpty() {
        Mapper mapper = new Mapper("{\"hello\":\"world\"}");
        Document<String> mapped = mapper.transform(new DefaultDocument<String>("", MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON);
        assertEquals("{\"hello\":\"world\"}", mapped.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, mapped.getMediaType());

        mapped = mapper.transform(new DefaultDocument<String>(" ", MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON);
        assertEquals("{\"hello\":\"world\"}", mapped.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, mapped.getMediaType());

        mapped = mapper.transform(new DefaultDocument<String>(null, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON);
        assertEquals("{\"hello\":\"world\"}", mapped.getContent());
        assertEquals(MediaTypes.APPLICATION_JSON, mapped.getMediaType());
    }
}
