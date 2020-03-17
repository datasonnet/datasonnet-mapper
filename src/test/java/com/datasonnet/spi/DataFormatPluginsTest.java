package com.datasonnet.spi;

import com.datasonnet.plugins.XMLFormatPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DataFormatPluginsTest {

    @Test
    void initialState() {
        DataFormatService service = new DataFormatService();
        assertNull(service.getPluginFor("application/xml"));
        assertNotNull(service.getPluginFor("application/json"));
    }

    @Test
    void xmlBuiltInDiscovered() {
        DataFormatService service = new DataFormatService();
        List<DataFormatPlugin> plugins = service.findPlugins();
        assertTrue(plugins.stream().anyMatch((plugin) -> plugin instanceof XMLFormatPlugin));
    }

    @Test
    void namedRegistrationWorks() {
        DataFormatService service = new DataFormatService();
        service.registerPluginFor("application/xml", new XMLFormatPlugin());
        assertNotNull(service.getPluginFor("application/xml"));
    }

    @Test
    void automaticRegistrationWorks() {
        DataFormatService service = new DataFormatService();
        service.findAndRegisterPlugins();
        assertNotNull(service.getPluginFor("application/csv"));
        assertNotNull(service.getPluginFor("application/xml"));
        assertEquals(service.getPluginFor("xml"), service.getPluginFor("application/xml"));
    }
}
