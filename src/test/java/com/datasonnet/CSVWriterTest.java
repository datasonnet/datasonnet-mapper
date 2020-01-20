package com.datasonnet;

import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVWriterTest {

    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }
    
    @Test
    void testCSVWriter() throws URISyntaxException, IOException {

        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                "application/json"
        );

        Mapper mapper = new Mapper("payload", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.contents().trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVExtTest.ds");

        Mapper mapper = new Mapper(datasonnet, new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.contents().trim());
    }

}
