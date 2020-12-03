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
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import ujson.Value;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class DefaultYamlFormatPlugin extends BaseJacksonDataFormatPlugin {

    public static final String DS_PARAM_YAML_HEADER = "removehead";

    public DefaultYamlFormatPlugin(){
        supportedTypes.add(MediaTypes.APPLICATION_YAML);

        readerSupportedClasses.add(java.lang.String.class);
        readerSupportedClasses.add(java.lang.CharSequence.class);
        readerSupportedClasses.add(java.nio.ByteBuffer.class);
        readerSupportedClasses.add(byte[].class);

        writerSupportedClasses.add(java.lang.String.class);
        writerSupportedClasses.add(java.lang.CharSequence.class);
        writerSupportedClasses.add(java.nio.ByteBuffer.class);
        writerSupportedClasses.add(byte[].class);

        readerParams.add(DS_PARAM_YAML_HEADER);
        writerParams.addAll(readerParams);

    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        try {
            ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
            Object obj = yamlReader.readValue((String) doc.getContent(), Object.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode inputAsNode = mapper.valueToTree(obj);
            return ujsonFrom(inputAsNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new PluginException("Failed to read yaml data");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        Charset charset = mediaType.getCharset();
        if (charset == null) {
            charset = Charset.defaultCharset();
        }

        try {
            Object inputAsJava = ujsonUtils.javaObjectFrom(input);
            ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

            String value = yamlMapper.writeValueAsString(inputAsJava);
            //remove the begining '---' if specified
            if(mediaType.getParameters().containsKey(DS_PARAM_YAML_HEADER)){
                value = value.replaceFirst("---(\\n| )", "");
            }

            if (targetType.isAssignableFrom(String.class)) {
                return new DefaultDocument<>((T) value, MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(CharSequence.class)) {
                return new DefaultDocument<>((T) value, MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(ByteBuffer.class)) {
                return new DefaultDocument<>((T) ByteBuffer.wrap(value.getBytes(charset)), MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return new DefaultDocument<>((T) value.getBytes(charset), MediaTypes.APPLICATION_YAML);
            }

            throw new PluginException("Unable to parse to target type.");
        } catch (JsonProcessingException e) {
            throw new PluginException("Failed to write yaml data");
        }
    }

}
