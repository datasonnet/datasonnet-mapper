package com.datasonnet.document;

public class JavaObjectDocument implements Document<Object> {

    private Object contents;

    public JavaObjectDocument(Object contents) {
        this.contents = contents;
    }

    @Override
    public Object getContents() {
        return contents;
    }

    @Override
    public String getMimeType() {
        return "application/java";
    }

    public boolean equals(JavaObjectDocument document) {
        return (contents.equals(document.getContents()) && "application/java".equalsIgnoreCase(document.getMimeType()));
    }
}
