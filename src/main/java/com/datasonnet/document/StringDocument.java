package com.datasonnet.document;

public class StringDocument extends AbstractBaseDocument {

    private String contents;
    private String mimeType;

    public StringDocument(String contents, String mimeType) {
        this.contents = contents;
        this.mimeType = mimeType;
    }

    @Override
    public boolean canGetContentsAs(Class klass) {
        if(String.class.equals(klass)) {
            return true;
        }
        return super.canGetContentsAs(klass);
    }

    @Override
    public Object getContentsAs(Class klass) {
        if(canGetContentsAs(klass)) {
            return contents;
        }
        return super.getContentsAs(klass);
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

}
