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
import com.datasonnet.document.MediaType;
import com.datasonnet.header.Header;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

public class HeaderTest {

    String headerStr ="/** DataSonnet\n" +
            "version=1.0\n" +
            "input payload application/xml;ds.xml.namespace-separator=\":\"\n" +
            "input * application/xml;ds.xml.text-value-key=__text\n" +
            "dataformat application/csv;separator=|\n" +
            "output application/csv;ds.csv.quote=\"\\\"\"\n" +
            "*/\n" +
            "[\n" +
            "    {\n" +
            "        greetings: payload[\"test:root\"].__text,\n" +
            "        name: myVar[\"test:myvar\"].__text\n" +
            "    }\n" +
            "]\n" +
            "\n";

    @Test
    void testHeader() throws Exception {

        Header header = Header.parseHeader(headerStr);

        // TODO: 8/5/20 run assertions on parsed header
    }
}
