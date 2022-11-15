package com.datasonnet;

/*-
 * Copyright 2019-2022 the original author or authors.
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
import org.apache.commons.fileupload.*;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MultipartReaderTest {

    @Test
    void testMultipartReader() throws URISyntaxException, IOException, JSONException {
        byte[] binaryFile = TestResourceReader.readFileAsBytes("DataSonnet.png");

        //Construct MultiPart data
        MultipartEntityBuilder entity = MultipartEntityBuilder.create();
        entity.addTextBody("textPart", "Hello World", ContentType.create("text/plain", "UTF-8"));
        entity.addBinaryBody("binaryPart",
                binaryFile,
                ContentType.create("image/png"),
                "DataSonnet.png"
        );
        HttpEntity httpEntity = entity.build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        httpEntity.writeTo(out);

        Document<byte[]> data = new DefaultDocument<>(
                out.toByteArray(),
                MediaTypes.MULTIPART_FORM_DATA
        );

        Mapper mapper = new Mapper("payload");
        String result = mapper.transform(data, Collections.emptyMap(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = TestResourceReader.readFileAsString("multipartReaderTest.json");
        JSONAssert.assertEquals(expectedJson, result, true);
    }
}
