package com.datasonnet;

public class JsonPath {

    public static String select(String json, String path) {
        String selected = com.jayway.jsonpath.JsonPath.read(json, path).toString();
        return selected;
    }
}
