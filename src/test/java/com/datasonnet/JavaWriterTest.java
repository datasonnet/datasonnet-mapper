package com.datasonnet;

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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.javatest.Gizmo;
import com.datasonnet.javatest.WsdlGeneratedObj;
import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


public class JavaWriterTest {

    @Test
    void testJavaWriter() throws Exception {
        //Test with output as Gizmo class
        String json = TestResourceReader.readFileAsString("javaTest.json");
        String mapping = TestResourceReader.readFileAsString("writeJavaTest.ds");

        Document<String> data = new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON);

        Mapper mapper = new Mapper(mapping);


        Document<Gizmo> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Gizmo.class);

        Object result = mapped.getContent();
        assertTrue(result instanceof Gizmo);

        Gizmo gizmo = (Gizmo) result;
        assertEquals("gizmo", gizmo.getName());
        assertEquals(123, gizmo.getQuantity());
        assertEquals(true, gizmo.isInStock());
        assertEquals(Arrays.asList("red", "white", "blue"), gizmo.getColors());
        assertEquals("ACME Corp.", gizmo.getManufacturer().getManufacturerName());
        assertEquals("ACME123", gizmo.getManufacturer().getManufacturerCode());

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        assertEquals("2020-01-06", df.format(gizmo.getDate()));

        //Test with default output, i.e. java.util.HashMap
        mapping = mapping.substring(mapping.lastIndexOf("*/") + 2);

        mapper = new Mapper(mapping);
        Document<Map> mappedMap = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Map.class);

        result = mappedMap.getContent();
        assertTrue(result instanceof java.util.Map);

        Map gizmoMap = (Map) result;
        assertTrue(gizmoMap.get("colors") instanceof java.util.List);
        assertTrue(gizmoMap.get("manufacturer") instanceof java.util.Map);
    }

    @Test
    void testJavaWriteFunction() throws Exception {
        String json = TestResourceReader.readFileAsString("javaTest.json");
        Document<String> data = new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON);

        //Test calling write() function
        String mapping = TestResourceReader.readFileAsString("writeJavaFunctionTest.ds");
        Mapper mapper = new Mapper(mapping);

        try {
            mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA);
            fail("Should not succeed");
        } catch (Exception e) {
            // this error is now thrown by jackson as it _will_ try to write a String...
            assertTrue(e.getMessage().contains("Unable to convert to target type"), "Failed with wrong message: " + e.getMessage());
        }
    }

    @Test
    void testIncompatibleRequestedType() throws Exception {
        Document<String> data = new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON);
        Mapper mapper = new Mapper("/** DataSonnet\n" +
                "version=2.0\n" +
                "output application/java; OutputClass=java.lang.Object\n" +
                "*/\n" +
                "{ a: 5 }");
        Document<Map> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Map.class);
        assertTrue(mapped.getContent() instanceof Map);

        Document<Object> remapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Object.class);
        assertTrue(remapped.getContent() instanceof Map); // yep, still should be, because that's what we auto-convert to!
    }

    @Test
    void testJAXBElementMapping() throws Exception {
        Document<String> data = new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON);
        String mapping = TestResourceReader.readFileAsString("writeJAXBElement.ds");
        Mapper mapper = new Mapper(mapping);

        Document<WsdlGeneratedObj> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, WsdlGeneratedObj.class);
        Object result = mapped.getContent();
        assertTrue(result instanceof WsdlGeneratedObj);

        Document<Object> objectMapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Object.class);
        Object objectResult = objectMapped.getContent();
        assertTrue(objectResult instanceof WsdlGeneratedObj);

        JAXBContext jaxbContext = JAXBContext.newInstance(WsdlGeneratedObj.class );
        Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
        jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter writer = new StringWriter();
        jaxbMarshaller.marshal(result, writer);
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
                "<WsdlGeneratedObj xmlns:ns2=\"http://com.datasonnet.test\">\n" +
                "    <ns2:testField>\n" +
                "        <test>Hello World</test>\n" +
                "    </ns2:testField>\n" +
                "</WsdlGeneratedObj>\n", writer.toString());

        StringWriter objectWriter = new StringWriter();
        jaxbMarshaller.marshal(objectResult, objectWriter);
        assertEquals(writer.toString(), objectWriter.toString());
    }
}
