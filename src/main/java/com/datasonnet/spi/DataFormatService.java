package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.plugins.JSONFormatPlugin;
import ujson.Str;
import ujson.Value;

import java.util.*;

public class DataFormatService {

    private Map<String, List<DataFormatPlugin>> pluginRegistry = new HashMap<>();

    public DataFormatService() {
        registerPlugin(new JSONFormatPlugin());
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

    public void findAndRegisterPlugins() {
        registerPlugins(findPlugins());
    }
}
