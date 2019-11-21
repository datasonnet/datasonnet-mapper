package com.datasonnet;

import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

public class XMLReaderTest {

    @Test
    void testNonAscii() throws Exception {
        mapAndAssert("xmlNonAscii.xml", "xmlNonAscii.json");
    }

    @Test
    void testOverrideNamespaces() throws Exception {
        String xml = "<a xmlns='http://example.com/1' xmlns:b='http://example.com/2'><b:b/></a>";
        // note how b is bound to the default namespace, which means the 'b' above needs to be auto-rebound
        //String jsonnet = "DS.Formats.readExt(payload, \"application/xml\", {NamespaceDeclarations: {b: \"http://example.com/1\"}})";
        String jsonnet = "DS.Formats.read(payload, \"application/xml\", {NamespaceDeclarations: {b: \"http://example.com/1\"}})";

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        String mapped = mapper.transform(new StringDocument(xml, "application/xml"), Collections.emptyMap(), "application/json").contents();

        // the b namespace must have been remapped
        assertThat(mapped, not(containsString("b:b")));
        // the default namespace must now be a
        assertThat(mapped, containsString("b:a"));

        // must include both namespaces
        assertThat(mapped, containsString("http://example.com/1"));
        assertThat(mapped, containsString("http://example.com/2"));

    }

    @Test
    void testXMLReaderExt() throws Exception {
        String xmlData = TestResourceReader.readFileAsString("readXMLExtTest.xml");
        String jsonnet = TestResourceReader.readFileAsString("readXMLExtTest.ds");
        String expectedJson = TestResourceReader.readFileAsString("readXMLExtTest.json");

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper(jsonnet, Collections.emptyList(), true);
        String mappedJson = mapper.transform(new StringDocument(xmlData, "application/xml"), Collections.emptyMap(), "application/json").contents();

        JSONAssert.assertEquals(expectedJson, mappedJson, false);
    }

    @Test
    void testMixedContent() throws Exception {
        mapAndAssert("xmlMixedContent.xml", "xmlMixedContent.json");
    }

    @Test
    void testCDATA() throws Exception {
        mapAndAssert("xmlCDATA.xml", "xmlCDATA.json");
    }

    @Test
    void testMultipleCDATA() throws Exception {
        mapAndAssert("xmlMultipleCDATA.xml", "xmlMultipleCDATA.json");
    }

    private void mapAndAssert(String inputFileName, String expectedFileName) throws Exception {
        String xmlData = TestResourceReader.readFileAsString(inputFileName);
        String expectedJson = TestResourceReader.readFileAsString(expectedFileName);

        DataFormatService.getInstance().findAndRegisterPlugins();

        Mapper mapper = new Mapper("DS.Formats.read(payload, \"application/xml\")", Collections.emptyList(), true);
        String mappedJson = mapper.transform(new StringDocument(xmlData, "application/xml"), Collections.emptyMap(), "application/json").contents();

        JSONAssert.assertEquals(expectedJson, mappedJson, false);
    }

}
