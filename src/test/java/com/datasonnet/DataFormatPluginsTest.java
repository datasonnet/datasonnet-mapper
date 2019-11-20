package com.datasonnet;

import com.datasonnet.portx.spi.DataFormatPlugin;
import com.datasonnet.portx.spi.DataFormatService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataFormatPluginsTest {

    @Test
    void testDataFormatPlugins() throws Exception {
        DataFormatService service = DataFormatService.getInstance();
        assertNull(service.getPluginFor("application/xml"));
        Map<String, List<DataFormatPlugin>> plugins = service.findPlugins();
        assertTrue(plugins.containsKey("application/xml"));
        assertFalse(plugins.containsKey("blah/blah"));
        List<DataFormatPlugin> xmlPlugins = plugins.get("application/xml");
        assertTrue(xmlPlugins.size() > 0);
        DataFormatPlugin xmlPlugin = xmlPlugins.get(0);
        service.registerPlugin("application/xml", xmlPlugin);
        assertNotNull(service.getPluginFor("application/xml"));
        service.findAndRegisterPlugins();
        assertNotNull(service.getPluginFor("application/csv"));
    }
}
