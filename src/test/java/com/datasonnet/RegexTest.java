package com.datasonnet;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegexTest {

    @Test
    void testRegexFullMatch() throws JSONException {
        Mapper mapper = new Mapper("DS.Regex.regexFullMatch(@'world', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        String result = mapper.transform("{}");
        assertEquals("null", result);

        mapper = new Mapper("DS.Regex.regexFullMatch(@'h.*o', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"string\":\"hello\",\"captures\":[],\"namedCaptures\":{}\n}", result, true);

        mapper = new Mapper("DS.Regex.regexFullMatch(@'h(.*)o', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"captures\":[\"ell\"],\"namedCaptures\":{},\"string\":\"hello\"}", result, true);

        mapper = new Mapper("DS.Regex.regexFullMatch(@'h(?P<mid>.*)o', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"captures\":[\"ell\"],\"namedCaptures\":{\"mid\":\"ell\"},\"string\":\"hello\"}", result, true);
    }

    @Test
    void testRegexPartialMatch() throws JSONException {
        Mapper mapper = new Mapper("DS.Regex.regexPartialMatch(@'world', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        String result = mapper.transform("{}");
        assertEquals("null", result);

        mapper = new Mapper("DS.Regex.regexPartialMatch(@'e', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"string\":\"hello\",\"captures\":[],\"namedCaptures\":{}\n}", result, true);

        mapper = new Mapper("DS.Regex.regexPartialMatch(@'e(.*)o', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"captures\":[\"ll\"],\"namedCaptures\":{},\"string\":\"hello\"}", result, true);

        mapper = new Mapper("DS.Regex.regexPartialMatch(@'e(?P<mid>.*)o', 'hello')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        JSONAssert.assertEquals("{\"captures\":[\"ll\"],\"namedCaptures\":{\"mid\":\"ll\"},\"string\":\"hello\"}", result, true);
    }

    @Test
    void testRegexQuoteMeta() {
        Mapper mapper = new Mapper("DS.Regex.regexQuoteMeta(@'1.5-2.0?')", Collections.emptyList(), Collections.emptyMap(),true);
        String result = mapper.transform("{}");
        assertEquals("\"1\\\\.5-2\\\\.0\\\\?\"", result);
    }

    @Test
    void testRegexReplace() {
        Mapper mapper = new Mapper("DS.Regex.regexReplace('wishyfishyisishy', @'ish', 'and')", Collections.emptyList(), Collections.emptyMap(),true);
        String result = mapper.transform("{}");
        assertEquals("\"wandyfishyisishy\"", result);

        mapper = new Mapper("DS.Regex.regexReplace('yabba dabba doo', @'b+', 'd')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        assertEquals("\"yada dabba doo\"", result);
    }

    @Test
    void testRegexGlobalReplace() {
        Mapper mapper = new Mapper("DS.Regex.regexGlobalReplace('wishyfishyisishy', @'ish', 'and')", Collections.emptyList(), Collections.emptyMap(),true);
        String result = mapper.transform("{}");
        assertEquals("\"wandyfandyisandy\"", result);

        mapper = new Mapper("DS.Regex.regexGlobalReplace('yabba dabba doo', @'b+', 'd')", Collections.emptyList(), Collections.emptyMap(),true);
        result = mapper.transform("{}");
        assertEquals("\"yada dada doo\"", result);
    }
}
