package com.datasonnet;

import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVWriterTest {

    @Test
    void testCSVWriter() throws URISyntaxException, IOException {

        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                "application/json"
        );

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper("DS.Formats.write(payload, \"application/csv\")", Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.contents().trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String jsonnet = TestResourceReader.readFileAsString("writeCSVExtTest.ds");

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.contents().trim());
    }

}
