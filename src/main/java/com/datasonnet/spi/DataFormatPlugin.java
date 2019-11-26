package com.datasonnet.spi;

import java.util.Map;

public interface DataFormatPlugin {
    ujson.Value read(String input, Map<String, Object> params) throws Exception;
    String write(ujson.Value input, Map<String, Object> params) throws Exception;

    String[] getSupportedMimeTypes();

    Map<String, String> getReadParameters();
    Map<String, String> getWriteParameters();

    String getPluginId();
}
