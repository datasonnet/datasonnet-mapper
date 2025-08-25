package com.datasonnet.plugins;

/*-
 * Copyright 2019-2025 the original author or authors.
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
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.fileupload2.core.*;
import org.apache.commons.fileupload2.jakarta.JakartaServletDiskFileUpload;
import ujson.Value;

import java.io.*;
import java.util.List;
import java.util.Map;

public class MimeMultipartPlugin extends BaseJacksonDataFormatPlugin {
    public static final String DS_PARAM_BOUNDARY = "boundary";

    private static final String lineFeed = "\r\n";

    public MimeMultipartPlugin() {
        supportedTypes.add(MediaTypes.MULTIPART_FORM_DATA);
        supportedTypes.add(MediaTypes.MULTIPART_MIXED);
        supportedTypes.add(MediaTypes.MULTIPART_RELATED);

        writerParams.add(DS_PARAM_BOUNDARY);
        readerParams.add(DS_PARAM_BOUNDARY);

        readerSupportedClasses.add(String.class);
        readerSupportedClasses.add(InputStream.class);
        readerSupportedClasses.add(byte[].class);

        writerSupportedClasses.add(String.class);
        writerSupportedClasses.add(ByteArrayOutputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }
        Map<String, String> params = doc.getMediaType().getParameters();

        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        ByteArrayInputStream multipartData = null;
        String boundary = null;

        if (String.class.isAssignableFrom(doc.getContent().getClass())) {
            multipartData = new ByteArrayInputStream(doc.getContent().toString().getBytes());
            boundary = params.getOrDefault(DS_PARAM_BOUNDARY, doc.getContent().toString().split("\r\n")[0].substring(2));
        } else if (byte[].class.isAssignableFrom(doc.getContent().getClass())) {
            multipartData = new ByteArrayInputStream((byte[]) doc.getContent());
            boundary = params.getOrDefault(DS_PARAM_BOUNDARY, new String((byte[]) doc.getContent()).split("\r\n")[0].substring(2));
        } else if (InputStream.class.isAssignableFrom(doc.getContent().getClass())) {
            InputStream is = (InputStream) doc.getContent();
            try {
                byte[] buf = new byte[is.available()];
                while (is.read(buf) != -1) {
                }
                multipartData = new ByteArrayInputStream(buf);
                boundary = params.getOrDefault(DS_PARAM_BOUNDARY, new String(buf).split("\r\n")[0].substring(2));
            } catch (IOException e) {
                throw new PluginException("Unable to read multipart data", e);
            }
        }

        final String finalBoundary = boundary;
        final ByteArrayInputStream finalMultipartData = multipartData;

        try {
            List<DiskFileItem> parts = new JakartaServletDiskFileUpload().parseRequest(new RequestContext() {
                    @Override
                    public String getCharacterEncoding() {
                        return "UTF-8";
                    }

                    @Override
                    public String getContentType() {
                        return MediaTypes.MULTIPART_FORM_DATA_VALUE + "; boundary=" + finalBoundary;
                    }

                    @Override
                    public long getContentLength() {
                        return finalMultipartData.available();
                    }

                    @Override
                    public InputStream getInputStream() throws IOException {
                        return finalMultipartData;
                    }
                });

            for (FileItem part : parts) {
                ObjectNode partNode = mapper.createObjectNode();
                partNode.put("name", part.getFieldName());
                partNode.put("contentType", part.getContentType());
                String fileName = part.getName();
                if (fileName != null) {
                    partNode.put("fileName", fileName);
                    partNode.put("content", part.get());
                } else {
                    partNode.put("content", part.getString());
                }
                result.add(partNode);
            }

        } catch (FileUploadException e) {
            throw new PluginException("Unable to parse multipart data", e);
        }

        return ujsonFrom(result);
    }

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Map<String, String> params = mediaType.getParameters();

        final String boundary = params.getOrDefault(DS_PARAM_BOUNDARY, "===" + System.currentTimeMillis() + "===");

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream);

        Object javaInput = ujsonUtils.javaObjectFrom(input);
        if (!List.class.isAssignableFrom(javaInput.getClass())) {
            throw new PluginException("Invalid format; array of part objects is expected");
        }
        List<Map> parts = (List<Map>) javaInput;
        for (Map nextPart : parts) {
            if (!nextPart.containsKey("name")) {
                throw new PluginException("Invalid part format; part name is required");
            }
            if (!nextPart.containsKey("contentType")) {
                throw new PluginException("Invalid part format; part content type is required");
            }
            if (!nextPart.containsKey("content")) {
                throw new PluginException("Invalid part format; part content is required");
            }

            boolean isBinary = nextPart.containsKey("fileName");

            writer.append("--" + boundary).append(lineFeed);
            writer.append("Content-Disposition: form-data; name=\"" + nextPart.get("name") + "\"")
                    .append(isBinary ? "; filename=\"" + nextPart.get("fileName") + "\"" : "")
                    .append(lineFeed);
            writer.append("Content-Type: " + nextPart.get("contentType")).append(
                    lineFeed);
            writer.append("Content-Transfer-Encoding: " + (isBinary ? "binary" : "8bit")).append(lineFeed).append(lineFeed);

            if (isBinary) {
                writer.flush();

                List<Number> fileBytes = (List<Number>) nextPart.get("content");
                ByteArrayOutputStream content = new ByteArrayOutputStream();
                for (Number nextOne : fileBytes) {
                    content.write(nextOne.byteValue());
                }
                byte[] contentBytes = content.toByteArray();
                try {
                    outputStream.write(contentBytes);
                    outputStream.flush();
                } catch (IOException e) {
                    throw new PluginException("Unable to write binary part", e);
                }
            } else {
                writer.append(nextPart.get("content").toString());
            }

            writer.append(lineFeed);
            writer.flush();
        }

        writer.append("--" + boundary + "--").append(lineFeed);
        writer.close();

        if (targetType.isAssignableFrom(ByteArrayOutputStream.class)) {
            return (Document<T>) new DefaultDocument<>(outputStream, mediaType);
        }
        if (targetType.isAssignableFrom(byte[].class)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                outputStream.writeTo(baos);
            } catch (IOException e) {
                throw new PluginException(e);
            }
            return (Document<T>) new DefaultDocument<>(baos.toByteArray(), mediaType);
        }
        if (targetType.isAssignableFrom(String.class)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try {
                outputStream.writeTo(baos);
            } catch (IOException e) {
                throw new PluginException(e);
            }
            return (Document<T>) new DefaultDocument<>(new String(baos.toByteArray()), mediaType);
        }
        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canWrite before invoking write"));
    }
}
