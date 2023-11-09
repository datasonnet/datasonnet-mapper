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
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.RequestContext;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MultipartWriterTest {

    @Test
    void testMultipartWriter() throws URISyntaxException, IOException {
        byte[] binaryFile = TestResourceReader.readFileAsBytes("DataSonnet.png");
        String datasonnet = TestResourceReader.readFileAsString("multipartWriterTest.ds");

        Document<String> data = new DefaultDocument<>(
                "{}",
                MediaTypes.APPLICATION_JSON
        );

        Map<String, Document<?>> variables = Collections.singletonMap("image", new DefaultDocument<>(binaryFile, MediaTypes.UNKNOWN));
        Mapper mapper = new Mapper(datasonnet, variables.keySet());

        final ByteArrayOutputStream multiPart = mapper.transform(data, variables, MediaTypes.MULTIPART_FORM_DATA, ByteArrayOutputStream.class).getContent();
        assertNotNull(multiPart);

        //Extract boundary
        final String boundary = new String(multiPart.toByteArray()).split("\r\n")[0].substring(2);

        //Mimic servlet context here
        try {
            List<DiskFileItem> parts = new JakartaServletDiskFileUpload().parseRequest(new RequestContext() {
                @Override
                public String getCharacterEncoding() {
                    return "UTF-8";
                }

                @Override
                public String getContentType() {
                    return MediaTypes.MULTIPART_FORM_DATA_VALUE + "; boundary=" + boundary;
                }

                @Override
                public long getContentLength() {
                    return multiPart.size();
                }

                @Override
                public InputStream getInputStream() throws IOException {
                    return new ByteArrayInputStream(multiPart.toByteArray());
                }
            });
            for (DiskFileItem part : parts) {
                String fieldName = part.getFieldName();
                if ("textPart".equalsIgnoreCase(fieldName)) {
                    assertEquals("Hello World", part.getString());
                }
                if ("binaryPart".equalsIgnoreCase(fieldName)) {
                    assertEquals("DataSonnet.png", part.getName());
                    assertTrue(Arrays.equals(binaryFile, part.get()));
                }
            }
        } catch (FileUploadException e) {
            throw new IOException("Unable to parse multipart data", e);
        }
    }
}
