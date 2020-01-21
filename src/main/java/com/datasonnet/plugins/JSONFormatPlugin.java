package com.datasonnet.plugins;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.UjsonUtil;
import ujson.Str;
import ujson.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class JSONFormatPlugin implements DataFormatPlugin {
    public JSONFormatPlugin() { }

    @Override
    public Value read(Object input, Map<String, Object> params) {
        return UjsonUtil.jsonObjectValueOf(input.toString());
    }

    @Override
    public Document write(Value input, Map<String, Object> params, String mimeType) {
        return new StringDocument(UjsonUtil.jsonObjectValueTo(input), mimeType);
    }

    @Override
    public String[] getSupportedIdentifiers() {
        return new String[] { "application/json", "json" };
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
        return "JSON";
    }
}
