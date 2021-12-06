package com.datasonnet.plugins;

/*-
 * Copyright 2019-2021 the original author or authors.
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
import com.datasonnet.io.AutoDeleteFileInputStream;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.sett4.dataformat.xlsx.XlsxMapper;
import com.github.sett4.dataformat.xlsx.XlsxParser;

import ujson.Value;

import javax.swing.text.html.Option;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultXLSXFormatPlugin extends BaseJacksonDataFormatPlugin {
    public static final String DS_PARAM_USE_HEADER = "useheader";
    public static final String DS_PARAM_HEADERS = "headers";
    public static final String DS_PARAM_USE_TEMPFILE = "usetempfile";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final XlsxMapper XLSX_MAPPER = new XlsxMapper();

    //static {
    //    XLSX_MAPPER.enable(XlsxParser.Feature.WRAP_AS_ARRAY);
    //}

    public DefaultXLSXFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_XLSX);
        //supportedTypes.add(MediaType.parseMediaType("text/csv"));

        writerParams.add(DS_PARAM_USE_HEADER);
        writerParams.add(DS_PARAM_HEADERS);
        writerParams.add(DS_PARAM_USE_TEMPFILE);

        writerSupportedClasses.add(InputStream.class);
        writerSupportedClasses.add(byte[].class);
    }

    private boolean isUseHeader(MediaType mediaType) {
        if (mediaType.getParameter(DS_PARAM_USE_HEADER) != null) {
            return Boolean.parseBoolean(mediaType.getParameter(DS_PARAM_USE_HEADER));
        }
        return true;
    }
    private boolean isUseTempfile(MediaType mediaType) {
        if (mediaType.getParameter(DS_PARAM_USE_TEMPFILE) != null) {
            return Boolean.parseBoolean(mediaType.getParameter(DS_PARAM_USE_TEMPFILE));
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Map<String, String> params = mediaType.getParameters();
        CsvSchema.Builder builder = this.getBuilder(mediaType);

        try {
            final JsonNode jsonTree = OBJECT_MAPPER.valueToTree(ujsonUtils.javaObjectFrom(input));
            if (isUseHeader(mediaType)) {
                if (params.containsKey(DS_PARAM_HEADERS)) {
                    String[] headers = params.get(DS_PARAM_HEADERS).split(",");
                    for (String header : headers) {
                        builder.addColumn(header);
                    }
                } else {
                    JsonNode firstObject = jsonTree.elements().next();
                    firstObject.fieldNames().forEachRemaining(builder::addColumn);
                }
            }

            CsvSchema csvSchema = builder.build();

            if (targetType.isAssignableFrom(InputStream.class)) {
            	if (isUseTempfile(mediaType)) {
                    File f = File.createTempFile("datasonnet", "tmp");
                    OutputStream out =  new BufferedOutputStream(new FileOutputStream(f));
                    XLSX_MAPPER.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValue(out, jsonTree);
                    out.flush();
                    out.close();
                    return (Document<T>) new DefaultDocument<>(new BufferedInputStream(new AutoDeleteFileInputStream(f)), MediaTypes.APPLICATION_XLSX);
                }
                else {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    OutputStream out = new BufferedOutputStream(baos);
                    XLSX_MAPPER.writerFor(JsonNode.class)
                            .with(csvSchema)
                            .writeValue(out, jsonTree);
                    out.flush();
                    out.close();
                    return (Document<T>) new DefaultDocument<>(new BufferedInputStream(new ByteArrayInputStream(baos.toByteArray())), MediaTypes.APPLICATION_XLSX);
                }
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return (Document<T>) new DefaultDocument<>(XLSX_MAPPER.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValueAsBytes(jsonTree), MediaTypes.APPLICATION_XLSX);
            }

            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canWrite before invoking write"));

        } catch (IOException e) {
            throw new PluginException("Unable to processing XLSX", e);
        }
    }

    private CsvSchema.Builder getBuilder(MediaType mediaType) {
        CsvSchema.Builder builder = CsvSchema.builder();

        String useHeadrStr = mediaType.getParameter(DS_PARAM_USE_HEADER);
        boolean useHeader = Boolean.parseBoolean(Optional.ofNullable(useHeadrStr).orElse("true"));
        builder.setUseHeader(useHeader);

        return builder;
    }
}
