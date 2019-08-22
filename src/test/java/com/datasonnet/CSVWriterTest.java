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

public class CSVWriterTest {

    @Test
    void testCSVWriter() throws URISyntaxException, IOException {

        String jsonData = TestResourceReader.readFileAsString("writeCSVTest.json");
        Mapper mapper = new Mapper("PortX.CSV.write(payload)", new HashMap<>(), true);
        String mappedValue = mapper.transform(jsonData, "application/json", "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVTest.csv");
        assertEquals(expected, mappedValue);
    }

    @Test
    void testCSVWriterExt() throws IOException, URISyntaxException {
        String jsonData = TestResourceReader.readFileAsString("writeCSVExtTest.json");
        String jsonnet = TestResourceReader.readFileAsString("writeCSVExtTest.jsonnet");

        Mapper mapper = new Mapper(jsonnet, new HashMap<>(), true);
        String mappedValue = mapper.transform(jsonData, "application/json", "application/csv");
        String expected = TestResourceReader.readFileAsString("writeCSVExtTest.csv");
        assertEquals(expected, mappedValue);
    }

}
