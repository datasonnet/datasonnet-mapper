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
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ImportTest {
    @Test
    void simpleImport() {
        Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(), Collections.singletonMap("output.json", "{\"a\": 5}"));
        String result = mapper.transform("{}");
        assertEquals("{\"a\":5}", result);
    }

    @Test
    void importParseErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a b"));
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("end-of-input at line 1 column 3"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void importExecuteErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a.b"));
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("output.json line 1 column 1"), "Found message: " + e.getMessage());
            assertTrue(e.getMessage().contains("line 1 column 1 of the transformation"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void importLibsonnet() throws Exception {
        try {
            final String lib = TestResourceReader.readFileAsString("importTest.libsonnet");
            final String json = TestResourceReader.readFileAsString("importLibsonnetTest.json");
            Mapper mapper = new Mapper("local testlib = import 'importTest.libsonnet'; local teststr = import 'importLibsonnetTest.json'; { greeting: testlib.sayHello('World') }", Collections.emptyList(), new HashMap<String, String>() {{
                put("importTest.libsonnet", lib);
                put("importLibsonnetTest.json", json);
            }});
        } catch (IllegalArgumentException e) {
            fail("This test should not fail, only libraries are evaluated at this point");
        }
    }

    @Test
    void importLibsonnetFail() throws Exception {
        try {
            final String libErr = TestResourceReader.readFileAsString("importTestFail.libsonnet");
            Mapper mapper = new Mapper("local testlib = import 'importTestFail.libsonnet'; { greeting: testlib.sayHello('World') }",
                    Collections.emptyList(), Collections.singletonMap("importTestFail.libsonnet", libErr));
            fail("This test should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to parse library: importTestFail.libsonnet"), "Found message: " + e.getMessage());
        }
    }
}
