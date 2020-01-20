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

        Mapper mapper = new Mapper("{ fName: payload[0][\"First Name\"] }", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/json");

        assertEquals("{\"fName\":\"Eugene\"}", mapped.contents());
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        Document data = new StringDocument(
                TestResourceReader.readFileAsString("readCSVExtTest.csv"),
                "application/csv"
        );
        String jsonnet = TestResourceReader.readFileAsString("readCSVExtTest.ds");

        Mapper mapper = new Mapper(jsonnet, new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/json");

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mapped.contents());
    }


}
