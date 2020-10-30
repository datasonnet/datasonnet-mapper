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

import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegexTest {

    @ParameterizedTest
    @MethodSource("jsonAssertProvider")
    void testRegexJSON(String jsonnet, String expected) throws JSONException {
        Mapper mapper = new Mapper(jsonnet);
        String result = mapper.transform("{}");
        JSONAssert.assertEquals(expected, result, true);
    }

    @ParameterizedTest
    @MethodSource("assertEqualsProvider")
    void testRegexStr(String jsonnet, String expected) throws JSONException {
        Mapper mapper = new Mapper(jsonnet);
        String result = mapper.transform("{}");
        assertEquals(expected, result);
    }

    static Stream<String[]> jsonAssertProvider() {
        return Stream.of(
                new String[]{"ds.regex.regexFullMatch(@'h.*o', 'hello')", "{\"string\":\"hello\",\"captures\":[],\"namedCaptures\":{}\n}"},
                new String[]{"ds.regex.regexFullMatch(@'h(.*)o', 'hello')", "{\"captures\":[\"ell\"],\"namedCaptures\":{},\"string\":\"hello\"}"},
                new String[]{"ds.regex.regexFullMatch(@'h(?P<mid>.*)o', 'hello')", "{\"captures\":[\"ell\"],\"namedCaptures\":{\"mid\":\"ell\"},\"string\":\"hello\"}"},
                new String[]{"ds.regex.regexPartialMatch(@'e', 'hello')", "{\"string\":\"e\",\"captures\":[],\"namedCaptures\":{}\n}"},
                new String[]{"ds.regex.regexPartialMatch(@'e(.*)o', 'hello')", "{\"captures\":[\"ll\"],\"namedCaptures\":{},\"string\":\"ello\"}"},
                new String[]{"ds.regex.regexPartialMatch(@'e(?P<mid>.*)o', 'hello')", "{\"captures\":[\"ll\"],\"namedCaptures\":{\"mid\":\"ll\"},\"string\":\"ello\"}"},
                new String[]{"ds.regex.regexScan(@'(?P<user>[a-z]*)@(?P<domain>[a-z]*).org', 'modus@datasonnet.org,box@datasonnet.org')",
                        "[{\"captures\":[\"modus\",\"datasonnet\"],\"namedCaptures\":{\"domain\":\"datasonnet\",\"user\":\"modus\"},\"string\":\"modus@datasonnet.org\"},{\"captures\":[\"box\",\"datasonnet\"],\"namedCaptures\":{\"domain\":\"datasonnet\",\"user\":\"box\"},\"string\":\"box@datasonnet.org\"}]"}
        );
    }

    static Stream<String[]> assertEqualsProvider() {
        return Stream.of(
                new String[]{"ds.regex.regexFullMatch(@'world', 'hello')", "null"},
                new String[]{"ds.regex.regexPartialMatch(@'world', 'hello')", "null"},
                new String[]{"ds.regex.regexQuoteMeta(@'1.5-2.0?')", "\"1\\\\.5-2\\\\.0\\\\?\""},
                new String[]{"ds.regex.regexReplace('wishyfishyisishy', @'ish', 'and')", "\"wandyfishyisishy\""},
                new String[]{"ds.regex.regexReplace('yabba dabba doo', @'b+', 'd')", "\"yada dabba doo\""},
                new String[]{"ds.regex.regexGlobalReplace('wishyfishyisishy', @'ish', 'and')", "\"wandyfandyisandy\""},
                new String[]{"ds.regex.regexGlobalReplace('yabba dabba doo', @'b+', 'd')", "\"yada dada doo\""}
        );
    }

    @Test
    void testRegexGlobalReplaceWithFunction() throws Exception {
        String jsonnet = TestResourceReader.readFileAsString("regexGlobalReplaceWithFunction.ds");
        Mapper mapper = new Mapper(jsonnet);
        String result = mapper.transform("{}");
        assertEquals("\"xxx4yyy16zzz36aaa\"", result);
    }

}
