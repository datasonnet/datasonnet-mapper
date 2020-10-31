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
import com.datasonnet.document.MediaTypes;
import ujson.Value;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractDataFormatPlugin implements DataFormatPlugin {
    public static final String DS_PARAM_INDENT = "indent";

    protected final Set<MediaType> supportedTypes = new LinkedHashSet<>(4);
    protected final Set<String> readerParams = new LinkedHashSet<>();
    protected final Set<String> writerParams = new LinkedHashSet<>();
    protected final Set<Class<?>> readerSupportedClasses = new LinkedHashSet<>(8);
    protected final Set<Class<?>> writerSupportedClasses = new LinkedHashSet<>(8);

    @Override
    public Value read(Document<?> doc) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public boolean canRead(Document<?> doc) {
        MediaType requestedType = doc.getMediaType();
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, readerParams) &&
                    // TODO: 9/2/20 write tests for null handling
                    (doc.getContent() == null || canReadClass(doc.getContent().getClass()))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(MediaType requestedType, Class<?> clazz) {
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, writerParams) &&
                    canWriteClass(clazz)) {
                return true;
            }
        }

        return false;
    }

    protected boolean canReadClass(Class<?> cls) {
        for (Class<?> supported : readerSupportedClasses) {
            if (supported.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canWriteClass(Class<?> clazz) {
        for (Class<?> supported : writerSupportedClasses) {
            if (clazz.isAssignableFrom(supported)) {
                return true;
            }
        }
        return false;
    }

    private boolean parametersAreSupported(MediaType requestedType, Set<String> supported) {
        for (String param : requestedType.getParameters().keySet()) {
            if (!(MediaTypes.PARAM_QUALITY_FACTOR.equals(param) || MediaTypes.PARAM_CHARSET.equals(param))) {
                // if it's not known params q or charset, or a general prefix, and it's not supported param, we fail
                boolean matched = false;
                for (String supportedParam : supported) {
                    if (param.matches(supportedParam)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }

        return true;
    }
}
