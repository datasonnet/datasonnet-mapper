package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

public class JsonPathTest {

    @Test
    void testJsonPathSelector() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("jsonPathTest.json");

        Mapper mapper = new Mapper("ds.jsonpath.select(payload, \"$..book[-2:]..author\")[0]");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "\"Herman Melville\"");
    }

    @Test
    void testJsonPathArrSelector() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("jsonPathArrTest.json");

        Mapper mapper = new Mapper("std.length(ds.jsonpath.select(payload, \"$..language[?(@.name == 'Java')]\")) > 0");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "true");
    }

}
