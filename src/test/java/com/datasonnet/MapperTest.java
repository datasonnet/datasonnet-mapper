package com.datasonnet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.datasonnet.wrap.Mapper;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.stream.Stream;

public class MapperTest {

    @ParameterizedTest
    @MethodSource("simpleProvider")
    void simple(String jsonnet, String json, String expected) {
        Mapper mapper = new Mapper(jsonnet, new HashMap<String, String>());
        assertEquals(expected, mapper.transform(json));
    }

    static Stream<String[]> simpleProvider() {
        return Stream.of(
                new String[] { "function(payload) { \"uid\": payload.user_id }", "{ \"user_id\": 7 }", "{\"uid\":7}"},
                new String[] { "function(payload) { \"uid\": payload.user_id }", "{ \"user_id\": 8 }", "{\"uid\":8}"},
                new String[] { "function(payload) { \"uid\": portx.timesfive(payload.user_id) }", "{ \"user_id\": 8 }", "{\"uid\":40}"}
                );
    }

    @ParameterizedTest
    @MethodSource("variableProvider")
    void variables(String jsonnet, String json, String variable, String value, String expected) {
        HashMap<String, String> variables = new HashMap<String, String>();
        variables.put(variable, value);
        Mapper mapper = new Mapper(jsonnet, variables);
        assertEquals(expected, mapper.transform(json));
    }

    static Stream<String[]> variableProvider() {
        return Stream.of(
                new String[] { "function(payload, name) { [name]: payload.user_id }", "{ \"user_id\": 7 }", "name", "\"variable\"", "{\"variable\":7}"},
                new String[] { "function(payload, offset) { \"uid\": payload.user_id + offset }", "{ \"user_id\": 8 }", "offset", "3", "{\"uid\":11}"}
        );
    }

}
