package com.datasonnet;

import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ujson.Value;

import java.io.IOException;
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

    public Value read(String input, Map<String, Object> params) throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        CsvSchema.Builder builder = this.getBuilder(params);

        boolean useHeader = params != null && params.get(USE_HEADER) != null ? (Boolean)params.get(USE_HEADER) : true;

        CsvSchema csvSchema = builder.build();
        CsvMapper csvMapper = new CsvMapper();

        csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);

        // Read data from CSV file
        List readAll = useHeader ? csvMapper.readerFor(Map.class).with(csvSchema).readValues(input).readAll() :
                csvMapper.readerFor(List.class).with(csvSchema).readValues(input).readAll();
        String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(readAll);

        return UjsonUtil.jsonObjectValueOf(jsonStr);
    }

    public String write(Value input, Map<String, Object> params) throws IOException {
        CsvSchema.Builder builder = this.getBuilder(params);

        JsonNode jsonTree = new ObjectMapper().readTree(UjsonUtil.jsonObjectValueTo(input));
        boolean useHeader = params != null && params.get(USE_HEADER) != null ? (Boolean)params.get(USE_HEADER) : true;

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
        String value = csvMapper.writerFor(JsonNode.class)
                .with(csvSchema).writeValueAsString(jsonTree);
        return value;
    }

    public String[] getSupportedIdentifiers() {
        return new String[] { "application/csv", "text/csv", "csv" };
    }

    private CsvSchema.Builder getBuilder(Map<String, Object> params) {
        CsvSchema.Builder builder = CsvSchema.builder();

        boolean useHeader = params != null && params.get(USE_HEADER) != null ? (Boolean)params.get(USE_HEADER) : true;
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
        return readParams;
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return getReadParameters();
    }

    public String getPluginId() {
        return "CSV";
    }
}
