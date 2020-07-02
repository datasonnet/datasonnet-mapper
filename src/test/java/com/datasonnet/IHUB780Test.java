package com.datasonnet;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class IHUB780Test {

    @Test
    void testCSV() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("IHUB780/payload.xml"),
                "application/xml"
        );
        String expected = TestResourceReader.readFileAsString("IHUB780/output.csv");
        String datasonnet = TestResourceReader.readFileAsString("IHUB780/IHUB780.ds");

        Mapper mapper = new Mapper(datasonnet);
        String mapped = mapper.transform(data, Collections.emptyMap(), "application/csv").getContentsAsString();
        assertNotEquals(expected.trim(), mapped.trim());

        datasonnet = datasonnet.replaceAll("output.application/csv.DisableQuotes=false", "output.application/csv.DisableQuotes=true");
        mapper = new Mapper(datasonnet);
        mapped = mapper.transform(data, Collections.emptyMap(), "application/csv").getContentsAsString();
        assertEquals(expected.trim(), mapped.trim());
    }
}
