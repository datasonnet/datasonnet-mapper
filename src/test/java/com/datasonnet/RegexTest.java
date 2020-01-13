package com.datasonnet;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegexTest {

    @ParameterizedTest
    @MethodSource("jsonAssertProvider")
    void testRegexJSON(String jsonnet, String expected) throws JSONException {
        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), Collections.emptyMap(), true);
        String result = mapper.transform("{}");
        JSONAssert.assertEquals(expected, result, true);
    }

    @ParameterizedTest
    @MethodSource("assertEqualsProvider")
    void testRegexStr(String jsonnet, String expected) throws JSONException {
        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), Collections.emptyMap(), true);
        String result = mapper.transform("{}");
        assertEquals(expected, result);
    }

    static Stream<String[]> jsonAssertProvider() {
        return Stream.of(
                new String[] { "DS.Regex.regexFullMatch(@'h.*o', 'hello')", "{\"string\":\"hello\",\"captures\":[],\"namedCaptures\":{}\n}"},
                new String[] { "DS.Regex.regexFullMatch(@'h(.*)o', 'hello')", "{\"captures\":[\"ell\"],\"namedCaptures\":{},\"string\":\"hello\"}"},
                new String[] { "DS.Regex.regexFullMatch(@'h(?P<mid>.*)o', 'hello')", "{\"captures\":[\"ell\"],\"namedCaptures\":{\"mid\":\"ell\"},\"string\":\"hello\"}"},
                new String[] { "DS.Regex.regexPartialMatch(@'e', 'hello')", "{\"string\":\"hello\",\"captures\":[],\"namedCaptures\":{}\n}"},
                new String[] { "DS.Regex.regexPartialMatch(@'e(.*)o', 'hello')", "{\"captures\":[\"ll\"],\"namedCaptures\":{},\"string\":\"hello\"}"},
                new String[] { "DS.Regex.regexPartialMatch(@'e(?P<mid>.*)o', 'hello')", "{\"captures\":[\"ll\"],\"namedCaptures\":{\"mid\":\"ll\"},\"string\":\"hello\"}"}
        );
    }

    static Stream<String[]> assertEqualsProvider() {
        return Stream.of(
                new String[] { "DS.Regex.regexFullMatch(@'world', 'hello')", "null"},
                new String[] { "DS.Regex.regexPartialMatch(@'world', 'hello')", "null"},
                new String[] { "DS.Regex.regexQuoteMeta(@'1.5-2.0?')", "\"1\\\\.5-2\\\\.0\\\\?\""},
                new String[] { "DS.Regex.regexReplace('wishyfishyisishy', @'ish', 'and')", "\"wandyfishyisishy\""},
                new String[] { "DS.Regex.regexReplace('yabba dabba doo', @'b+', 'd')", "\"yada dabba doo\""},
                new String[] { "DS.Regex.regexGlobalReplace('wishyfishyisishy', @'ish', 'and')", "\"wandyfandyisandy\""},
                new String[] { "DS.Regex.regexGlobalReplace('yabba dabba doo', @'b+', 'd')", "\"yada dada doo\""}
        );
    }
}
