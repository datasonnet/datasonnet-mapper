package com.datasonnet.spi;

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

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.plugins.DefaultCSVFormatPlugin;
import com.datasonnet.plugins.DefaultJSONFormatPlugin;
import com.datasonnet.plugins.DefaultJavaFormatPlugin;
import com.datasonnet.plugins.DefaultPlainTextFormatPlugin;
import com.datasonnet.plugins.DefaultXMLFormatPlugin$;
import com.datasonnet.plugins.DefaultYamlFormatPlugin;
import ujson.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DataFormatService {
    private final List<DataFormatPlugin> plugins;
    public static final DataFormatService DEFAULT =
            new DataFormatService(Arrays.asList(
                    new DefaultJSONFormatPlugin(),
                    new DefaultJavaFormatPlugin(),
                    DefaultXMLFormatPlugin$.MODULE$,
                    new DefaultCSVFormatPlugin(),
                    new DefaultPlainTextFormatPlugin(),
                    new DefaultYamlFormatPlugin()));

    public DataFormatService(List<DataFormatPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<DataFormatPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public Optional<DataFormatPlugin> thatCanWrite(MediaType output, Class<?> target) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canWrite(output, target)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public Optional<DataFormatPlugin> thatCanRead(Document<?> doc) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canRead(doc)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public <T> Document<T> mandatoryWrite(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        return thatCanWrite(mediaType, targetType)
                .orElseThrow(() -> new IllegalArgumentException("The output MediaType " + mediaType + " is not supported for class" + targetType))
                .write(input, mediaType, targetType);
    }

    public ujson.Value mandatoryRead(Document<?> doc) throws PluginException {
        return thatCanRead(doc)
                .orElseThrow(() -> new IllegalArgumentException("The input MediaType " + doc.getMediaType() + " is not supported for class" + doc.getContent().getClass()))
                .read(doc);
    }
}
