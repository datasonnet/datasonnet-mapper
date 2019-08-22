package com.datasonnet.portx;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;

public class CSVWriter
{
    public static String writeCSV(String input) throws IOException {
        return writeCSV(input, true, null, null, null, null);
    }

    public static String writeCSV(String input, boolean useHeader, String quote, String separator, String escape, String newLine) throws IOException {
        CsvSchema.Builder builder = CsvSchema.builder();
        JsonNode jsonTree = new ObjectMapper().readTree(input);

        if (useHeader) {
            JsonNode firstObject = jsonTree.elements().next();
            firstObject.fieldNames().forEachRemaining(fieldName -> {
                builder.addColumn(fieldName);
            });
            builder.setUseHeader(true);
        }
        if (quote != null) {
            builder.setQuoteChar(quote.charAt(0));
        }
        if (separator != null) {
            builder.setColumnSeparator(separator.charAt(0));
        }
        if (escape != null) {
            builder.setEscapeChar(escape.charAt(0));
        }
        if (newLine != null) {
            builder.setLineSeparator(newLine);
        }

        CsvSchema csvSchema = builder.build();

        CsvMapper csvMapper = new CsvMapper();
        String value = csvMapper.writerFor(JsonNode.class)
                                .with(csvSchema).writeValueAsString(jsonTree);
        return value;
    }

}
