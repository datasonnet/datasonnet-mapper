package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVReaderTest {

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        Document<String> data = new DefaultDocument<String>(
                TestResourceReader.readFileAsString("readCSVTest.csv"),
                MediaTypes.APPLICATION_CSV
        );

        Mapper mapper = new Mapper("{ fName: payload[0][\"First Name\"] }");


        Document<String> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertEquals("{\"fName\":\"Eugene\"}", mapped.getContent());
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        Document<String> data = new DefaultDocument<>(
                TestResourceReader.readFileAsString("readCSVExtTest.csv"),
                MediaTypes.APPLICATION_CSV
        );
        String jsonnet = TestResourceReader.readFileAsString("readCSVExtTest.ds");

        Mapper mapper = new Mapper(jsonnet);


        Document<String> mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON);

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mapped.getContent());
    }


}
