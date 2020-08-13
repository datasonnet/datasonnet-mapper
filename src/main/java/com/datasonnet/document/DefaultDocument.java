package com.datasonnet.document;

public class DefaultDocument<T> implements Document<T> {
    private final T content;
    private final MediaType mediaType;

    public DefaultDocument(T content) {
        this.content = content;
        this.mediaType = MediaTypes.APPLICATION_JAVA;
    }

    public DefaultDocument(T content, MediaType mediaType) {
        this.content = content;
        this.mediaType = mediaType;
    }

    public DefaultDocument<T> withMediaType(MediaType mediaType) {
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
