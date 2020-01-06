package com.datasonnet.plugins;

import com.datasonnet.document.Document;
import com.datasonnet.document.JavaObjectDocument;
import com.datasonnet.document.StringDocument;
import com.datasonnet.spi.DataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.UjsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ujson.Value;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaFormatPlugin implements DataFormatPlugin {
    public static String OUTPUT_CLASS = "OutputClass";
    public static String DATE_FORMAT = "DateFormat";

    public static String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    public JavaFormatPlugin() { }

    public Value read(Object input, Map<String, Object> params) throws PluginException {
        ObjectMapper mapper = new ObjectMapper();
        DateFormat df = new SimpleDateFormat(params.containsKey(DATE_FORMAT) ? (String)params.get(DATE_FORMAT) : DEFAULT_DATE_FORMAT);
        mapper.setDateFormat(df);
        try {
            String jsonStr = mapper.writeValueAsString(input);
            return UjsonUtil.jsonObjectValueOf(jsonStr);
        } catch (JsonProcessingException e) {
            throw new PluginException(e);
        }
    }

    public Document write(Value input, Map<String, Object> params, String mimeType) throws PluginException {
        try {
            String jsonString = UjsonUtil.jsonObjectValueTo(input);
            ObjectMapper mapper = new ObjectMapper();
            DateFormat df = new SimpleDateFormat(params.containsKey(DATE_FORMAT) ? (String)params.get(DATE_FORMAT) : DEFAULT_DATE_FORMAT);
            mapper.setDateFormat(df);
            JsonNode node = mapper.readTree(jsonString);

            JavaType valueType = null;

            if (node.isObject()) {
                if (params != null && params.containsKey(OUTPUT_CLASS)) {
                    valueType = mapper.getTypeFactory().constructFromCanonical(params.get(OUTPUT_CLASS).toString());
                } else {
                    valueType = mapper.getTypeFactory().constructRawMapType(HashMap.class);
                }
            } else if (node.isArray()) {
                valueType = mapper.getTypeFactory().constructRawCollectionType(List.class);
            } else if (node.isTextual()) {
                valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.String");
            } else if (node.isBoolean()) {
                valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.Boolean");
            } else if (node.isNumber()) {
                valueType = mapper.getTypeFactory().constructFromCanonical("java.lang.Number");
            }

            return new JavaObjectDocument(mapper.readValue(jsonString, valueType));
        } catch (Exception e) {
            throw new PluginException(e);
        }
    }

    public String[] getSupportedIdentifiers() {
        return new String[] { "application/java", "java" };
    }

    @Override
    public Map<String, String> getReadParameters() {
        Map<String, String> readParams = new HashMap<>();
        readParams.put(DATE_FORMAT, "Controls the date format for serializing/deserializing");
        return readParams;
    }

    @Override
    public Map<String, String> getWriteParameters() {
        Map<String, String> writeParams = new HashMap<>();
        writeParams.put(OUTPUT_CLASS, "Fully qualified class name of the output");
        writeParams.put(DATE_FORMAT, "Controls the date format for serializing/deserializing");
        return writeParams;
    }

    public String getPluginId() {
        return "Java";
    }
}
