package com.datasonnet.plugins;

/*-
 * Copyright 2019-2021 the original author or authors.
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
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import java.util.*;



public class DefaultJavaFormatPlugin extends BaseJacksonDataFormatPlugin {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    public static final String DEFAULT_DS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DS_PARAM_DATE_FORMAT = "dateformat";
    public static final String DS_PARAM_TYPE = "type";  // aligns with existing java object mimetypes
    public static final String DS_PARAM_OUTPUT_CLASS = "outputclass";  // supports legacy
    public static final String DS_PARAM_MIXINS = "mixins";
    public static final String DS_PARAM_POLYMORPHIC_TYPES = "polymorphictypes";
    public static final String DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY = "polymorphictypeidproperty";

    private static final Map<String, ObjectMapper> MAPPER_CACHE = new RecentsMap<>(64);

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
        supportedTypes.add(MediaType.parseMediaType("application/java"));

        readerParams.add(DS_PARAM_DATE_FORMAT);
        readerParams.add(DS_PARAM_TYPE);
        readerParams.add(DS_PARAM_OUTPUT_CLASS);
        writerParams.addAll(readerParams);
        writerParams.add(DS_PARAM_MIXINS);
        writerParams.add(DS_PARAM_POLYMORPHIC_TYPES);
        writerParams.add(DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY);
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

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        T converted = writeValue(input, mediaType, targetType);
        return new DefaultDocument<>(converted, mediaType);
    }

    @Nullable
    private <T> T writeValue(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        if (input == ujson.Null$.MODULE$) {
            return null;
        }

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
            if (targetType.isAssignableFrom(inputAsJava.getClass())) {
                return (T) inputAsJava;
            } else {
                return mapper.convertValue(inputAsJava, targetType);
            }

        } catch (IllegalArgumentException e) {
            throw new PluginException("Unable to convert to target type", e);
        }
    }


    private ObjectMapper adaptObjectMapper(Map<String, String> parameters) {
        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER.copy();

        if (parameters.containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = parameters.get(DS_PARAM_DATE_FORMAT);
            mapper.setDateFormat(makeDateFormat(dateFormat));
        }

        if (parameters.containsKey(DS_PARAM_MIXINS)) {
            try {
                Map<String, String> mixinsMap = mapper.readValue(parameters.get(DS_PARAM_MIXINS), Map.class);
                for (Map.Entry<String,String> entry : mixinsMap.entrySet()) {
                    mapper.addMixIn(Class.forName(entry.getKey()), Class.forName(entry.getValue()));
                }
            } catch (JsonProcessingException jpe) {
                throw new PluginException("Invalid 'mixins' header format, must be JSON object", jpe);
            } catch (ClassNotFoundException cnfe) {
                throw new PluginException("Unable to add mixin", cnfe);
            }
        }

        if (parameters.containsKey(DS_PARAM_POLYMORPHIC_TYPES)) {
            final Set<String> polymorphicTypes = new HashSet<>(Arrays.asList(parameters.get(DS_PARAM_POLYMORPHIC_TYPES).trim().split("\\s*,\\s*")));

            // we use the deprecated constructor that is "unsafe" here, but safety is preserved
            // because of the useForType check
            ObjectMapper.DefaultTypeResolverBuilder resolver = new ObjectMapper.DefaultTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL) {
                @Override
                public boolean useForType(JavaType t) {
                    // only use our new resolver when it is one of the indicated types
                    return polymorphicTypes.contains(t.getRawClass().getTypeName());
                }
            };

            // require fully identified type name
            resolver.init(JsonTypeInfo.Id.CLASS, null);

            // require it be a property value
            resolver.inclusion(JsonTypeInfo.As.PROPERTY);

            // determine which property value
            if (parameters.containsKey(DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY)) {
                resolver.typeProperty(parameters.get(DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY));
            } else {
                resolver.typeProperty("@class");  // already default, but be explicit
            }
            mapper.setDefaultTyping(resolver);
        }

        return mapper;
    }

    private ObjectMapper getObjectMapper(MediaType mediaType) throws PluginException {
        Map<String, String> parameters = mediaType.getParameters();

        // for these keys we adapt the object mapper some, but we can keep reusing that every time the same collection
        // of parameters comes up
        // We have this check instead of just caching on every parameter combo
        // because most parameters do not require object mapper changes
        if (parameters.containsKey(DS_PARAM_DATE_FORMAT) || parameters.containsKey(DS_PARAM_MIXINS) || parameters.containsKey(DS_PARAM_POLYMORPHIC_TYPES)) {
            String key = mediaType.toString();
            return MAPPER_CACHE.computeIfAbsent(key, k -> {
                return adaptObjectMapper(parameters);
            });
        }

        return DEFAULT_OBJECT_MAPPER;
    }

    private String getJavaType(MediaType mediaType) {
        if(mediaType.getParameters().containsKey(DS_PARAM_TYPE)) {
            return mediaType.getParameter(DS_PARAM_TYPE);
        } else {
            return mediaType.getParameter(DS_PARAM_OUTPUT_CLASS);
        }
    }
}
