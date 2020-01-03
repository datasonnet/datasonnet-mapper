package com.datasonnet.spi;

import com.datasonnet.document.Document;

import java.util.Map;

public interface DataFormatPlugin {
    ujson.Value read(Object input, Map<String, Object> params) throws Exception;
    Document write(ujson.Value input, Map<String, Object> params, String mimeType) throws Exception;

    String[] getSupportedIdentifiers();

    Map<String, String> getReadParameters();
    Map<String, String> getWriteParameters();

    String getPluginId();
}
