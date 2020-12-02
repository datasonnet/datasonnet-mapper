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

import com.datasonnet.document.MediaTypes;
import com.datasonnet.header.Header;
import com.datasonnet.header.HeaderParseException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HeaderTest {

    Header header;

    String headerStr = "/** DataSonnet\n" +
            "version=2.0\n" +
            "preserveOrder=false\n" +
            "  \n" +
            "// comment\n" +
            "input payload application/xml;namespace-separator=\":\";text-value-key=__text\n" +
            "input * application/xml;text-value-key=__text\n" +
            "input myvar application/csv;separator=|\n" +
            "dataformat application/vnd.ms-excel;payload.param=xyz\n" +
            "  output application/csv;ds.csv.quote=\"\"\"\n" +
            "*/\n" +
            "[\n" +
            "    {\n" +
            "        greetings: payload[\"test:root\"].__text,\n" +
            "        name: myVar[\"test:myvar\"].__text\n" +
            "    }\n" +
            "]\n" +
            "\n";


    @BeforeAll
    void setUp() throws Exception{
        header = Header.parseHeader(headerStr);
    }

    @Test
    void testHeaderVersion() throws HeaderParseException {
        assertEquals(header.getVersion(), "2.0");
        assertEquals(Header.parseHeader(
                "/** DataSonnet\n" +
                "version=2.1\n" +
                "*/\n"
        ).getVersion(), "2.1");
        assertEquals(Header.parseHeader(
                "/** DataSonnet\n" +
                        "version=2.15678.45678\n" +
                        "*/\n"
        ).getVersion(), "2.15678.45678");
        assertThrows(HeaderParseException.class,  ()  -> {
            Header.parseHeader(
                    "/** DataSonnet\n" +
                            "version=1.1\n" +
                            "*/\n"
            );});
        assertThrows(HeaderParseException.class,  ()  -> {
            Header.parseHeader(
                    "/** DataSonnet\n" +
                    "version=3.2\n" +
                    "*/\n"
            );});
    }

    @Test
    void testHeaderPreserveOrder() {
        assertEquals(header.isPreserveOrder(), false);
    }

    @Test
    void testHeaderAllInputs()  {
        Set<String> allInputs = header.getAllInputs().iterator().next().getParameters().keySet();
        assertTrue(allInputs.contains("text-value-key"));
    }

    @Test
    void testHeaderNamedInputs() {
        Set<String> namedInputs = header.getNamedInputs().keySet();
        assertTrue(namedInputs.contains("payload"));
        assertTrue(namedInputs.contains("myvar"));
    }

    @Test
    void testHeaderNamedInputCommaSeparated() {
        Map<String, String> parameters = header.getDefaultNamedInput("payload").orElseThrow(AssertionError::new).getParameters();
        assertTrue(parameters.containsKey("namespace-separator"));
        assertTrue(parameters.containsKey("text-value-key"));
    }

    @Test
    void testHeaderDataformat() {
        String subtype = header.getDataFormats().iterator().next().getSubtype();
        assertTrue(subtype.equals("vnd.ms-excel"));
    }

    @Test
    void testHeaderOutput() {
        Set<String> keys = header.getDefaultOutput().orElseThrow(AssertionError::new).getParameters().keySet();
        assertTrue(keys.contains("ds.csv.quote"));
    }

    @Test
    void testUnknownHeaderFails() {
        assertThrows(HeaderParseException.class,  ()  -> {
           Header.parseHeader("/** DataSonnet\n" +
                   "version=2.0\n" +
                   "nonsense\n" +
                   "*/");
        });
    }

    @Test
    void testUnterminatedHeaderFailsNicely() {
        assertThrows(HeaderParseException.class,  ()  -> {
            Header.parseHeader("/** DataSonnet\n" +
                    "version=2.0\n");
        });
    }

    @Test
    public void testDefaultOutput() throws HeaderParseException {
        Header header1 = Header.parseHeader("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/x-java-object;q=0.9\n" +
                "output application/json;q=1.0\n" +
                "*/");

        assertTrue(header1.getDefaultOutput().isPresent());
        assertTrue(MediaTypes.APPLICATION_JSON.equalsTypeAndSubtype(header1.getDefaultOutput().get()));
    }

    @Test
    public void testDefaultInput() throws HeaderParseException {
        Header header1 = Header.parseHeader("/** DataSonnet\n" +
                "version=2.0\n" +
                "input payload application/x-java-object;q=1.0\n" +
                "input payload application/json;q=0.9\n" +
                "*/");

        assertTrue(header1.getDefaultPayload().isPresent());
        assertTrue(MediaTypes.APPLICATION_JAVA.equalsTypeAndSubtype(header1.getDefaultPayload().get()));
    }
}
