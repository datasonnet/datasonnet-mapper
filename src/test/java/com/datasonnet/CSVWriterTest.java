package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
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

        Document<String> data = new DefaultDocument<>(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("payload");


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV).getContent();
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        Document<String> data = new DefaultDocument<>(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                MediaTypes.APPLICATION_JSON
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV).getContent();
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunction() throws URISyntaxException, IOException {

        Document data = new DefaultDocument<String>(
                TestResourceReader.readFileAsString("writeCSVTest.json"),
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("{ embeddedCSVValue: ds.write(payload, \"application/csv\") }");


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        String expected = "{\"embeddedCSVValue\":\"\\\"First Name\\\",\\\"Last Name\\\",Phone\\nWilliam,Shakespeare,\\\"(123)456-7890\\\"\\nChristopher,Marlow,\\\"(987)654-3210\\\"\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

    @Test
    void testCSVWriteFunctionExt() throws IOException, URISyntaxException {
        Document data = new DefaultDocument<String>(
                TestResourceReader.readFileAsString("writeCSVExtTest.json"),
                MediaTypes.APPLICATION_JSON
        );
        String datasonnet = TestResourceReader.readFileAsString("writeCSVFunctionExtTest.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        String expected = "{\"embeddedCSVValue\":\"'William'|'Shakespeare'|'(123)456-7890'\\n'Christopher'|'Marlow'|'(987)654-3210'\\n\"}";
        assertEquals(expected.trim(), mapped.trim());
    }

}
