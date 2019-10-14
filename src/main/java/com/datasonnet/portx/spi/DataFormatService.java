package com.datasonnet.portx.spi;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class DataFormatService {

    private static DataFormatService service;
    private ServiceLoader<DataFormatPlugin> loader;

    private DataFormatService() {
        loader = ServiceLoader.load(DataFormatPlugin.class, DataFormatService.class.getClassLoader());
    }

    public static synchronized DataFormatService getInstance() {
        if (service == null) {
            service = new DataFormatService();
        }
        return service;
    }

    public DataFormatPlugin getPluginFor(String mimeType) throws UnsupportedMimeTypeException {
        Iterator<DataFormatPlugin> plugins = loader.iterator();

        while (plugins.hasNext()) {
            DataFormatPlugin nextPlugin = plugins.next();
            String[] supportedTypes = nextPlugin.getSupportedMimeTypes();
            for (String nextType: supportedTypes) {
                if (nextType.equalsIgnoreCase(mimeType)) {
                    return nextPlugin;
                }
            }
        }

        throw new UnsupportedMimeTypeException("No suitable plugin found for mime type: " + mimeType);
    }

    public List<String> getSupportedMimeTypes() {
        List<String> mimeTypes = new ArrayList<>();

        mimeTypes.add("application/json");

        Iterator<DataFormatPlugin> plugins = loader.iterator();

        while (plugins.hasNext()) {
            DataFormatPlugin nextPlugin = plugins.next();
            for (String t : nextPlugin.getSupportedMimeTypes()) {
                mimeTypes.add(t);
            }
        }

        return mimeTypes;
    }
}
