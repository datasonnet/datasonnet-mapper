package com.datasonnet;

import com.datasonnet.wrap.Mapper;
import org.apache.commons.text.StringEscapeUtils;
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

public class CSVMapperTest {

    @Test
    void testCSVReader() throws URISyntaxException, IOException {
        String jsonData = "\"" + StringEscapeUtils.escapeJson(readFileAsString("test.csv")) + "\"";

        Mapper mapper = new Mapper("local csvInput = std.parseJson(PortX.CSV.read(payload)); { fName: csvInput[0][\"First Name\"] }", new HashMap<>(), true);
        String mappedJson = mapper.transform(jsonData);//.replaceAll("\\\"", "\"");

        assertEquals("{\"fName\":\"Eugene\"}", mappedJson);
    }

    @Test
    void testCSVReaderExt() throws IOException, URISyntaxException {
        String jsonData = "\"" + StringEscapeUtils.escapeJson(readFileAsString("test2.csv")) + "\"";
        String jsonnet = readFileAsString("CSVExt.jsonnet");

        Mapper mapper = new Mapper(jsonnet, new HashMap<>(), true);
        String mappedJson = mapper.transform(jsonData);

        assertEquals("{\"fName\":\"Eugene\",\"num\":\"234\"}", mappedJson);
    }

    private String readFileAsString(String filePath) throws URISyntaxException, IOException {
        Path path = Paths.get(getClass().getClassLoader()
                .getResource(filePath).toURI());

        Stream<String> lines = Files.lines(path);
        String data = lines.collect(Collectors.joining("\n"));
        lines.close();

        return data;
    }
}
