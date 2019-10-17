package com.datasonnet.portx.spi;

import java.io.IOException;
import java.util.Map;

public interface DataFormatPlugin {
    public ujson.Value read(Object input, Map<String, Object> params) throws Exception;
    public String write(ujson.Value input, Map<String, Object> params) throws Exception;

    public String[] getSupportedMimeTypes();

    public Map<String, String> getReadParameters();
    public Map<String, String> getWriteParameters();

    public String getPluginId();
}
