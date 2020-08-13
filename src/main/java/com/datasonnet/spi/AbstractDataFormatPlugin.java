package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import ujson.Value;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractDataFormatPlugin implements DataFormatPlugin {
    public static final String DS_PARAM_INDENT = "indent";

    protected final Set<String> READER_PARAMS = new HashSet<>();
    protected final Set<String> WRITER_PARAMS = new HashSet<>();
    protected final Set<Class<?>> READER_SUPPORTED_CLASSES = new HashSet<>();
    protected final Set<Class<?>> WRITER_SUPPORTED_CLASSES = new HashSet<>();

    protected Set<Class<?>> getReaderSupportedClasses() {
        return READER_SUPPORTED_CLASSES;
    }

    protected Set<Class<?>> getWriterSupportedClasses() {
        return WRITER_SUPPORTED_CLASSES;
    }

    @Override
    public Set<String> getReaderParams() {
        return READER_PARAMS;
    }

    @Override
    public Set<String> getWriterParams() {
        return WRITER_PARAMS;
    }

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

        for (MediaType supportedType : supportedTypes()) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, READER_PARAMS) &&
                    canReadClass(doc.getContent().getClass())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canWrite(MediaType requestedType, Class<?> clazz) {

        for (MediaType supportedType : supportedTypes()) {
            if (supportedType.includes(requestedType) &&
                    parametersAreSupported(requestedType, WRITER_PARAMS) &&
                    canWriteClass(clazz)) {
                return true;
            }
        }

        return false;
    }

    public abstract Set<MediaType> supportedTypes();

    protected boolean canReadClass(Class<?> cls) {
        for (Class<?> supported : READER_SUPPORTED_CLASSES) {
            if (supported.isAssignableFrom(cls)) {
                return true;
            }
        }
        return false;
    }

    protected boolean canWriteClass(Class<?> clazz) {
        for (Class<?> supported : WRITER_SUPPORTED_CLASSES) {
            if (supported.equals(clazz)) {
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
