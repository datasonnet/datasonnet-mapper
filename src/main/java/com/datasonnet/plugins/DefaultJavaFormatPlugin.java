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
import com.datasonnet.plugins.jackson.PolymorphicTypesMixIn;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.SimpleMixInResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.EnumMemberValue;
import javassist.bytecode.annotation.StringMemberValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ujson.Value;

import javax.xml.bind.JAXBElement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;



public class DefaultJavaFormatPlugin extends BaseJacksonDataFormatPlugin {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    public static final String DEFAULT_DS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String DS_PARAM_DATE_FORMAT = "dateformat";
    public static final String DS_PARAM_TYPE = "type";  // aligns with existing java object mimetypes
    public static final String DS_PARAM_OUTPUT_CLASS = "outputclass";  // supports legacy
    public static final String DS_PARAM_MIXINS = "mixins";
    public static final String DS_PARAM_POLYMORPHIC_TYPES = "polymorphictypes";
    public static final String DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY = "polymorphictypeidproperty";

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

    private ObjectMapper getObjectMapper(MediaType mediaType) throws PluginException {
        //I disabled the caching because it doesn't play well with manipulating mixins; instead, I clone the default mapper
/*
        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER;

        if (mediaType.getParameters().containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = mediaType.getParameter(DS_PARAM_DATE_FORMAT);
            int cacheKey = dateFormat.hashCode();
            mapper = MAPPER_CACHE.computeIfAbsent(cacheKey,
                    integer -> new ObjectMapper().setDateFormat(makeDateFormat(dateFormat)));
        }
*/
        ObjectMapper mapper = DEFAULT_OBJECT_MAPPER.copy();

        if (mediaType.getParameters().containsKey(DS_PARAM_DATE_FORMAT)) {
            String dateFormat = mediaType.getParameter(DS_PARAM_DATE_FORMAT);
            mapper.setDateFormat(makeDateFormat(dateFormat));
        }

        if (mediaType.getParameters().containsKey(DS_PARAM_MIXINS)) {
            try {
                Map<String, String> mixinsMap = mapper.readValue(mediaType.getParameter(DS_PARAM_MIXINS), Map.class);
                for (Map.Entry<String,String> entry : mixinsMap.entrySet()) {
                    mapper.addMixIn(Class.forName(entry.getKey()), Class.forName(entry.getValue()));
                }
            } catch (JsonProcessingException jpe) {
                throw new PluginException("Invalid 'mixins' header format, must be JSON object", jpe);
            } catch (ClassNotFoundException cnfpe) {
                throw new PluginException("Unable to add mixin", cnfpe);
            }
        }

        Class mixinClass = PolymorphicTypesMixIn.class;

        if (mediaType.getParameters().containsKey(DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY)) {
            try {
                ClassPool cp = ClassPool.getDefault();
                CtClass cc = cp.get("com.datasonnet.plugins.jackson.PolymorphicTypesMixIn");
                //cp.makePackage(cp.getClassLoader(), "com.datasonnet.plugins.jackson");
                ClassFile cfile = cc.getClassFile();
                ConstPool constPool = cfile.getConstPool();
                AnnotationsAttribute attr = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
                Annotation a = new Annotation("com.fasterxml.jackson.annotation.JsonTypeInfo", constPool);
                a.addMemberValue("property", new StringMemberValue(mediaType.getParameter(DS_PARAM_POLYMORPHIC_TYPE_ID_PROPERTY), constPool));
                EnumMemberValue jsonTypeInfoId = new EnumMemberValue(constPool);
                jsonTypeInfoId.setType(JsonTypeInfo.Id.class.getName());
                jsonTypeInfoId.setValue(JsonTypeInfo.Id.CLASS.name());
                a.addMemberValue("use", jsonTypeInfoId);
                EnumMemberValue jsonTypeInfoAs = new EnumMemberValue(constPool);
                jsonTypeInfoAs.setType(JsonTypeInfo.As.class.getName());
                jsonTypeInfoAs.setValue(com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY.name());
                a.addMemberValue("include", jsonTypeInfoAs);
                attr.setAnnotation(a);

                cfile.addAttribute(attr);
                String newClassName = "com.datasonnet.plugins.jackson.MixIn" + UUID.randomUUID().toString();
                cc.setName(newClassName);
                cfile.setVersionToJava5();

                mixinClass = cc.toClass();

            } catch (Exception e) {
                throw new PluginException("Unable to override polymorphic type property", e);
            }
        }

        if (mediaType.getParameters().containsKey(DS_PARAM_POLYMORPHIC_TYPES)) {
            try {
                String[] polymorphicTypes = mediaType.getParameter(DS_PARAM_POLYMORPHIC_TYPES).split(",");
                for (final String type : polymorphicTypes) {
                    mapper.addMixIn(Class.forName(type), mixinClass);
                }
            } catch (ClassNotFoundException cnfpe) {
                throw new PluginException("Polymorphic type cannot be resolved", cnfpe);
            }
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
