package com.datasonnet;

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.javatest.Gizmo;
import com.datasonnet.javatest.Manufacturer;
import com.datasonnet.javatest.TestField;
import com.datasonnet.javatest.WsdlGeneratedObj;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaReaderTest {

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

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        theGizmo.setDate(df.parse("2020-01-06"));

        Document<Gizmo> data = new DefaultDocument<>(theGizmo);

        String mapping = TestResourceReader.readFileAsString("readJavaTest.ds");

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = TestResourceReader.readFileAsString("javaTest.json");
        JSONAssert.assertEquals(expectedJson, mapped, false);
    }

    @Test
    void testJAXBElementMapping() throws Exception {
        TestField testField = new TestField();
        testField.setTest("HelloWorld");
        WsdlGeneratedObj obj = new WsdlGeneratedObj();
        obj.setTestField(new JAXBElement<TestField>(new QName("http://com.datasonnet.test", "testField"),
                                                    TestField.class,
                                                    testField));
        Document<WsdlGeneratedObj> data = new DefaultDocument<>(obj);
        Mapper mapper = new Mapper("payload");
        Document<String> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON);

        String result = mapped.getContent();
        JSONAssert.assertEquals("{\"testField\":{\"name\":\"{http://com.datasonnet.test}testField\",\"declaredType\":\"com.datasonnet.javatest.TestField\",\"value\":{\"test\":\"HelloWorld\"}}}", result, true);
    }
}
