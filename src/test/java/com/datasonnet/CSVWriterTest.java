package com.datasonnet;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

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

        Mapper mapper = new Mapper("payload", Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.getContents().toString().trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVExtTest.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.getContents().toString().trim());
    }

    @Test
    void testCSVWriteFunction() throws URISyntaxException, IOException {

        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                "application/json"
        );

        Mapper mapper = new Mapper("{ embeddedCSVValue: DS.Formats.write(payload, \"application/csv\") }", Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");
        String expected = "{\"embeddedCSVValue\":\"\\\"First Name\\\",\\\"Last Name\\\",Phone\\nWilliam,Shakespeare,\\\"(123)456-7890\\\"\\nChristopher,Marlow,\\\"(987)654-3210\\\"\\n\"}";
        assertEquals(expected.trim(), mapped.getContents().toString().trim());
    }

    @Test
    void testCSVWriteFunctionExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVFunctionExtTest.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");
        String expected = "{\"embeddedCSVValue\":\"'William'|'Shakespeare'|'(123)456-7890'\\n'Christopher'|'Marlow'|'(987)654-3210'\\n\"}";
        assertEquals(expected.trim(), mapped.getContents().toString().trim());
    }

}
