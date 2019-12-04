package com.datasonnet;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlainTextFormatPlugin implements DataFormatPlugin {
    public PlainTextFormatPlugin() { }

    public Value read(String input, Map<String, Object> params) {
        return UjsonUtil.stringValueOf(input);
    }

    public String write(Value input, Map<String, Object> params) {
        if(input instanceof Str) {
            return UjsonUtil.stringValueTo((Str) input);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }

    public String[] getSupportedIdentifiers() {
        return new String[] { "text/plain", "txt" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        return new HashMap<>();
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return new HashMap<>();
    }

    public String getPluginId() {
        return "Text";
    }
}
