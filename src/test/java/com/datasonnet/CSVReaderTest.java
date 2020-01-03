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

public class CSVReaderTest {

    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("readCSVTest.csv"),
                "application/csv"
        );

        Mapper mapper = new Mapper("{ fName: payload[0][\"First Name\"] }", Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");

        assertEquals("{\"fName\":\"Eugene\"}", mapped.getContents());
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("readCSVExtTest.csv"),
                "application/csv"
        );
        String jsonnet = TestResourceReader.readFileAsString("readCSVExtTest.ds");

        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mapped.getContents());
    }


}
