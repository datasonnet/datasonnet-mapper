package com.datasonnet;

import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import com.datasonnet.Mapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class XMLWriterTest {

    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }

    @Test
    void testOverrideNamespaces() throws Exception {
        String json = "{\"b:a\":{\"@xmlns\":{\"b\":\"http://example.com/1\",\"b1\":\"http://example.com/2\"},\"b1:b\":{}}}";
        String datasonnet = TestResourceReader.readFileAsString("xmlOverrideNamespaces.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mapped = mapper.transform(new StringDocument(json, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        // original mapping is gone
        assertThat(mapped, not(containsString("b:a")));
        assertThat(mapped, not(containsString("b1:b")));

        // elements are in new namespaces
        assertThat(mapped, containsString("c:a"));
        assertThat(mapped, containsString("<b"));

        // namespaces defined
        assertThat(mapped, containsString("xmlns:c=\"http://example.com/1\""));
        assertThat(mapped, containsString("xmlns=\"http://example.com/2\""));
    }

    @Test
    void testNamespaceBump() throws Exception {
        String json = "{\"b:a\":{\"@xmlns\":{\"b\":\"http://example.com/1\",\"b1\":\"http://example.com/2\"},\"b1:b\":{}}}";

        String datasonnet = TestResourceReader.readFileAsString("xmlNamespaceBump.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mapped = mapper.transform(new StringDocument(json, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();


        // original mapping is gone
        assertThat(mapped, not(containsString("b:a")));
        assertThat(mapped, not(containsString("b1:b")));

        // elements are in new namespaces
        assertThat(mapped, containsString("b1:a"));

        // namespaces defined
        assertThat(mapped, containsString("xmlns:b1=\"http://example.com/1\""));
        assertThat(mapped, containsString("http://example.com/2"));
    }

    @Test
    void testXMLWriterExt() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("readXMLExtTest.json");
        String datasonnet = TestResourceReader.readFileAsString("writeXMLExtTest.ds");
        String expectedXml = TestResourceReader.readFileAsString("readXMLExtTest.xml");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        Map<String, String> namespaces = new HashMap<>();
        namespaces.put("test", "http://www.modusbox.com");
        namespaces.put("datasonnet", "http://www.modusbox.com");

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).withNamespaceContext(namespaces).ignoreWhitespace());
    }

    @Test
    void testNonAscii() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlNonAscii.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlNonAscii.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlNonAscii.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertEquals(expectedXml, mappedXml);

        //XMLUnit does not support non-ascii
        //assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testCDATA() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlCDATA.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlCDATA.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlNonAscii.ds");//Reuse existing one to avoid duplication

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    //@Test
    void testXMLMixedContent() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlMixedContent.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlMixedContent.xml");

        Mapper mapper = new Mapper("local params = {\n" +
                "    \"XmlVersion\" : \"1.1\"\n" +
                "};DS.Formats.write(payload, \"application/xml\", params)", Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testEmptyElements() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlEmptyElements.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlEmptyElementsNull.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlEmptyElementsNull.ds");

        Mapper mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());

        expectedXml = TestResourceReader.readFileAsString("xmlEmptyElementsNoNull.xml");
        datasonnet = TestResourceReader.readFileAsString("xmlEmptyElementsNoNull.ds");

        mapper = new Mapper(datasonnet, Collections.emptyList(), true);
        mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testOmitXml() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlEmptyElements.json");

        Mapper mapper = new Mapper("/** DataSonnet\n" +
                "version=1.0\n" +
                "output.application/xml.OmitXmlDeclaration=true\n" +
                "*/\n" +
                "payload", Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertFalse(mappedXml.contains("<?xml"));

        mapper = new Mapper("/** DataSonnet\n" +
                "version=1.0\n" +
                "output.application/xml.OmitXmlDeclaration=false\n" +
                "*/\n" +
                "payload", Collections.emptyList(), true);

        mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();

        assertTrue(mappedXml.startsWith("<?xml"));
    }

    @Test
    void testXMLRoot() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlRoot.json");

        Mapper mapper = new Mapper("DS.Formats.write(payload, \"application/xml\")", Collections.emptyList(), true);
        try {
            String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();
            fail("Must fail to transform");
        } catch(IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Object must have only one root element"), "Found message: " + e.getMessage());
        }

        mapper = new Mapper("/** DataSonnet\n" +
                "version=1.0\n" +
                "output.application/xml.RootElement=TestRoot\n" +
                "*/\n" +
                "payload", Collections.emptyList(), true);
        try {
            String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();
        } catch(IllegalArgumentException e) {
            fail("This transformation should not fail");
        }
    }

    void simpleJsonTest() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("test.json");

        Mapper mapper = new Mapper("DS.Formats.write(payload, \"application/xml\")", Collections.emptyList(), true);
        String mappedXml = mapper.transform(new StringDocument(jsonData, "application/json"), Collections.emptyMap(), "application/xml").getContents().toString();


        assertTrue(mappedXml.contains("<?xml"));
    }
}
