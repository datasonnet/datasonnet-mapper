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
import ujson.Value;

public interface DataFormatPlugin {

    default boolean canRead(Document<?> doc) {
        return false;
    }

    default boolean canWrite(MediaType mediaType, Class<?> clazz) {
        return false;
    }

    ujson.Value read(Document<?> doc) throws PluginException;

    <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException;
}
