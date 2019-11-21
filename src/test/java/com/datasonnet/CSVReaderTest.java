package com.datasonnet;

import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVReaderTest {

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("readCSVTest.csv"),
                "application/csv"
        );

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper("local csvInput = DS.Formats.read(payload, \"application/csv\"); { fName: csvInput[0][\"First Name\"] }", Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");

        assertEquals("{\"fName\":\"Eugene\"}", mapped.contents());
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("readCSVExtTest.csv"),
                "application/csv"
        );
        String jsonnet = TestResourceReader.readFileAsString("readCSVExtTest.ds");

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        Document mapped = mapper.transform(data, Collections.emptyMap(), "application/json");

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mapped.contents());
    }


}
