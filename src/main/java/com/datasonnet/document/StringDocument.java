package com.datasonnet.document;

public class StringDocument implements Document<String> {

    private String contents;
    private String mimeType;

    public StringDocument(String contents, String mimeType) {
        this.contents = contents;
        this.mimeType = mimeType;
    }

    @Override
    public String getContents() {
        return contents;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    public boolean equals(StringDocument document) {
        return (contents.equals(document.getContents()) && mimeType.equalsIgnoreCase(document.getMimeType()));
    }
}
