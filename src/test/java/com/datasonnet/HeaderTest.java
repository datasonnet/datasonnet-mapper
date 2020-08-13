package com.datasonnet;

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
