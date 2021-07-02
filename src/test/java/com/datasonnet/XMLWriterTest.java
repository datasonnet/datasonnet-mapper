package com.datasonnet;

/*-
 * Copyright 2019-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;

public class XMLWriterTest {

    @Test
    void testOverrideNamespaces() throws Exception {
        String json = "{\"b:a\":{\"@xmlns\":{\"b\":\"http://example.com/1\",\"b1\":\"http://example.com/2\"},\"b1:b\":{}}}";
        String datasonnet = TestResourceReader.readFileAsString("xmlOverrideNamespaces.ds");

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

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

        Mapper mapper = new Mapper(datasonnet);


        String mapped = mapper.transform(new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();


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

        Mapper mapper = new Mapper(datasonnet);

        Document<String> mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML);
        assertEquals(MediaTypes.APPLICATION_XML, mappedXml.getMediaType());
        assertThat(mappedXml.getContent(), CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testNoDoubleWrite() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("writeXMLExtDouble.json");
        String datasonnet = TestResourceReader.readFileAsString("writeXMLExtTest.ds");
        String expectedXml = TestResourceReader.readFileAsString("readXMLExtTest.xml");

        Mapper mapper = new Mapper(datasonnet);

        String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testXMLEscaped() throws Exception {
        String datasonnet =
                "{ root: {" +
                "  '@name': 'QueryText'," +
                "   '$1': 'dDocTitle <substring> foo <and> dDocCreatedDate >= bar'" +
                " } " +
                "}";
        String expectedXml = TestResourceReader.readFileAsString("writeXMLEscapedTest.xml");

        Mapper mapper = new Mapper(datasonnet);
        String mappedXml = mapper.transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_XML, String.class).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testNonAscii() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("writerXmlNonAscii.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlNonAscii.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlNonAscii.ds");

        Mapper mapper = new Mapper(datasonnet);

        String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertEquals(expectedXml, mappedXml);

        //XMLUnit does not support non-ascii
        //assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testCDATA() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlCDATA.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlCDATA.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlNonAscii.ds");//Reuse existing one to avoid duplication

        Mapper mapper = new Mapper(datasonnet);

        String mappedXml = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testXMLMixedContent() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlMixedContent.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlMixedContent.xml");

        Mapper mapper = new Mapper("local params = {\n" +
                "    \"XmlVersion\" : \"1.1\"\n" +
                "};ds.write(payload, \"application/xml\", params)");


        String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testEmptyElements() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlEmptyElements.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlEmptyElementsNull.xml");
        String datasonnet = TestResourceReader.readFileAsString("xmlEmptyElementsNull.ds");

        Mapper mapper = new Mapper(datasonnet);

        String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());

        expectedXml = TestResourceReader.readFileAsString("xmlEmptyElementsNoNull.xml");
        datasonnet = TestResourceReader.readFileAsString("xmlEmptyElementsNoNull.ds");

        mapper = new Mapper(datasonnet);


        mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    void testOmitXml() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlEmptyElements.json");

        Mapper mapper = new Mapper("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml;OmitXmlDeclaration=true\n" +
                "*/\n" +
                "payload");

        String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertFalse(mappedXml.contains("<?xml"));

        mapper = new Mapper("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml;OmitXmlDeclaration=false\n" +
                "*/\n" +
                "payload");

        mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();

        assertTrue(mappedXml.startsWith("<?xml"));
    }

    // TODO add version using namespaces
    @Test
    void testFlattenMixedContent() throws Exception {
        String xmlData = TestResourceReader.readFileAsString("xmlMixedContent.xml");
        String expected = TestResourceReader.readFileAsString("xmlMixedContent.txt");

        Mapper mapper = new Mapper("ds.xml.flattenContents(payload.letter)");

        String mapped = mapper.transform(new DefaultDocument<>(xmlData, MediaType.valueOf(MediaTypes.APPLICATION_XML_VALUE)), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();

        assertEquals(expected, mapped);
    }

    @Test
    void testFlattenMixedContentWithNamespaces() throws Exception {
        String xmlData = TestResourceReader.readFileAsString("xmlMixedContentNamespaces.xml");
        String expected = TestResourceReader.readFileAsString("xmlMixedContent.txt");

        Mapper mapper = new Mapper("ds.xml.flattenContents(payload[\"ns:letter\"], {\"$\": \"https://example.com\"})");

        String mapped = mapper.transform(new DefaultDocument<>(xmlData, MediaType.valueOf(MediaTypes.APPLICATION_XML_VALUE)), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();

        assertEquals(expected, mapped, "Expected " + expected + " but got " + mapped);
    }

    @Test
    void testXMLRoot() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlRoot.json");

        Mapper mapper = new Mapper("ds.write(payload, \"application/xml\")");


        try {
            String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
            fail("Must fail to transform");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Object must have only one root element"), "Found message: " + e.getMessage());
        }

        mapper = new Mapper("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/xml; RootElement=TestRoot\n" +
                "*/\n" +
                "payload");

        try {
            String mappedXml = mapper.transform(new DefaultDocument<>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
        } catch (IllegalArgumentException e) {
            fail("This transformation should not fail");
        }
    }

    @Test
    void testNestedNamespaces() throws Exception {
        String jsonData = TestResourceReader.readFileAsString("xmlNestedNamespaces.json");
        String expectedXml = TestResourceReader.readFileAsString("xmlNestedNamespaces.xml");

        Mapper mapper = new Mapper("payload");

        String mappedXml = mapper.transform(new DefaultDocument<String>(jsonData, MediaTypes.APPLICATION_JSON), Collections.emptyMap(), MediaTypes.APPLICATION_XML).getContent();
        assertThat(mappedXml, CompareMatcher.isSimilarTo(expectedXml).ignoreWhitespace());
    }

    @Test
    public void testNull() {
        Mapper mapper = new Mapper("null");

        try {
            mapper.transform(DefaultDocument.NULL_INSTANCE, Collections.emptyMap(), MediaTypes.APPLICATION_XML);
            fail("Should not succeed");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Input for XML writer must be an Object"), "Failed with wrong message: " + e.getMessage());
        }
    }
}
