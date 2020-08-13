package com.datasonnet.plugins;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.AbstractDataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import ujson.Value;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DefaultJSONFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultJSONFormatPlugin() {
        WRITER_PARAMS.add(DS_PARAM_INDENT);

        READER_SUPPORTED_CLASSES.add(java.lang.String.class);
        READER_SUPPORTED_CLASSES.add(java.lang.CharSequence.class);
        READER_SUPPORTED_CLASSES.add(java.nio.file.Path.class);
        READER_SUPPORTED_CLASSES.add(java.io.File.class);
        READER_SUPPORTED_CLASSES.add(java.nio.ByteBuffer.class);
        READER_SUPPORTED_CLASSES.add(byte[].class);

        WRITER_SUPPORTED_CLASSES.add(java.lang.String.class);
        WRITER_SUPPORTED_CLASSES.add(java.lang.CharSequence.class);
        WRITER_SUPPORTED_CLASSES.add(java.nio.ByteBuffer.class);
        WRITER_SUPPORTED_CLASSES.add(java.io.OutputStream.class);
        WRITER_SUPPORTED_CLASSES.add(byte[].class);
    }

    @Override
    public Set<MediaType> supportedTypes() {
        Set<MediaType> set = new HashSet<>();
        set.add(MediaTypes.APPLICATION_JSON);
        set.add(new MediaType("application", "*+json"));
        return set;
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        Class<?> targetType = doc.getContent().getClass();

        if (String.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromString((String) doc.getContent()), false);
        }

        if (CharSequence.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromCharSequence((CharSequence) doc.getContent()), false);
        }

        if (Path.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromPath((Path) doc.getContent()), false);
        }

        if (File.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read((ujson.Readable) ujson.Readable.fromFile((File) doc.getContent()), false);
        }

        if (ByteBuffer.class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteBuffer((ByteBuffer) doc.getContent()), false);
        }

        if (byte[].class.isAssignableFrom(targetType)) {
            return ujsonUtils.read(ujson.Readable.fromByteArray((byte[]) doc.getContent()), false);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }
        
        int indent = mediaType.getParameters().containsKey(DS_PARAM_INDENT) ? 4 : -1;

        if (OutputStream.class.equals(targetType)) {
            BufferedOutputStream out = new BufferedOutputStream(new ByteArrayOutputStream());
            ujsonUtils.writeTo(input, new OutputStreamWriter(out, charset), indent, false);

            return new DefaultDocument<>((T) out, MediaTypes.APPLICATION_JSON);
        }

        String result = ujsonUtils.write(input, indent, false);

        if (String.class.equals(targetType)) {
            return new DefaultDocument<>((T) result, MediaTypes.APPLICATION_JSON);
        }

        if (CharSequence.class.equals(targetType)) {
            return new DefaultDocument<>((T) result, MediaTypes.APPLICATION_JSON);
        }

        if (ByteBuffer.class.equals(targetType)) {
            return new DefaultDocument<>((T) ByteBuffer.wrap(result.getBytes(charset)), MediaTypes.APPLICATION_JSON);
        }

        if (byte[].class.equals(targetType)) {
            return new DefaultDocument<>((T) result.getBytes(charset), MediaTypes.APPLICATION_JSON);
        }

        throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
    }
}
