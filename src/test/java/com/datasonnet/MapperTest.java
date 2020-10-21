
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
//package com.datasonnet;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertTrue;
//import static org.junit.jupiter.api.Assertions.fail;
//
//import com.datasonnet.document.DefaultDocument;
//import com.datasonnet.document.Document;
//import com.datasonnet.util.TestResourceReader;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.MethodSource;
//
//import javax.ws.rs.core.MediaType;
//import java.util.*;
//import java.util.stream.Stream;
//
//public class MapperTest {
//
//    @ParameterizedTest
//    @MethodSource("simpleProvider")
//    void simple(String jsonnet, String json, String expected) {
//        Mapper mapper = new Mapper(jsonnet);
//        assertEquals(expected, mapper.transform(json));
//    }
//
//    static Stream<String[]> simpleProvider() {
//        return Stream.of(
//                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
//                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
//                new String[] { "DS.ZonedDateTime.offset(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
//                );
//    }
//
//    @ParameterizedTest
//    @MethodSource("variableProvider")
//    void variables(String jsonnet, String json, String variable, String value, String expected) {
//        Map<String, Document<String>> variables = Collections.singletonMap(variable, new DefaultDocument<String>(value, MediaType.APPLICATION_JSON_TYPE));
//        Mapper mapper = new Mapper(jsonnet, variables.keySet(), true);
//        assertEquals(expected, mapper.transform(new DefaultDocument<String>(json, MediaType.APPLICATION_JSON_TYPE), variables).getContent());
//    }
//
//    static Stream<String[]> variableProvider() {
//        return Stream.of(
//                new String[] { "{ [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
//                new String[] { "{ \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
//        );
//    }
//
//    @Test
//    void parseErrorLineNumber() {
//        try {
//            Mapper mapper = new Mapper("function(payload) DS.time.now() a", Collections.emptyList(), false);
//            fail("Must fail to parse");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 33"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void parseErrorLineNumberWhenWrapped() {
//        try {
//            Mapper mapper = new Mapper("DS.time.now() a", Collections.emptyList(), true);
//            fail("Must fail to parse");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 15"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void noTopLevelFunction() {
//        try {
//            Mapper mapper = new Mapper("{}", Collections.emptyList(), false);
//            fail("Must fail to execute");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("Top Level Function"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void executeErrorLineNumber() {
//        try {
//            Mapper mapper = new Mapper("function(payload) payload.foo", Collections.emptyList(), false);
//            mapper.transform("{}");
//            fail("Must fail to execute");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("at line 1 column 26"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void executeErrorLineNumberWhenWrapped() {
//        try {
//            Mapper mapper = new Mapper("payload.foo", Collections.emptyList(), true);
//            mapper.transform("{}");
//            fail("Must fail to execute");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("at line 1 column 8"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void includedJsonnetLibraryWorks() {
//        Mapper mapper = new Mapper("DS.Util.select({a: {b: 5}}, 'a.b')", Collections.emptyList(), true);
//        assertEquals("5", mapper.transform("{}"));
//    }
//
//    Map<String, Document> stringArgument(String key, String value) {
//        return new HashMap<String, Document>() {{
//            put(key, new DefaultDocument<String>(value, MediaType.TEXT_PLAIN_TYPE));
//        }};
//    }
//
//    @Test
//    void nonJsonArguments() {
//        Mapper mapper = new Mapper("argument", Arrays.asList("argument"), true);
//
//
//        Map<String, Document> map = Collections.singletonMap("argument", new DefaultDocument<String>("value", MediaType.TEXT_PLAIN_TYPE));
//
//        Document mapped = mapper.transform(new DefaultDocument<String>("{}", MediaType.APPLICATION_JSON_TYPE), map, MediaType.TEXT_PLAIN_TYPE);
//
//        //assertEquals(new DefaultDocument<String>("value", MediaType.TEXT_PLAIN_TYPE), mapped);
//        assertEquals("value", mapped.getContent());
//        assertEquals(MediaType.TEXT_PLAIN_TYPE, mapped.getMediaType());
//
//    }
//
//    @Test
//    void noTopLevelFunctionArgs() {
//        try {
//            Mapper mapper = new Mapper("function() { test: \'HelloWorld\' } ", Collections.emptyList(), false);
//            fail("Must fail to execute");
//        } catch(IllegalArgumentException e) {
//            assertTrue(e.getMessage().contains("Top Level Function must have at least one argument"), "Found message: " + e.getMessage());
//        }
//    }
//
//    @Test
//    void testFieldsOrder() throws Exception {
//        String jsonData = TestResourceReader.readFileAsString("fieldOrder.json");
//        String datasonnet = TestResourceReader.readFileAsString("fieldOrder.ds");
//
//        Map<String, Document> variables = new HashMap<>();
//        variables.put("v2", new DefaultDocument<String>("v2value", MediaType.TEXT_PLAIN_TYPE));
//        variables.put("v1", new DefaultDocument<String>("v1value", MediaType.TEXT_PLAIN_TYPE));
//
//        Mapper mapper = new Mapper(datasonnet, variables.keySet(), true);
//
//
//        String mapped = mapper.transform(new DefaultDocument<String>(jsonData, MediaType.APPLICATION_JSON_TYPE), variables, MediaType.APPLICATION_JSON_TYPE).getContent();
//
//        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());
//
//        datasonnet = "/** DataSonnet\n" +
//                     "version=2.0\n" +
//                     "output.preserveOrder=false\n*/\n" + datasonnet;
//
//        mapper = new Mapper(datasonnet, variables.keySet(), true);
//
//
//        mapped = mapper.transform(new DefaultDocument<String>(jsonData, MediaType.APPLICATION_JSON_TYPE), variables, MediaType.APPLICATION_JSON_TYPE).getContent();
//
//        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
//    }
//}
