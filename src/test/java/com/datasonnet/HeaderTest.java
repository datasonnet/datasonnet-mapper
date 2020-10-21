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

import com.datasonnet.header.Header;
import org.junit.jupiter.api.BeforeAll;
import com.datasonnet.header.HeaderParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HeaderTest {

    Header header;

    String headerStr = "/** DataSonnet\n" +
            "version=2.0\n" +
            "preserveOrder=false\n" +
            "input payload application/xml;namespace-separator=\":\";text-value-key=__text\n" +
            "input * application/xml;text-value-key=__text\n" +
            "input myvar application/csv;separator=|\n" +
            "dataformat application/vnd.ms-excel;payload.param=xyz\n" +
            "output application/csv;ds.csv.quote=\"\"\"\n" +
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
    void testHeaderVersion() {
        assertEquals(header.getVersion(), "2.0");
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
        Map<String, String> parameters = header.getNamedInputs().values().iterator().next().getParameters();
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
        Set<String> keys = header.getOutput().getParameters().keySet();
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
}
