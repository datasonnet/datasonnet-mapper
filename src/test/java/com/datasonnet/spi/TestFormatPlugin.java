package com.datasonnet.spi;

import ujson.Value;

import java.util.HashMap;
import java.util.Map;

public class TestFormatPlugin implements DataFormatPlugin {

    public static String TEST_PARAM = "TestParam";

    @Override
    public Value read(String input, Map<String, Object> params) throws Exception {
        return UjsonUtil.stringValueOf("In 'read' Test Param Is " + params.get(TEST_PARAM));
    }

    @Override
    public String write(Value input, Map<String, Object> params) throws Exception {
        return "In 'write' Test Param Is " + params.get(TEST_PARAM);
    }

    @Override
    public String[] getSupportedIdentifiers() {
        return new String[] { "application/test.test", "test" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        Map<String, String> readParams = new HashMap<>();
        readParams.put(TEST_PARAM, "TestParameter");
        return readParams;
    }

    @Override
    public Map<String, String> getWriteParameters() {
        return getReadParameters();
    }

    public String getPluginId() {
        return "TEST";
    }
}
