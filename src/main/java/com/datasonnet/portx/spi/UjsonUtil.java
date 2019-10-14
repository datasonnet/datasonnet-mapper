package com.datasonnet.portx.spi;

import ujson.Str;
import ujson.Value;

public class UjsonUtil {
    public static Value stringValueOf(String str) {
        return new Str(str);
    }

    public static Value jsonObjectValueOf(String jsonData) {
        return ujson.package$.MODULE$.read(ujson.Readable.fromString(jsonData));
    }

    public static String jsonObjectValueTo(Value value) {
        return value.toString();
    }

    public static String stringValueTo(Str value) {
        return value.str();
    }
}
