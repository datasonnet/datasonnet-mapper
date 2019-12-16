package com.datasonnet;

import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class HeaderTest {

    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }

    @Test
    void testHeader() throws Exception {
        Document payload = new StringDocument(
                TestResourceReader.readFileAsString("headerTest.xml"),
                "application/xml"
        );
        Document myVar = new StringDocument(
                TestResourceReader.readFileAsString("headerTestVar.xml"),
                "application/xml"
        );
        String ds = TestResourceReader.readFileAsString("headerTest.ds");

        HashMap<String, Document> variables = new HashMap<>();
        variables.put("myVar", myVar);

        Mapper mapper = new Mapper(ds, variables.keySet(), true);
        String mapped = mapper.transform(payload, variables, "application/csv").contents();

        assertTrue(mapped.startsWith("\"greetings\"|\"name\""));
        assertTrue(mapped.trim().endsWith("\"Hello\"|\"World\""));
    }

    @Test
    void testDotMimeType() throws Exception {
        Document payload = new StringDocument(
                "TestResource",
                "application/test.test"
        );
        String ds = TestResourceReader.readFileAsString("dotMimeTypeTest.ds");

        Mapper mapper = new Mapper(ds, new ArrayList<>(), true);
        String mapped = mapper.transform(payload, new HashMap<>(), "text/plain").contents();
        assertTrue(mapped.endsWith("HelloWorld"));
        mapped = mapper.transform(payload, new HashMap<>(), "application/test.test").contents();
        assertTrue(mapped.endsWith("GoodByeWorld"));
    }
}
