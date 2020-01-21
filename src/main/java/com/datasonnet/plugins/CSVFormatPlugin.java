package com.datasonnet.plugins;

import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ujson.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CSVFormatPlugin implements DataFormatPlugin {

    public static String USE_HEADER = "UseHeader";
    public static String QUOTE_CHAR = "Quote";
    public static String SEPARATOR_CHAR = "Separator";
    public static String ESCAPE_CHAR = "Escape";
    public static String NEW_LINE = "NewLine";
    public static String HEADERS = "Headers";

    public CSVFormatPlugin() {

    }

    @Override
    public Value read(Object input, Map<String, Object> params) throws PluginException {

        ObjectMapper mapper = new ObjectMapper();
        CsvSchema.Builder builder = this.getBuilder(params);

        boolean useHeader = params != null && params.get(USE_HEADER) != null ? new Boolean(params.get(USE_HEADER).toString()) : true;

        CsvSchema csvSchema = builder.build();
        CsvMapper csvMapper = new CsvMapper();

        csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        // Read data from CSV file
        try {
            List readAll = useHeader ? csvMapper.readerFor(Map.class).with(csvSchema).readValues(input.toString()).readAll() :
                    csvMapper.readerFor(List.class).with(csvSchema).readValues(input.toString()).readAll();
            String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(readAll);
            return UjsonUtil.jsonObjectValueOf(jsonStr);
        } catch (JsonProcessingException jpe) {
            throw new PluginException("Unable to convert CSV to JSON", jpe);
        } catch (IOException ioe) {
            throw new PluginException("Unable to read CSV input", ioe);
        }
    }

    @Override
    public StringDocument write(Value input, Map<String, Object> params, String mimeType) throws PluginException {
        CsvSchema.Builder builder = this.getBuilder(params);

        final JsonNode jsonTree;
        try {
            jsonTree = new ObjectMapper().readTree(UjsonUtil.jsonObjectValueTo(input));
        } catch (JsonProcessingException e) {
            throw new PluginException("Unable to read JSON Tree", e);
        }
        boolean useHeader = params != null && params.get(USE_HEADER) != null ? new Boolean(params.get(USE_HEADER).toString()) : true;

        if (useHeader) {
            if (params != null && params.containsKey(HEADERS)) {
                List<String> headers = (List)params.get(HEADERS);
                for (String header : headers) {
                    builder.addColumn(header);
                }
            } else {
                JsonNode firstObject = jsonTree.elements().next();
                firstObject.fieldNames().forEachRemaining(fieldName -> {
                    builder.addColumn(fieldName);
                });
            }
        }

        CsvSchema csvSchema = builder.build();

        CsvMapper csvMapper = new CsvMapper();

        try {
            final String value = csvMapper.writerFor(JsonNode.class)
                    .with(csvSchema).writeValueAsString(jsonTree);
            return new StringDocument(value, mimeType);
        } catch (JsonProcessingException e) {
            throw new PluginException("Unable to write CSV output", e);
        }
    }

    @Override
    public String[] getSupportedIdentifiers() {
        return new String[] { "application/csv", "text/csv", "csv" };
    }

    private CsvSchema.Builder getBuilder(Map<String, Object> params) {
        CsvSchema.Builder builder = CsvSchema.builder();

        boolean useHeader = params != null && params.get(USE_HEADER) != null ? new Boolean(params.get(USE_HEADER).toString()) : true;
        builder.setUseHeader(useHeader);

        if (params != null) {
            if (params.get(QUOTE_CHAR) != null) {
                builder.setQuoteChar(params.get(QUOTE_CHAR).toString().charAt(0));
            }
            if (params.get(SEPARATOR_CHAR) != null) {
                builder.setColumnSeparator(params.get(SEPARATOR_CHAR).toString().charAt(0));
            }
            if (params.get(ESCAPE_CHAR) != null) {
                builder.setEscapeChar(params.get(ESCAPE_CHAR).toString().charAt(0));
            }
            if (params.get(NEW_LINE) != null) {
                builder.setLineSeparator(params.get(NEW_LINE).toString());
            }
        }
        return builder;
    }

    @Override
    public Map<String, String> getReadParameters() {
        Map<String, String> readParams = new HashMap<>();
        readParams.put(USE_HEADER, "Set to \"true\" if the CSV first row has column names");
        readParams.put(QUOTE_CHAR, "CSV quote character");
        readParams.put(SEPARATOR_CHAR, "CSV separator character");
        readParams.put(ESCAPE_CHAR, "CSV escape character");
        readParams.put(NEW_LINE, "New line character");
        return Collections.unmodifiableMap(readParams);
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return getReadParameters();
    }

    @Override
    public String getPluginId() {
        return "CSV";
    }
}
