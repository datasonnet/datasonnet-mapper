package com.datasonnet;

/*-
 * Copyright 2019-2020 the original author or authors.
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
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class XMLReaderTest {

    @Test
    void testNonAscii() throws Exception {
        mapAndAssert("xmlNonAscii.xml", "xmlNonAscii.json");
    }

    @Test
    void testOverrideNamespaces() throws Exception {
        String xml = "<a xmlns='http://example.com/1' xmlns:b='http://example.com/2'><b:b/></a>";
        // note how b is bound to the default namespace, which means the 'b' above needs to be auto-rebound

        String jsonnet = "/** DataSonnet\n" +
                "version=2.0\n" +
                "input payload application/xml;NamespaceDeclarations.b=\"http://example.com/1\"\n" +
                "*/\n" +
                "payload";

        Mapper mapper = new Mapper(jsonnet);


        String mapped = mapper.transform(new DefaultDocument<>(xml, MediaTypes.APPLICATION_XML), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

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

        Mapper mapper = new Mapper(jsonnet);


        String mappedJson = mapper.transform(new DefaultDocument<>(xmlData, MediaType.valueOf(MediaTypes.APPLICATION_XML_VALUE)), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        JSONAssert.assertEquals(expectedJson, mappedJson, true);
    }

    @Test
    void nestedNamespaces() throws Exception {
        mapAndAssert("xmlNestedNamespaces.xml", "xmlNestedNamespaces.json");
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

        Mapper mapper = new Mapper("payload");

        String mappedJson = mapper.transform(new DefaultDocument<>(xmlData, MediaType.valueOf(MediaTypes.APPLICATION_XML_VALUE)), Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();
        JSONAssert.assertEquals(expectedJson, mappedJson, true);
    }

}
