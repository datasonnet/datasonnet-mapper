package com.datasonnet.javaplugin;

/*-
 * Copyright 2019-2025 the original author or authors.
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

import com.datasonnet.Mapper;
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaType;
import com.datasonnet.document.MediaTypes;
import com.datasonnet.util.TestResourceReader;
import jakarta.xml.bind.JAXBElement;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import javax.xml.namespace.QName;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.*;

public class JavaReaderTest {

    @Test
    void testJavaReader() throws Exception {
        Gizmo theGizmo = new Gizmo();
        theGizmo.setName("gizmo");
        theGizmo.setQuantity(123);
        theGizmo.setInStock(true);
        theGizmo.setColors(Arrays.asList("red", "white", "blue"));

        Manufacturer manufacturer = new Manufacturer();
        manufacturer.setManufacturerName("ACME Corp.");
        manufacturer.setManufacturerCode("ACME123");
        theGizmo.setManufacturer(manufacturer);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        theGizmo.setDate(df.parse("2020-01-06"));

        Document<Gizmo> data = new DefaultDocument<>(theGizmo, MediaTypes.APPLICATION_JAVA);

        String mapping = TestResourceReader.readFileAsString("readJavaTest.ds");

        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = TestResourceReader.readFileAsString("javaTest.json");
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }

    @Test
    void testOptionalJavaReader() throws Exception {
        User user = new User("John Doe", "john@example.com");
        Document<User> data = new DefaultDocument<>(user, MediaTypes.APPLICATION_JAVA);
        String mapping = TestResourceReader.readFileAsString("readJavaOptionalTest.ds");
        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedJson = "{\"userName\":\"John Doe\",\"userEmail\":\"john@example.com\"}";
        JSONAssert.assertEquals(expectedJson, mapped, true);

        user.setEmail(null);
        data = new DefaultDocument<>(user, MediaTypes.APPLICATION_JAVA);
        mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();
        expectedJson = "{\"userName\":\"John Doe\"}";
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }

    @Test
    void testJava8TypesReader() throws Exception {
        User user = new User(
                "John Doe",
                "john@example.com",
                30,
                180L,
                70.5,
                LocalDate.of(1990, 1, 1),
                LocalTime.of(7, 0),
                LocalDateTime.of(2023, 3, 24, 12, 0)
        );

        Document<User> data = new DefaultDocument<>(user, MediaTypes.APPLICATION_JAVA);
        String mapping = TestResourceReader.readFileAsString("readJavaOptionalTest.ds");
        Mapper mapper = new Mapper(mapping);
        String mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();

        String expectedUserResponse = TestResourceReader.readFileAsString("user1.json");

        JSONAssert.assertEquals(expectedUserResponse, mapped, true);
        user.setAge(OptionalInt.empty());
        user.setHeight(OptionalLong.empty());
        user.setWeight(OptionalDouble.empty());
        user.setEmail(null);
        data = new DefaultDocument<>(user, MediaTypes.APPLICATION_JAVA);
        mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON).getContent();
        String expectedJson = TestResourceReader.readFileAsString("user2.json");
        JSONAssert.assertEquals(expectedJson, mapped, true);
    }



    @Test
    void testJAXBElementMapping() throws Exception {
        TestField testField = new TestField();
        testField.setTest("HelloWorld");
        WsdlGeneratedObj obj = new WsdlGeneratedObj();
        obj.setTestField(new JAXBElement<TestField>(new QName("http://com.datasonnet.test", "testField"),
                TestField.class,
                testField));
        Document<WsdlGeneratedObj> data = new DefaultDocument<>(obj, MediaTypes.APPLICATION_JAVA);
        Mapper mapper = new Mapper("payload");
        Document<String> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON);

        String result = mapped.getContent();
        JSONAssert.assertEquals("{\"testField\":{\"name\":\"{http://com.datasonnet.test}testField\",\"declaredType\":\"com.datasonnet.javaplugin.TestField\",\"value\":{\"test\":\"HelloWorld\"}}}", result, true);
    }

    @Test
    void testNullJavaObject() throws Exception {
        Document<?> nullObj = DefaultDocument.NULL_INSTANCE;
        Mapper mapper = new Mapper("payload == null");
        Document<Boolean> mapped = mapper.transform(nullObj, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Boolean.class);
        assertTrue(mapped.getContent());
    }

    @Test
    void testVaryingMediaType() {
        Document<Object> object = new DefaultDocument<>("", MediaType.parseMediaType("application/java"));
        Mapper mapper = new Mapper("payload");
        Document<Object> mapped = mapper.transform(object, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Object.class);
    }

    @Test
    void testOffsetDateTime() {
        OffsetDateTime dateTime = OffsetDateTime.now();
        Document<OffsetDateTime> data = new DefaultDocument<>(dateTime, MediaTypes.APPLICATION_JAVA);
        try {
            Mapper mapper = new Mapper("payload");
            Document<String> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON);
            fail("Import should fail");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Java 8 date/time type `java.time.OffsetDateTime` not supported by default"), "Found message: " + e.getMessage());
        }

        String script = "/** DataSonnet\n" +
                "version=2.5\n" +
                "input payload application/x-java-object; FindAndRegisterModules=true\n" +
                "*/\n" +
                "\n" +
                "payload";
        Mapper mapper = new Mapper(script);
        Document<String> mapped = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JSON);
        String result = mapped.getContent();
        assertNotNull(result);
    }
}
