package com.datasonnet.portx;

import com.datasonnet.portx.spi.DataFormatPlugin;
import com.datasonnet.portx.spi.UjsonUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ujson.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JacksonAdapter {

    public static Value javaToJson(Object input) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String jsonStr = mapper.writeValueAsString(input);
        return UjsonUtil.jsonObjectValueOf(jsonStr);
    }

    public static Object jsonToJava(Value output) throws Exception {
        String jsonString = UjsonUtil.jsonObjectValueTo(output);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(jsonString);

        JavaType valueType = null;

        if (node.isObject()) {
            valueType = mapper.getTypeFactory().constructRawMapType(HashMap.class);
        } else if (node.isArray()) {
            valueType = mapper.getTypeFactory().constructRawCollectionType(List.class);
        } else if (node.isTextual()) {
            valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.String");
        } else if (node.isBoolean()) {
            valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.Boolean");
        } else if (node.isNumber()) {
            valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.Number");
        }

        return mapper.readValue(jsonString, valueType);

    }
}
