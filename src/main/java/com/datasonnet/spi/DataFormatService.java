package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.plugins.JSONFormatPlugin;
import ujson.Str;
import ujson.Value;

import java.util.*;

public class DataFormatService {

    private static DataFormatService service;

    private Map<String, List<DataFormatPlugin>> pluginRegistry = new HashMap<>();

    DataFormatService() {
        registerPlugin(new JSONFormatPlugin());
    }

    public static synchronized DataFormatService getInstance() {
        if (service == null) {
            service = new DataFormatService();
        }
        return service;
    }

    public void registerPlugin(DataFormatPlugin plugin) {
        for(String identifier : plugin.getSupportedIdentifiers()) {
            List<DataFormatPlugin> pluginsList = pluginRegistry.getOrDefault(identifier, new ArrayList<>());
            if (!pluginsList.contains(plugin)) {
                pluginsList.add(plugin);
            }
            pluginRegistry.put(identifier, pluginsList);
        }
    }

    public void registerPluginFor(String identifier, DataFormatPlugin plugin) {
        List<DataFormatPlugin> pluginsList = pluginRegistry.getOrDefault(identifier, new ArrayList<>());
        if (!pluginsList.contains(plugin)) {
            pluginsList.add(plugin);
        }
        pluginRegistry.put(identifier, pluginsList);
    }

    public void registerPlugins(Iterable<DataFormatPlugin> plugins) {
        for(DataFormatPlugin plugin : plugins) {
            registerPlugin(plugin);
        }
    }

    public DataFormatPlugin getPluginFor(String identifier) {
        identifier = identifier.toLowerCase();
        return pluginRegistry.containsKey(identifier) ? pluginRegistry.get(identifier).get(0) : null;
    }

    public List<DataFormatPlugin> findPlugins() {

        ServiceLoader<DataFormatPlugin> loader = ServiceLoader.load(DataFormatPlugin.class);
        return new ArrayList() {{
            for (DataFormatPlugin plugin : loader) {
                add(plugin);
            }
        }};
    }

    public List<String> getSupportedIdentifiers() {
        return new ArrayList(pluginRegistry.keySet());
    }

    public void findAndRegisterPlugins() {
        registerPlugins(findPlugins());
    }
    
}
