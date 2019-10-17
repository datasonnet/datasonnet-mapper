package com.datasonnet;

import com.datasonnet.util.TestResourceReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JavaReaderTest {

    @Test
    void testJavaReaderSimpleMap() throws URISyntaxException, IOException {
        Map testData = new HashMap();
        testData.put("fName", "Eugene");

        Document data = new ObjectDocument(testData, "application/java");

        Mapper mapper = new Mapper("{ firstName: payload.fName }", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/json");

        assertEquals("{\"firstName\":\"Eugene\"}", mapped.contents());
    }

    @Test
    void testJavaReaderObject() throws URISyntaxException, IOException {
        Person testData = new Person("Eugene", "Berman");
        Document data = new ObjectDocument(testData, "application/java");

        Mapper mapper = new Mapper("payload", new ArrayList<>(), true);
        Document mapped = mapper.transform(data, new HashMap<>(), "application/json");

        assertEquals("{\"firstName\":\"Eugene\",\"lastName\":\"Berman\"}", mapped.contents());
    }

    class Person {
        String firstName;
        String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }
    }
}
