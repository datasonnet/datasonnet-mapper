package com.datasonnet.portx;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import sjsonnet.Materializer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CSVReader {
    public static String readCSV(String input) throws IOException {
        return readCSV(input, true, null, null, null, null);
    }

    public static String readCSV(String input, boolean useHeader, String quote, String separator, String escape, String newLine) throws IOException {

        ObjectMapper mapper = new ObjectMapper();

        CsvSchema.Builder builder = CsvSchema.builder().setUseHeader(useHeader);

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

        if (!useHeader) {
            csvMapper.enable(CsvParser.Feature.WRAP_AS_ARRAY);
        }

        // Read data from CSV file
//            List readAll = useHeader ? csvMapper.readerFor(Map.class).with(csvSchema).readValues(input).readAll() :
//                                       csvMapper.readerFor(Object[].class).with(csvSchema).readValues(input).readAll();
        List readAll = useHeader ? csvMapper.readerFor(Map.class).with(csvSchema).readValues(input).readAll() :
                csvMapper.readerFor(List.class).with(csvSchema).readValues(input).readAll();
        String jsonStr = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(readAll);

        return jsonStr;

    }

}
