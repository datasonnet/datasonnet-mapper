package com.datasonnet;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JavaWriterTest {

    public static void main(String args[]) throws Exception {
        new JavaWriterTest().testJavaWriterMapOfObjects();
    }

    @Test
    void testJavaWriterSimpleMap() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("{ firstName: 'Eugene' }", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof Map);
        assertTrue(((Map)mappedContents).containsKey("firstName"));
        assertEquals(((Map)mappedContents).get("firstName"), "Eugene");
    }

    @Test
    void testJavaWriterMapOfObjects() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("{ person: { firstName: 'Eugene', lastName: 'Berman' }, id: 12345}", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof Map);
        assertTrue(((Map)mappedContents).containsKey("person"));
        assertTrue(((Map)mappedContents).get("person") instanceof Map);

        Map person = (Map)((Map)mappedContents).get("person");
        assertEquals(person.get("firstName"), "Eugene");
    }

    @Test
    void testJavaWriterSimpleList() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("[ 'Eugene', 'Russel' ]", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof List);
        assertTrue(((List)mappedContents).contains("Eugene"));
    }

    @Test
    void testJavaWriterListOfObjects() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("[ { firstName: 'Eugene', lastName: 'Berman' }, { firstName: 'Russel', lastName: 'Duhon' }]", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof List);

        List contentsList = (List)mappedContents;
        assertTrue(contentsList.get(0) instanceof Map);

        Map person = (Map)contentsList.get(0);
        assertEquals(person.get("firstName"), "Eugene");
    }

    @Test
    void testJavaWriterString() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("'HelloWorld'", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof String);
        assertEquals(mappedContents, "HelloWorld");
    }

    @Test
    void testJavaWriterNumber() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("12345", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof Number);
        assertEquals(mappedContents, 12345);
    }

    @Test
    void testJavaWriterBoolean() throws URISyntaxException, IOException {
        Document data = new StringDocument("{}", "application/json");

        Mapper mapper = new Mapper("true", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/java");
        Object mappedContents = mapped.contents();

        assertTrue(mappedContents instanceof Boolean);
        assertEquals(mappedContents, true);
    }

}
