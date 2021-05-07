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
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ujson.Value;

import javax.swing.text.html.Option;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DefaultCSVFormatPlugin extends BaseJacksonDataFormatPlugin {
    public static final String DS_PARAM_USE_HEADER = "useheader";
    public static final String DS_PARAM_QUOTE_CHAR = "quote";
    public static final String DS_PARAM_SEPARATOR_CHAR = "separator";
    public static final String DS_PARAM_ESCAPE_CHAR = "escape";
    public static final String DS_PARAM_NEW_LINE = "newline";
    public static final String DS_PARAM_HEADERS = "headers";
    public static final String DS_PARAM_DISABLE_QUOTES = "disablequotes";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CsvMapper CSV_MAPPER = new CsvMapper();

    static {
        CSV_MAPPER.enable(CsvParser.Feature.WRAP_AS_ARRAY);
    }

    public DefaultCSVFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_CSV);
        supportedTypes.add(MediaType.parseMediaType("text/csv"));

        readerParams.add(DS_PARAM_USE_HEADER);
        readerParams.add(DS_PARAM_QUOTE_CHAR);
        readerParams.add(DS_PARAM_SEPARATOR_CHAR);
        readerParams.add(DS_PARAM_ESCAPE_CHAR);
        readerParams.add(DS_PARAM_NEW_LINE);
        readerParams.add(DS_PARAM_HEADERS);
        readerParams.add(DS_PARAM_DISABLE_QUOTES);

        writerParams.addAll(readerParams);

        readerSupportedClasses.add(InputStream.class);
        readerSupportedClasses.add(byte[].class);
        readerSupportedClasses.add(String.class);

        writerSupportedClasses.add(OutputStream.class);
        writerSupportedClasses.add(byte[].class);
        writerSupportedClasses.add(String.class);
    }

    private boolean isUseHeader(MediaType mediaType) {
        if (mediaType.getParameter(DS_PARAM_USE_HEADER) != null) {
            return Boolean.parseBoolean(mediaType.getParameter(DS_PARAM_USE_HEADER));
        }
        return true;
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        CsvSchema.Builder builder = this.getBuilder(doc.getMediaType());
        boolean useHeader = isUseHeader(doc.getMediaType());
        CsvSchema csvSchema = builder.build();

        // Read data from CSV file
        try {
            if (String.class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = CSV_MAPPER
                        .readerFor(useHeader ? Map.class : List.class)
                        .with(csvSchema)
                        .readTree((String) doc.getContent());

                return ujsonFrom(result);
            } else if (byte[].class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = CSV_MAPPER
                        .readerFor(useHeader ? Map.class : List.class)
                        .with(csvSchema)
                        .readTree((byte[]) doc.getContent());

                return ujsonFrom(result);
            } else if (InputStream.class.isAssignableFrom(doc.getContent().getClass())) {
                JsonNode result = CSV_MAPPER
                        .readerFor(useHeader ? Map.class : List.class)
                        .with(csvSchema)
                        .readTree((InputStream) doc.getContent());

                return ujsonFrom(result);
            } else {
                throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
            }
        } catch (JsonProcessingException jpe) {
            throw new PluginException("Unable to convert CSV to JSON", jpe);
        } catch (IOException ioe) {
            throw new PluginException("Unable to read CSV input", ioe);
        }
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

            if (targetType.isAssignableFrom(String.class)) {
                return (Document<T>) new DefaultDocument<>(CSV_MAPPER.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValueAsString(jsonTree), MediaTypes.APPLICATION_CSV);
            }

            if (targetType.isAssignableFrom(OutputStream.class)) {
                OutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
                CSV_MAPPER.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValue(out, jsonTree);
                return (Document<T>) new DefaultDocument<>(out, MediaTypes.APPLICATION_CSV);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return (Document<T>) new DefaultDocument<>(CSV_MAPPER.writerFor(JsonNode.class)
                        .with(csvSchema)
                        .writeValueAsBytes(jsonTree), MediaTypes.APPLICATION_CSV);
            }

            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canWrite before invoking write"));

        } catch (IOException e) {
            throw new PluginException("Unable to processing CSV", e);
        }
    }

    private CsvSchema.Builder getBuilder(MediaType mediaType) {
        CsvSchema.Builder builder = CsvSchema.builder();

        String useHeadrStr = mediaType.getParameter(DS_PARAM_USE_HEADER);
        boolean useHeader = Boolean.parseBoolean(Optional.ofNullable(useHeadrStr).orElse("true"));
        builder.setUseHeader(useHeader);

        String disableQuotesStr = mediaType.getParameter(DS_PARAM_DISABLE_QUOTES);
        boolean disableQuotes = Boolean.parseBoolean(Optional.ofNullable(disableQuotesStr).orElse("false"));

        if(disableQuotes) {
            builder.disableQuoteChar();
        } else if (mediaType.getParameter(DS_PARAM_QUOTE_CHAR) != null) {
            builder.setQuoteChar(mediaType.getParameter(DS_PARAM_QUOTE_CHAR).charAt(0));
        }

        if (mediaType.getParameter(DS_PARAM_SEPARATOR_CHAR) != null) {
            builder.setColumnSeparator(mediaType.getParameter(DS_PARAM_SEPARATOR_CHAR).charAt(0));
        }

        if (mediaType.getParameter(DS_PARAM_ESCAPE_CHAR) != null) {
            builder.setEscapeChar(mediaType.getParameter(DS_PARAM_ESCAPE_CHAR).charAt(0)
            );
        }

        if (mediaType.getParameter(DS_PARAM_NEW_LINE) != null) {
            builder.setLineSeparator(mediaType.getParameter(DS_PARAM_NEW_LINE)
                    .replaceAll("LF", "\n")
                    .replaceAll("CR", "\r")
            );
        }

        return builder;
    }
}
