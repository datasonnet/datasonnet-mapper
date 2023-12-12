package com.datasonnet;

/*-
 * Copyright 2019-2023 the original author or authors.
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
import com.datasonnet.library.TestLib;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

//This is the test suite for the documented programmatic usages of DataSonnet, we need to make sure that all examples in the doc are working
//https://datasonnet.github.io/datasonnet-mapper/datasonnet/latest/jar-lib.html
public class ProgrammaticUsagesTest {

    @Test
    void testJsonExample() throws JSONException {
        String json = """
            {
              "userId" : "123",
              "name" : "DataSonnet"
            }
            """;
        String script = """
            {
               "uid": payload.userId,
               "uname": payload.name,
             }
            """;
        String expected = """
            {
               "uid": "123",
               "uname": "DataSonnet"
             }
            """;

        Mapper mapper = new Mapper(script);
        String result = mapper.transform(new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON)).getContent();
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    void testSimple() throws JSONException {
        String payload = "HelloWorld";
        String script = """
            {
               "greetings": payload
            }
            """;
        String expected = """
            {
               "greetings": "HelloWorld"
             }
            """;

        Mapper mapper = new Mapper(script);
        String result = mapper.transform(payload);
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    void testWithVariables() throws JSONException {
        String payload = "{ \"greetings\": \"HelloWorld\"}";

        String json = """
            {
              "userId" : "123",
              "name" : "DataSonnet"
            }
            """;

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("userData", new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON));

        String script = """
            {
               "greetings": payload.greetings,
               "uid": userData.userId,
               "uname": userData.name,
             }
            """;
        String expected = """
            {
               "greetings": "HelloWorld", 
               "uid": "123",
               "uname": "DataSonnet"
             }
            """;

        Mapper mapper = new Mapper(script, variables.keySet());
        String result = mapper.transform(new DefaultDocument<String>(payload, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();
        JSONAssert.assertEquals(expected, result, true);
    }

    @Test
    void testMapperBuilder() throws JSONException, URISyntaxException, IOException {
        String payload = "{ \"greetings\": \"HelloWorld\"}";

        String json = """
            {
              "userId" : "123",
              "name" : "DataSonnet"
            }
            """;

        Map<String, Document<?>> variables = new HashMap<>();
        variables.put("userData", new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON));

        String script = """
            local importedLib = import 'importTest.ds';
            
            {
               "greetings": payload.greetings,
               "sayHello": testlib.sayHello(),
               "uid": userData.userId,
               "uname": userData.name,
               "foo": importedLib.caps('foo')
             }
            """;
        String expected = """
            {
               "greetings": "HelloWorld", 
               "sayHello": "Hello, World",
               "uid": "123",
               "uname": "DataSonnet",
               "foo": "FOO"
             }
            """;

        final String dsImport = TestResourceReader.readFileAsString("importTest.ds");

        String result = new MapperBuilder(script)
                .withImports(Collections.singletonMap("importTest.ds", dsImport))
                .withLibrary(TestLib.getInstance())
                .withInputNames(variables.keySet())
                .build()
                .transform(new DefaultDocument<String>(payload, MediaTypes.APPLICATION_JSON), variables, MediaTypes.APPLICATION_JSON).getContent();
        JSONAssert.assertEquals(expected, result, true);
    }

}
