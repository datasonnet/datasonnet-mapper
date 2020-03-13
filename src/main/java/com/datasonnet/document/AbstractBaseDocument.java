package com.datasonnet.document;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

public abstract class AbstractBaseDocument implements Document {

    @Override
    public boolean canGetContentsAs(Class klass) {
        return false;
    }

    @Override
    public Object getContentsAs(Class klass) {
        throw new UnsupportedOperationException("Cannot get contents");
    }

    @Override
    public String getContents() {
        return (String) getContentsAs(String.class);
    }
}
