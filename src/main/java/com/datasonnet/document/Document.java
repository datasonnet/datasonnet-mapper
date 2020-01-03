package com.datasonnet.document;

public interface Document<T> {
    public T getContents();
    public String getMimeType();
}
