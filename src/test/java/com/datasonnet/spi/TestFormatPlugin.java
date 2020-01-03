package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.document.StringDocument;
import ujson.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestFormatPlugin implements DataFormatPlugin {

    public static String TEST_PARAM = "TestParam";

    @Override
    public Value read(Object input, Map<String, Object> params) throws Exception {
        return UjsonUtil.stringValueOf("In 'read' Test Param Is " + params.get(TEST_PARAM));
    }

    @Override
    public Document write(Value input, Map<String, Object> params, String mimeType) throws Exception {
        return new StringDocument("In 'write' Test Param Is " + params.get(TEST_PARAM), mimeType);
    }

    @Override
    public String[] getSupportedIdentifiers() {
        return new String[] { "application/test.test", "test" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        return Collections.singletonMap(TEST_PARAM, "TestParameter");
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return getReadParameters();
    }

    public String getPluginId() {
        return "TEST";
    }
}
