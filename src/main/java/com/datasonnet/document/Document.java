package com.datasonnet.document;

public interface Document<T> {
    T getContent();

    MediaType getMediaType();

}
