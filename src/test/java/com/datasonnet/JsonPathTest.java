package com.datasonnet;

import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;

public class JsonPathTest {

    @Test
    void testJsonPathSelector() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("jsonPathTest.json");

        Mapper mapper = new Mapper("PortX.JsonPath.select(payload, \"$..book[-2:]..author\")[0]", new ArrayList<>(), true);
        String mappedJson = mapper.transform(new StringDocument(jsonData, "application/json"), new HashMap<>(), "application/json").contents();

        assertEquals(mappedJson, "\"Herman Melville\"");
    }

}
