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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import ujson.Value;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;

public class DefaultYamlFormatPlugin extends BaseJacksonDataFormatPlugin {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper();
    private static final YAMLFactory DEFAULT_YAML_FACTORY = new YAMLFactory();

    public static final String DS_PARAM_YAML_HEADER = "removehead";
    // this may break some things like reading 3.0.0 as a number,
    // would need to specify this in docs
    public static final String DS_PARAM_DISABLE_QUOTES = "disablequotes";

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
        writerParams.add(DS_PARAM_DISABLE_QUOTES);

    }

    @Override
    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        try {
            YAMLParser yamlParser = DEFAULT_YAML_FACTORY.createParser((String) doc.getContent());
            List<JsonNode> docs = DEFAULT_OBJECT_MAPPER.readValues(yamlParser, new TypeReference<JsonNode>() {}).readAll();

            if(docs.size()<=1){ //if only one node, only one object so dont return the list
                return ujsonFrom(DEFAULT_OBJECT_MAPPER.valueToTree(docs.get(0)));
            }
            return ujsonFrom(DEFAULT_OBJECT_MAPPER.valueToTree(docs));
        } catch (IOException e) {
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
            ObjectMapper yamlMapper = new ObjectMapper(DEFAULT_YAML_FACTORY);
            StringBuilder value = null;

            //if instance of list, it is multiple docs in one.
            if(inputAsJava instanceof List){
                List<Object> listInputAsJava = (List<Object>) inputAsJava;
                value = new StringBuilder();
                for(Object obj : listInputAsJava){
                    value.append(yamlMapper.writeValueAsString(obj));
                }
            }else{ //single document
                //remove the beginning '---' if specified
                //only available for single docs
                if(mediaType.getParameters().containsKey(DS_PARAM_YAML_HEADER)){
                    value = new StringBuilder(yamlMapper.writeValueAsString(inputAsJava).replaceFirst("---(\\n| )", ""));
                }else{
                    value = new StringBuilder(yamlMapper.writeValueAsString(inputAsJava));
                }
            }

            String output = value.toString();
            if(mediaType.getParameters().containsKey(DS_PARAM_DISABLE_QUOTES)){
                output = output.replaceAll("\"","");
            }

            if (targetType.isAssignableFrom(String.class)) {
                return new DefaultDocument<>((T) output, MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(CharSequence.class)) {
                return new DefaultDocument<>((T) output, MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(ByteBuffer.class)) {
                return new DefaultDocument<>((T) ByteBuffer.wrap(output.getBytes(charset)), MediaTypes.APPLICATION_YAML);
            }

            if (targetType.isAssignableFrom(byte[].class)) {
                return new DefaultDocument<>((T) output.getBytes(charset), MediaTypes.APPLICATION_YAML);
            }

            throw new PluginException("Unable to parse to target type.");
        } catch (JsonProcessingException e) {
            throw new PluginException("Failed to write yaml data");
        }
    }

}
