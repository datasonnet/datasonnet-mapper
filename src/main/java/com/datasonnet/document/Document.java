package com.datasonnet.document;

public interface Document {
    public boolean canGetContentsAs(Class klass);

    public Object getContentsAs(Class klass);

    public String getContents();

    public String getMimeType();
}
