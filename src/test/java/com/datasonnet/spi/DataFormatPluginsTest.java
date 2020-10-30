
/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//package com.datasonnet.spi;
//
//import com.datasonnet.plugins.XMLFormatPlugin;
//import org.junit.jupiter.api.Test;
//
//import java.util.List;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//public class DataFormatPluginsTest {
//
//    @Test
//    void initialState() {
//        DataFormatService service = new DataFormatService();
//        assertNull(service.getPluginFor("application/xml"));
//        assertNotNull(service.getPluginFor("application/json"));
//    }
//
//    @Test
//    void xmlBuiltInDiscovered() {
//        DataFormatService service = new DataFormatService();
//        List<DataFormatPlugin> plugins = service.findPlugins();
//        assertTrue(plugins.stream().anyMatch((plugin) -> plugin instanceof XMLFormatPlugin));
//    }
//
//    @Test
//    void namedRegistrationWorks() {
//        DataFormatService service = new DataFormatService();
//        service.registerPluginFor("application/xml", new XMLFormatPlugin());
//        assertNotNull(service.getPluginFor("application/xml"));
//    }
//
//    @Test
//    void automaticRegistrationWorks() {
//        DataFormatService service = new DataFormatService();
//        service.findAndRegisterPlugins();
//        assertNotNull(service.getPluginFor("application/csv"));
//        assertNotNull(service.getPluginFor("application/xml"));
//        assertEquals(service.getPluginFor("xml"), service.getPluginFor("application/xml"));
//    }
//}
