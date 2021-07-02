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
import com.datasonnet.spi.AbstractDataFormatPlugin;
import com.datasonnet.spi.PluginException;
import com.datasonnet.spi.ujsonUtils;
import ujson.Value;

public class DefaultPlainTextFormatPlugin extends AbstractDataFormatPlugin {
    public DefaultPlainTextFormatPlugin() {
        supportedTypes.add(MediaTypes.TEXT_PLAIN);

        readerSupportedClasses.add(String.class);
        writerSupportedClasses.add(String.class);
    }

    public Value read(Document<?> doc) throws PluginException {
        if (doc.getContent() == null) {
            return ujson.Null$.MODULE$;
        }

        if (String.class.isAssignableFrom(doc.getContent().getClass())) {
            return ujsonUtils.strValueOf((String) doc.getContent());
        } else {
            throw new PluginException(new IllegalArgumentException("Unsupported document content class, use the test method canRead before invoking read"));
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        if (targetType.isAssignableFrom(String.class)) {
            return (Document<T>) new DefaultDocument<>(ujsonUtils.stringValueOf(input), MediaTypes.TEXT_PLAIN);
        } else {
            throw new IllegalArgumentException("Only strings can be written as plain text.");
        }
    }
}
