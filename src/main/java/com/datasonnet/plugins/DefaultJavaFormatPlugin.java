package com.datasonnet.plugins;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.plugins.jackson.JAXBElementMixIn;
import com.datasonnet.plugins.jackson.JAXBElementSerializer;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ujson.Value;

import javax.xml.bind.JAXBElement;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DefaultJavaFormatPlugin extends BaseJacksonDataFormatPlugin {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    public static final String DEFAULT_DS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DS_PARAM_DATE_FORMAT = "dateformat";

    private static final Map<Integer, ObjectMapper> MAPPER_CACHE = new HashMap<>(4);

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(JAXBElement.class, new JAXBElementSerializer());
        DEFAULT_OBJECT_MAPPER.registerModule(module);
        DEFAULT_OBJECT_MAPPER.addMixIn(JAXBElement.class, JAXBElementMixIn.class);
        DEFAULT_OBJECT_MAPPER.setDateFormat(new SimpleDateFormat(DEFAULT_DS_DATE_FORMAT));
    }

    public DefaultJavaFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JAVA);

        readerParams.add(DS_PARAM_DATE_FORMAT);
        writerParams.add(DS_PARAM_DATE_FORMAT);
    }

    @Override
    protected boolean canReadClass(Class<?> cls) {
        return true;
    }

    @Override
    protected boolean canWriteClass(Class<?> clazz) {
        return true;
    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Value.Null();
        }

        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER;

        if (doc.getMediaType().getParameters().containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = doc.getMediaType().getParameter(DS_PARAM_DATE_FORMAT);
            int cacheKey = dateFormat.hashCode();
            mapper = MAPPER_CACHE.computeIfAbsent(cacheKey,
                    integer -> new ObjectMapper().setDateFormat(new SimpleDateFormat(dateFormat)));
        }

        JsonNode inputAsNode = mapper.valueToTree(doc.getContent());
        return ujsonFrom(inputAsNode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER;

        if (mediaType.getParameters().containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = mediaType.getParameter(DS_PARAM_DATE_FORMAT);
            int cacheKey = dateFormat.hashCode();
            mapper = MAPPER_CACHE.computeIfAbsent(cacheKey,
                    integer -> new ObjectMapper().setDateFormat(new SimpleDateFormat(dateFormat)));
        }

        try {
            Object inputAsJava = ujsonUtils.javaObjectFrom(input);
            T converted;

            if (Object.class.equals(targetType)) {
                converted = (T) inputAsJava;
            } else {
                converted = mapper.convertValue(inputAsJava, targetType);
            }

            return new DefaultDocument<>(converted);
        } catch (IllegalArgumentException e) {
            throw new PluginException("Unable to convert to target type", e);
        }
    }
}
