package com.datasonnet.plugins;

import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import ujson.Str;
import ujson.Value;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlainTextFormatPlugin implements DataFormatPlugin {
    public PlainTextFormatPlugin() { }

    @Override
    public Value read(Object input, Map<String, Object> params) {
        return UjsonUtil.stringValueOf(input.toString());
    }

    @Override
    public StringDocument write(Value input, Map<String, Object> params, String mimeType) {
        if(input instanceof Str) {
            return new StringDocument(UjsonUtil.stringValueTo((Str) input), mimeType);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }

    @Override
    public String[] getSupportedIdentifiers() {
        return new String[] { "text/plain", "txt" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return Collections.emptyMap();
    }

    @Override
    public String getPluginId() {
        return "Text";
    }
}
