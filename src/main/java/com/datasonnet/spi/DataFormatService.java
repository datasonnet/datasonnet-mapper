package com.datasonnet.spi;

import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.plugins.DefaultCSVFormatPlugin;
import com.datasonnet.plugins.DefaultJSONFormatPlugin;
import com.datasonnet.plugins.DefaultJavaFormatPlugin;
import com.datasonnet.plugins.DefaultPlainTextFormatPlugin;
import com.datasonnet.plugins.DefaultXMLFormatPlugin$;

import java.util.*;

public class DataFormatService {
    private final List<DataFormatPlugin> plugins;
    public static final DataFormatService DEFAULT =
            new DataFormatService(Arrays.asList(
                    new DefaultJSONFormatPlugin(),
                    new DefaultJavaFormatPlugin(),
                    DefaultXMLFormatPlugin$.MODULE$,
                    new DefaultCSVFormatPlugin(),
                    new DefaultPlainTextFormatPlugin()));

    public DataFormatService(List<DataFormatPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<DataFormatPlugin> getPlugins() {
        return Collections.unmodifiableList(plugins);
    }

    public Optional<DataFormatPlugin> thatProduces(MediaType output, Class<?> target) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canWrite(output, target)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }

    public Optional<DataFormatPlugin> thatAccepts(Document<?> doc) {
        for (DataFormatPlugin plugin : plugins) {
            if (plugin.canRead(doc)) {
                return Optional.of(plugin);
            }
        }
        return Optional.empty();
    }
}
