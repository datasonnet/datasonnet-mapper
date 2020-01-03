package com.datasonnet;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

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

        Map<String, Document<?>> variables = Collections.singletonMap("myVar", myVar);

        Mapper mapper = new Mapper(ds, variables.keySet(), true);
        String mapped = mapper.transform(payload, variables, "application/csv").getContents().toString();

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

        Mapper mapper = new Mapper(ds, Collections.emptyList(), true);
        String mapped = mapper.transform(payload, Collections.emptyMap(), "text/plain").getContents().toString();
        assertEquals("HelloWorld", mapped);
        mapped = mapper.transform(payload, Collections.emptyMap(), "application/test.test").getContents().toString();
        assertEquals("GoodByeWorld", mapped);
    }

    @Test
    void testIllegalParameter() throws Exception {
        Document payload = new StringDocument(
                "TestResource",
                "application/test.test"
        );
        String ds = TestResourceReader.readFileAsString("illegalParameter.ds");

        Mapper mapper = new Mapper(ds, Collections.emptyList(), true);

        try {
            String mapped = mapper.transform(payload, Collections.emptyMap(), "text/plain").getContents().toString();
            fail("Must fail to transform");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("The parameter 'BadParam' not supported by plugin TEST"), "Found message: " + e.getMessage());
        }
    }

}
