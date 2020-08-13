package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLTest {

    @Test
    void testEncodeDecode() throws Exception {
        String data = "Hello World";
        String encodedData = "Hello+World";
        Mapper mapper = new Mapper("DS.URL.encode(payload)");
        String result = mapper.transform(new DefaultDocument<String>(data, MediaTypes.TEXT_PLAIN), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();
        assertEquals(encodedData, result);
        mapper = new Mapper("DS.URL.decode(payload)");
        result = mapper.transform(new DefaultDocument<String>(result, MediaTypes.TEXT_PLAIN), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();
        assertEquals(data, result);
    }

}
