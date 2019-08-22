package com.datasonnet;

import com.datasonnet.util.TestResourceReader;
import com.datasonnet.wrap.Mapper;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CSVReaderTest {

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        String jsonData = TestResourceReader.readFileAsString("readCSVTest.csv");

        Mapper mapper = new Mapper("local csvInput = PortX.CSV.read(payload); { fName: csvInput[0][\"First Name\"] }", new HashMap<>(), true);
        String mappedJson = mapper.transform(jsonData, "application/csv");

        assertEquals("{\"fName\":\"Eugene\"}", mappedJson);
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        String jsonData = TestResourceReader.readFileAsString("readCSVExtTest.csv");
        String jsonnet = TestResourceReader.readFileAsString("readCSVExtTest.jsonnet");

        Mapper mapper = new Mapper(jsonnet, new HashMap<>(), true);
        String mappedJson = mapper.transform(jsonData, "application/csv");

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mappedJson);
    }


}
