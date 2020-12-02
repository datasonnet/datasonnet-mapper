package com.datasonnet.document;

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

/***
 * The DefaultDocument class
 *
 * @param <T> the content
 */
public class DefaultDocument<T> implements Document<T> {
    private final T content;
    private final MediaType mediaType;

    public final static Document<Object> NULL_INSTANCE = new DefaultDocument<>(null, MediaTypes.APPLICATION_JAVA);

    public DefaultDocument(T content) {
        this(content, null);
    }

    public DefaultDocument(T content, MediaType mediaType) {
        this.content = content;
        if (mediaType != null) {
            this.mediaType = mediaType;
        } else {
            this.mediaType = MediaTypes.UNKNOWN;
        }
    }

    @Override
    public Document<T> withMediaType(MediaType mediaType) {
        return new DefaultDocument<>(this.getContent(), mediaType);
    }

    @Override
    public T getContent() {
        return content;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }
}
