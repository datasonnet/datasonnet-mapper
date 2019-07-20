package com.datasonnet;

import com.datasonnet.wrap.Mapper;

import java.util.HashMap;

public class Example {
    public static void main(String[] args) {
        String json = "{ \"user_id\": 7 }";
        String jsonnet = "function(payload) { \"uid\": payload.user_id }";
        String result = transform(json, jsonnet);
        System.out.println(result);
    }

    private static String transform(String json, String jsonnet) {
        Mapper mapper = new Mapper(jsonnet, new HashMap<String, String>());
        return mapper.transform(json);
    }
}
