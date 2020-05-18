package com.datasonnet;

import com.datasonnet.document.StringDocument;
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLTest {

    @Test
    void testEncodeDecode() throws Exception {
        String data = "Hello World";
        String encodedData = "Hello+World";
        Mapper mapper = new Mapper("DS.URL.encode(payload)");
        String result = mapper.transform(new StringDocument(data, "text/plain"), Collections.emptyMap(), "text/plain").getContentsAsString();
        assertEquals(encodedData, result);
        mapper = new Mapper("DS.URL.decode(payload)");
        result = mapper.transform(new StringDocument(result, "text/plain"), Collections.emptyMap(), "text/plain").getContentsAsString();
        assertEquals(data, result);
    }

}
