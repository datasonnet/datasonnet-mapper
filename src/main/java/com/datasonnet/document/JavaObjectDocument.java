package com.datasonnet.document;

public class JavaObjectDocument extends AbstractBaseDocument {

    private Object contents;

    public JavaObjectDocument(Object contents) {
        this.contents = contents;
    }

    @Override
    public boolean canGetContentsAs(Class klass) {
        if(Object.class.equals(klass)) {
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
        return "application/java";
    }
}
