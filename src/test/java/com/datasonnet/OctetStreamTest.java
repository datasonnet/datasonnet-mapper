package com.datasonnet;

/*-
 * Copyright 2019-2023 the original author or authors.
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
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class OctetStreamTest {

    @Test
    void testReadData() throws URISyntaxException, IOException, JSONException {
        byte[] binaryFile = TestResourceReader.readFileAsBytes("DataSonnet.png");

        Document<byte[]> data = new DefaultDocument<>(
                binaryFile,
                MediaTypes.APPLICATION_OCTET_STREAM
        );

        Mapper mapper = new Mapper("ds.binaries.toBase64(payload)");
        String result = mapper.transform(data, Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();

        byte[] resultBytes = Base64.getDecoder().decode(result);

        assertArrayEquals(binaryFile, resultBytes);
    }

    @Test
    void testWriteData() throws URISyntaxException, IOException, JSONException {
        byte[] binaryFile = TestResourceReader.readFileAsBytes("DataSonnet.png");
        String jsonFile = TestResourceReader.readFileAsString("binaryFile.json");

        Document<String> data = new DefaultDocument<>(
                jsonFile,
                MediaTypes.APPLICATION_JSON
        );

        Mapper mapper = new Mapper("payload");

        byte[] resultBytes = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_OCTET_STREAM, byte[].class).getContent();
        assertArrayEquals(binaryFile, resultBytes);
    }

}
