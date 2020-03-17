package com.datasonnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

public class MapperTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        assertEquals(expected, mapper.transform(json));
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "DS.ZonedDateTime.offset(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        Map<String, Document> variables = Collections.singletonMap(variable, new StringDocument(value, "application/json"));
        Mapper mapper = new Mapper(jsonnet, variables.keySet(), true);
        assertEquals(expected, mapper.transform(new StringDocument(json, "application/json"), variables).getContentsAsString());
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
            Mapper mapper = new Mapper("function(payload) DS.time.now() a", Collections.emptyList(), false);
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 33"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void parseErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("DS.time.now() a", Collections.emptyList(), true);
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 15"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void noTopLevelFunction() {
        try {
            Mapper mapper = new Mapper("{}", Collections.emptyList(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) payload.foo", Collections.emptyList(), false);
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 26"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("payload.foo", Collections.emptyList(), true);
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 8"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void includedJsonnetLibraryWorks() {
        Mapper mapper = new Mapper("DS.Util.select({a: {b: 5}}, 'a.b')", Collections.emptyList(), true);
        assertEquals("5", mapper.transform("{}"));
    }

    Map<String, Document> stringArgument(String key, String value) {
        return new HashMap<String, Document>() {{
            put(key, new StringDocument(value, "text/plain"));
        }};
    }

    @Test
    void nonJsonArguments() {
        Mapper mapper = new Mapper("argument", Arrays.asList("argument"), true);
        mapper.findAndRegisterPlugins();

        Map<String, Document> map = Collections.singletonMap("argument", new StringDocument("value", "text/plain"));

        Document mapped = mapper.transform(new StringDocument("{}", "application/json"), map, "text/plain");

        //assertEquals(new StringDocument("value", "text/plain"), mapped);
        assertEquals("value", mapped.getContentsAsString());
        assertEquals("text/plain", mapped.getMimeType());

    }

    @Test
    void noTopLevelFunctionArgs() {
        try {
            Mapper mapper = new Mapper("function() { test: \'HelloWorld\' } ", Collections.emptyList(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function must have at least one argument"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void testFieldsOrder() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("fieldOrder.json");
        String datasonnet = TestResourceReader.readFileAsString("fieldOrder.ds");

        Map<String, Document> variables = new HashMap<>();
        variables.put("v2", new StringDocument("v2value", "text/plain"));
        variables.put("v1", new StringDocument("v1value", "text/plain"));

        Mapper mapper = new Mapper(datasonnet, variables.keySet(), true);
        mapper.findAndRegisterPlugins();

        String mapped = mapper.transform(new StringDocument(jsonData, "application/json"), variables, "application/json").getContentsAsString();

        assertEquals("{\"z\":\"z\",\"a\":\"a\",\"v2\":\"v2value\",\"v1\":\"v1value\",\"y\":\"y\",\"t\":\"t\"}", mapped.trim());

        datasonnet = "/** DataSonnet\n" +
                     "version=1.0\n" +
                     "output.preserveOrder=false\n*/\n" + datasonnet;

        mapper = new Mapper(datasonnet, variables.keySet(), true);
        mapper.findAndRegisterPlugins();

        mapped = mapper.transform(new StringDocument(jsonData, "application/json"), variables, "application/json").getContentsAsString();

        assertEquals("{\"a\":\"a\",\"t\":\"t\",\"v1\":\"v1value\",\"v2\":\"v2value\",\"y\":\"y\",\"z\":\"z\"}", mapped.trim());
    }
}
