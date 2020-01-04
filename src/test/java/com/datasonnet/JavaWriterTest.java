package com.datasonnet;

import com.datasonnet.document.Document;
import com.datasonnet.document.JavaObjectDocument;
import com.datasonnet.document.StringDocument;
import com.datasonnet.javatest.Gizmo;
import com.datasonnet.javatest.Manufacturer;
import com.datasonnet.spi.DataFormatService;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaWriterTest {
    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }

    @Test
    void testJavaWriter() throws Exception {
        String json = TestResourceReader.readFileAsString("javaTest.json");
        String mapping = TestResourceReader.readFileAsString("writeJavaTest.ds");

        Document data = new StringDocument(json, "application/json");

        Mapper mapper = new Mapper(mapping, new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");

        Object result = mapped.getContents();
        assertTrue(result instanceof Gizmo);

        Gizmo gizmo = (Gizmo)result;
        assertEquals("gizmo", gizmo.getName());
        assertEquals(123, gizmo.getQuantity());
        assertEquals(true, gizmo.isInStock());
        assertEquals(Arrays.asList("red","white","blue"), gizmo.getColors());
        assertEquals("ACME Corp.", gizmo.getManufacturer().getManufacturerName());
        assertEquals("ACME123", gizmo.getManufacturer().getManufacturerCode());

        mapping = mapping.substring(mapping.lastIndexOf("*/") + 2);

        mapper = new Mapper(mapping, new ArrayList<>(), true);
        mapped = mapper.transform(data, new HashMap<>(), "application/java");

        result = mapped.getContents();
        assertTrue(result instanceof java.util.HashMap);

        Map gizmoMap = (Map)result;
        assertTrue(gizmoMap.get("colors") instanceof java.util.ArrayList);
        assertTrue(gizmoMap.get("manufacturer") instanceof java.util.HashMap);
    }
}
