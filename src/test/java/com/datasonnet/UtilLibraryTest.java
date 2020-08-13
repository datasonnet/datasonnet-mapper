package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UtilLibraryTest {

    @Test
    void testDuplicates() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibDuplicatesTest.json");

        Mapper mapper = new Mapper("DS.Util.duplicates(payload.primitive)");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[\"hello\",\"world\"]");

        mapper = new Mapper("DS.Util.duplicates(payload.complex, function(x) x.language.name)");
        mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[{\"language\":{\"name\":\"Java8\",\"version\":\"1.8.0\"}}]");

        mapper = new Mapper("DS.Util.duplicates(payload.moreComplex, function(x) std.substr(x.language.version, 0, 3))");
        mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        assertEquals(mappedJson, "[{\"language\":{\"name\":\"Java1.8\",\"version\":\"1.8_152\"}}]");
    }

    @Test
    void testRemoveFields() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibRemoveFieldsTest.json");
        testDS("utilLibRemoveFieldsTest.ds", jsonData);
    }

    @Test
    void testReverse() throws Exception {
        String jsonData = "[\"a\",\"b\",\"c\",\"d\"]";
        Mapper mapper = new Mapper("DS.Util.reverse(payload)");
        String mappedJson = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "[\"d\",\"c\",\"b\",\"a\"]");
    }

    @Test
    void testGroupBy() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("utilLibGroupByTest.json");
        testDS("utilLibGroupByTest.ds", jsonData);
    }

    @Test
    void testRound() throws Exception {
        testDS("utilLibRoundTest.ds", "{}");
    }

    @Test
    void testCounts() throws Exception {
        testDS("utilLibCountsTest.ds", "{}");
    }

    @Test
    void testMapToObject() throws Exception {
        testDS("utilLibMapToObjectTest.ds", "{}");
    }

    private void testDS(String dsFileName, String input) throws Exception {
        String ds = TestResourceReader.readFileAsString(dsFileName);

        Mapper mapper = new Mapper(ds);
        String mappedJson = mapper.transform(new DefaultDocument<String>(input, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        assertEquals(mappedJson, "true");
    }
}
