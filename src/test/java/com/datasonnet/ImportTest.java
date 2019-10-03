package com.datasonnet;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ImportTest {
    @Test
    void simpleImport() {
        Mapper mapper = new Mapper("import 'output.json'", new ArrayList<>(), new HashMap<String, String>() {{ put("output.json", "{\"a\": 5}"); }},true);
        String result = mapper.transform("{}");
        assertEquals("{\"a\":5}", result);
    }

    @Test
    void importParseErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", new ArrayList<>(), new HashMap<String, String>() {{
                put("output.json", "a b");
            }}, true);
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("end-of-input at line 1 column 3"), "Found message: " + e.getMessage());
        }
    }

    @Test
    void importExecuteErrorLineNumber() {
        try {
            Mapper mapper = new Mapper("import 'output.json'", new ArrayList<>(), new HashMap<String, String>() {{
                put("output.json", "a.b");
            }}, true);
            String result = mapper.transform("{}");
            fail("Import should fail");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("output.json line 1 column 1"), "Found message: " + e.getMessage());
            assertTrue(e.getMessage().contains("line 1 column 1 of the transformation"), "Found message: " + e.getMessage());
        }
    }
}
