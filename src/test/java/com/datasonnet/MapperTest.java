package com.datasonnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.*;
import java.util.stream.Stream;

public class MapperTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Mapper mapper = new Mapper(jsonnet, new ArrayList<>(), true);
        assertEquals(expected, mapper.transform(json));
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "{ \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "PortX.ZonedDateTime.offset(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", "{}", "\"2020-07-23T21:00:00Z\""}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        HashMap<String, Document> variables = new HashMap<>();
        variables.put(variable, new StringDocument(value, "application/json"));
        Mapper mapper = new Mapper(jsonnet, variables.keySet(), true);
        assertEquals(expected, mapper.transform(new StringDocument(json, "application/json"), variables).contents());
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
            Mapper mapper = new Mapper("function(payload) portx.time.now() a", new ArrayList<>(), false);
            fail("Must fail to parse");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Expected end-of-input at line 1 column 36"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void noTopLevelFunction() {
        try {
            Mapper mapper = new Mapper("{}", new ArrayList<>(), false);
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Top Level Function"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("function(payload) payload.foo", new ArrayList<>(), false);
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 26"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void executeErrorLineNumberWhenWrapped() {
        try {
            Mapper mapper = new Mapper("payload.foo", new ArrayList<>(), true);
            mapper.transform("{}");
            fail("Must fail to execute");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("at line 1 column 8"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void includedJsonnetLibraryWorks() {
        Mapper mapper = new Mapper("PortX.Util.select({a: {b: 5}}, 'a.b')", new ArrayList<>(), true);
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
        Document mapped = mapper.transform(new StringDocument("{}", "application/json"), stringArgument("argument", "value"), "text/plain");
        assertEquals(new StringDocument("value", "text/plain"), mapped);
    }
}
