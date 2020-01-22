package com.datasonnet;

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
        Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(), Collections.singletonMap("output.json", "{\"a\": 5}"),true);
        String result = mapper.transform("{}");
        assertEquals("{\"a\":5}", result);
    }

    @Test
    void importParseErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a b"), true);
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("end-of-input at line 1 column 3"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void importExecuteErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", Collections.emptyList(),
                    Collections.singletonMap("output.json", "a.b"), true);
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch(IllegalArgumentException e) {
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
            }}, true);
        } catch (IllegalArgumentException e) {
            fail("This test should not fail, only libraries are evaluated at this point");
        }
    }

    @Test
    void importLibsonnetFail() throws Exception {
        try {
            final String libErr = TestResourceReader.readFileAsString("importTestFail.libsonnet");
            Mapper mapper = new Mapper("local testlib = import 'importTestFail.libsonnet'; { greeting: testlib.sayHello('World') }",
                    Collections.emptyList(), Collections.singletonMap("importTestFail.libsonnet", libErr), true);
            fail("This test should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unable to parse library: importTestFail.libsonnet"), "Found message: " + e.getMessage());
        }
    }
}
