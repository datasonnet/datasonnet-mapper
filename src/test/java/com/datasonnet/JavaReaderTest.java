package com.datasonnet;

import com.datasonnet.document.Document;
import com.datasonnet.document.JavaObjectDocument;
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

public class JavaReaderTest {
    @BeforeAll
    static void registerPlugins() throws Exception {
        DataFormatService.getInstance().findAndRegisterPlugins();
    }

    @Test
    void testJavaReader() throws Exception {
        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red","white","blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        Document data = new JavaObjectDocument(theGizmo);

        String mapping = TestResourceReader.readFileAsString("readJavaTest.ds");

        Mapper mapper = new Mapper(mapping, new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/json");

        String expectedJson = TestResourceReader.readFileAsString("javaTest.json");
        JSONAssert.assertEquals(expectedJson, mapped.getContents().toString(), false);
    }
}
