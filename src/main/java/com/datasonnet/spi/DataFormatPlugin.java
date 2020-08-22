package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import ujson.Value;

import java.util.Collections;
import java.util.Set;

public interface DataFormatPlugin {

    default boolean canRead(Document<?> doc) {
        return false;
    }

    default boolean canWrite(MediaType mediaType, Class<?> clazz) {
        return false;
    }

    ujson.Value read(Document<?> doc) throws PluginException;

    <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException;
}
