package com.datasonnet;

import com.datasonnet.document.StringDocument;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

public class JsonPathTest {

    @Test
    void testJsonPathSelector() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("jsonPathTest.json");

        Mapper mapper = new Mapper("DS.JsonPath.select(payload, \"$..book[-2:]..author\")[0]", Collections.emptyList(), true);
        String mappedJson = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/json").getContents().toString();

        assertEquals(mappedJson, "\"Herman Melville\"");
    }

    @Test
    void testJsonPathArrSelector() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("jsonPathArrTest.json");

        Mapper mapper = new Mapper("std.length(DS.JsonPath.select(payload, \"$..language[?(@.name == 'Java')]\")) > 0", Collections.emptyList(), true);
        String mappedJson = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/json").getContents().toString();

        assertEquals(mappedJson, "true");
    }

}
