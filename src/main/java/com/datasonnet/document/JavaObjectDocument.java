package com.datasonnet.document;

public class JavaObjectDocument extends AbstractBaseDocument {

    private final Object contents;

    public JavaObjectDocument(Object contents) {
        this.contents = contents;
    }

    @Override
    public boolean canGetContentsAs(Class klass) {
        if (this.contents != null) {
            if (klass.isAssignableFrom(this.contents.getClass())) {
                return true;
            } else {
                return super.canGetContentsAs(klass);
            }
        }

        return true;
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
