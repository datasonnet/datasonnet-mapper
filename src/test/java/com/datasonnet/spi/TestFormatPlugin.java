package com.datasonnet.spi;

import ujson.Value;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestFormatPlugin implements DataFormatPlugin {

    public static String TEST_PARAM = "TestParam";

    @Override
    public Value read(String input, Map<String, Object> params) throws Exception {
        return UjsonUtil.stringValueOf(params.get(TEST_PARAM).toString());
    }

    @Override
    public String write(Value input, Map<String, Object> params) throws Exception {
        return params.get(TEST_PARAM).toString();
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
