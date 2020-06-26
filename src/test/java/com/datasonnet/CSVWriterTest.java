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
    
    @Test
    void testCSVWriter() throws URISyntaxException, IOException {

        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                "application/json"
        );

        Mapper mapper = new Mapper("payload");


        String mapped = mapper.transform(data, Collections.emptyMap(), "application/csv").getContentsAsString();
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(data, Collections.emptyMap(), "application/csv").getContentsAsString();
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunction() throws URISyntaxException, IOException {

        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                "application/json"
        );

        Mapper mapper = new Mapper("{ embeddedCSVValue: DS.Formats.write(payload, \"application/csv\") }");


        String mapped = mapper.transform(data, Collections.emptyMap(), "application/json").getContentsAsString();
        String expected = "{\"embeddedCSVValue\":\"\\\"First Name\\\",\\\"Last Name\\\",Phone\\nWilliam,Shakespeare,\\\"(123)456-7890\\\"\\nChristopher,Marlow,\\\"(987)654-3210\\\"\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunctionExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                "application/json"
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVFunctionExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);

        String mapped = mapper.transform(data, Collections.emptyMap(), "application/json").getContentsAsString();
        String expected = "{\"embeddedCSVValue\":\"'First Name'|'Last Name'|'Phone'\\n'William'|'Shakespeare'|'(123)456-7890'\\n'Christopher'|'Marlow'|'(987)654-3210'\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

}
