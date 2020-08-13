package com.datasonnet.plugins;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.AbstractDataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import ujson.Str;
import ujson.Value;

import java.util.Collections;
import java.util.Set;

public class DefaultPlainTextFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultPlainTextFormatPlugin() {
        READER_SUPPORTED_CLASSES.add(String.class);
        WRITER_SUPPORTED_CLASSES.add(String.class);
    }

    @Override
    public Set<MediaType> supportedTypes() {
        return Collections.singleton(MediaTypes.TEXT_PLAIN);
    }

    public Value read(Document<?> doc) throws PluginException {
        if (String.class.isAssignableFrom(doc.getContent().getClass())){
            return ujsonUtils.strOf((String) doc.getContent());
        } else {
            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        if (String.class.equals(targetType)) {
            return (Document<T>) new DefaultDocument<>(ujsonUtils.stringValueOf(input), MediaTypes.TEXT_PLAIN);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }
}
