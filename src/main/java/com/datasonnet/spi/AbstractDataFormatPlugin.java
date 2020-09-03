package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import ujson.Value;

import java.util.LinkedHashSet;
import java.util.Set;

public abstract class AbstractDataFormatPlugin implements DataFormatPlugin {
    public static final String DS_PARAM_INDENT = "indent";

    protected final Set<MediaType> supportedTypes = new LinkedHashSet<>(4);
    protected final Set<String> readerParams = new LinkedHashSet<>();
    protected final Set<String> writerParams = new LinkedHashSet<>();
    protected final Set<Class<?>> readerSupportedClasses = new LinkedHashSet<>(8);
    protected final Set<Class<?>> writerSupportedClasses = new LinkedHashSet<>(8);

    @Override
    public Value read(Document<?> doc) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public <T> Document<T> write(Value input, MediaType mediaType, Class<T> targetType) throws PluginException {
        throw new UnsupportedOperationException("not implemented!");
    }

    @Override
    public boolean canRead(Document<?> doc) {
        MediaType requestedType = doc.getMediaType();
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, readerParams) &&
                    // TODO: 9/2/20 write tests for null handling
                    (doc.getContent() == null || canReadClass(doc.getContent().getClass()))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canWrite(MediaType requestedType, Class<?> clazz) {
        for (MediaType supportedType : supportedTypes) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, writerParams) &&
                    canWriteClass(clazz)) {
                return true;
            }
        }

        return false;
    }

    protected boolean canReadClass(Class<?> cls) {
        for (Class<?> supported : readerSupportedClasses) {
            if (supported.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canWriteClass(Class<?> clazz) {
        for (Class<?> supported : writerSupportedClasses) {
            if (clazz.isAssignableFrom(supported)) {
                return true;
            }
        }
        return false;
    }

    private boolean parametersAreSupported(MediaType requestedType, Set<String> supported) {
        for (String param : requestedType.getParameters().keySet()) {
            if (!(MediaTypes.PARAM_QUALITY_FACTOR.equals(param) || MediaTypes.PARAM_CHARSET.equals(param))) {
                // if it's not known params q or charset, and it's not supported param, we fail
                boolean matched = false;
                for (String supportedParam : supported) {
                    if (param.matches(supportedParam)) {
                        matched = true;
                        break;
                    }
                }
                if (!matched) {
                    return false;
                }
            }
        }

        return true;
    }
}
