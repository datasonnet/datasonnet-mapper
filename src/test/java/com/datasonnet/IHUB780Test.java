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
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;

import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class IHUB780Test {

    @Test
    void testCSV() throws IOException, URISyntaxException {
        Document data = new DefaultDocument<String>(
                TestResourceReader.readFileAsString("IHUB780/payload.xml"),
                MediaTypes.APPLICATION_XML
        );
        String expected = TestResourceReader.readFileAsString("IHUB780/output.csv");
        String datasonnet = TestResourceReader.readFileAsString("IHUB780/IHUB780.ds");

        Mapper mapper = new Mapper(datasonnet);
        String mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV).getContent();
        assertNotEquals(expected.trim(), mapped.trim());

        datasonnet = datasonnet.replaceAll("DisableQuotes=false", "DisableQuotes=true");
        mapper = new Mapper(datasonnet);
        mapped = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_CSV).getContent();

        assertEquals(expected.trim(), mapped.trim());
    }
}
