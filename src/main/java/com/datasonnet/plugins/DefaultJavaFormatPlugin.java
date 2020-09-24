package com.datasonnet.plugins;

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import ujson.Value;

import javax.xml.bind.JAXBElement;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

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
        // TODO: 9/8/20 add test for empty beans
        DEFAULT_OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
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
            return ujson.Null$.MODULE$;
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
