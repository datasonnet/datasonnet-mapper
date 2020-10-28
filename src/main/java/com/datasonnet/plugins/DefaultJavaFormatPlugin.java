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

import com.datasonnet.RecentsMap;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.plugins.jackson.JAXBElementMixIn;
import com.datasonnet.plugins.jackson.JAXBElementSerializer;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ujson.Value;

import javax.xml.bind.JAXBElement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;


public class DefaultJavaFormatPlugin extends BaseJacksonDataFormatPlugin {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    public static final String DEFAULT_DS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DS_PARAM_DATE_FORMAT = "dateformat";
    public static final String DS_PARAM_TYPE = "type";  // aligns with existing java object mimetypes
    public static final String DS_PARAM_OUTPUT_CLASS = "outputclass";  // supports legacy

    private static final Map<Integer, ObjectMapper> MAPPER_CACHE = new RecentsMap<>(64);

    static {
        SimpleModule module = new SimpleModule();
        module.addSerializer(JAXBElement.class, new JAXBElementSerializer());
        DEFAULT_OBJECT_MAPPER.registerModule(module);
        DEFAULT_OBJECT_MAPPER.addMixIn(JAXBElement.class, JAXBElementMixIn.class);
        // TODO: 9/8/20 add test for empty beans
        DEFAULT_OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        DEFAULT_OBJECT_MAPPER.setDateFormat(makeDateFormat(DEFAULT_DS_DATE_FORMAT));
    }

    @NotNull
    private static DateFormat makeDateFormat(String defaultDsDateFormat) {
        DateFormat format = new SimpleDateFormat(defaultDsDateFormat);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    public DefaultJavaFormatPlugin() {
        supportedTypes.add(MediaTypes.APPLICATION_JAVA);

        readerParams.add(DS_PARAM_DATE_FORMAT);
        readerParams.add(DS_PARAM_TYPE);
        readerParams.add(DS_PARAM_OUTPUT_CLASS);
        writerParams.addAll(readerParams);
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

        ObjectMapper mapper = getObjectMapper(doc.getMediaType());

        JsonNode inputAsNode = mapper.valueToTree(doc.getContent());
        return ujsonFrom(inputAsNode);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        T converted = writeValue(input, mediaType, targetType);
        return new DefaultDocument<>(converted);
    }

    @Nullable
    private <T> T writeValue(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        ObjectMapper mapper = getObjectMapper(mediaType);

        try {
            Object inputAsJava = ujsonUtils.javaObjectFrom(input);
            if(mediaType.getParameters().containsKey(DS_PARAM_TYPE) || mediaType.getParameters().containsKey(DS_PARAM_OUTPUT_CLASS)) {
                String typeName = getJavaType(mediaType);
                if (!"".equals(typeName)) {  // make it possible to opt out with a media type that blanks the param
                    JavaType javaType = mapper.getTypeFactory().constructFromCanonical(typeName);
                    // provide a requested subtype, if it's compatible with the type requested
                    if (javaType.isTypeOrSubTypeOf(targetType)) {
                        // we already have something that works
                        if(javaType.isTypeOrSuperTypeOf(inputAsJava.getClass())) {
                            return (T) inputAsJava;
                        } else {
                            return mapper.convertValue(inputAsJava, javaType);
                        }
                    }  // otherwise fall through to default behavior
                }
            }

            // fancier version of the Object.equals optimization
            if(targetType.isAssignableFrom(inputAsJava.getClass())) {
                return (T) inputAsJava;
            } else {
                return mapper.convertValue(inputAsJava, targetType);
            }

        } catch (IllegalArgumentException e) {
            throw new PluginException("Unable to convert to target type", e);
        }
    }

    private ObjectMapper getObjectMapper(MediaType mediaType) {
        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER;

        if (mediaType.getParameters().containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = mediaType.getParameter(DS_PARAM_DATE_FORMAT);
            int cacheKey = dateFormat.hashCode();
            mapper = MAPPER_CACHE.computeIfAbsent(cacheKey,
                    integer -> new ObjectMapper().setDateFormat(makeDateFormat(dateFormat)));
        }
        return mapper;
    }

    private String getJavaType(MediaType mediaType) {
        if(mediaType.getParameters().containsKey(DS_PARAM_TYPE)) {
            return mediaType.getParameter(DS_PARAM_TYPE);
        } else {
            return mediaType.getParameter(DS_PARAM_OUTPUT_CLASS);
        }
    }
}
