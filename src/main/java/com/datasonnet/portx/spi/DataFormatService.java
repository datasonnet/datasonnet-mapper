package com.datasonnet.portx.spi;

import java.util.*;

public class DataFormatService {

    private static DataFormatService service;

    private static Map<String, List<DataFormatPlugin>> pluginRegistry;

    private DataFormatService() {
        pluginRegistry = new HashMap<>();
    }

    public static synchronized DataFormatService getInstance() {
        if (service == null) {
            service = new DataFormatService();
        }
        return service;
    }

    public void registerPlugin(String mimeType, DataFormatPlugin plugin) {
        List<DataFormatPlugin> pluginsList = pluginRegistry.getOrDefault(mimeType, new ArrayList<>());
        if (!pluginsList.contains(plugin)) {
            pluginsList.add(plugin);
        }
        pluginRegistry.put(mimeType, pluginsList);
    }

    public DataFormatPlugin getPluginFor(String mimeType) {
        //TODO should we return list instead?
        return pluginRegistry.containsKey(mimeType) ? pluginRegistry.get(mimeType).get(0) : null;
    }

    public Map<String, List<DataFormatPlugin>> findPlugins() {
        Map<String, List<DataFormatPlugin>> pluginsMap = new HashMap<>();

        ServiceLoader<DataFormatPlugin> loader = ServiceLoader.load(DataFormatPlugin.class);

        for (DataFormatPlugin plugin : loader) {
            for (String mimeType: plugin.getSupportedMimeTypes()) {
                List<DataFormatPlugin> pluginsList = pluginsMap.getOrDefault(mimeType, new ArrayList<>());
                pluginsList.add(plugin);
                pluginsMap.put(mimeType, pluginsList);
            }
        }

        return pluginsMap;
    }

    public List<String> getSupportedMimeTypes() {
        return new ArrayList(pluginRegistry.keySet());
    }

    public void findAndRegisterPlugins() {
        pluginRegistry.putAll(findPlugins());
    }
}
