package com.datasonnet.document;

import com.datasonnet.Mapper;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify operation of the JavaObjectDocument logic.
 */
class JavaObjectDocumentTest {

    @Test
    void testCanGetContentAs() {
        //
        // Test Setup
        //
        Mapper mapper = new Mapper("true");

        HashMap<String, String> payload = new HashMap<>();
        JavaObjectDocument javaObjectDocument = new JavaObjectDocument(payload);

        //
        // Execute
        //
        JavaObjectDocument
            result =
            (JavaObjectDocument) mapper
                .transform(javaObjectDocument, Collections.EMPTY_MAP, "application/x-java-object");

        //
        // Verify
        //
        assertTrue(result.canGetContentsAs(Object.class), "canGetContentAs(Object.class)");
        assertTrue(result.canGetContentsAs(Boolean.class), "canGetContentAs(Boolean.class)");

        assertFalse(result.canGetContentsAs(boolean.class), "! canGetContentAs(boolean.class)");
        assertFalse(result.canGetContentsAs(String.class), "! canGetContentAs(String.class)");
    }

    @Test
    void testGetContentAs() {
        //
        // Test Setup
        //
        Mapper mapper = new Mapper("true");

        HashMap<String, String> payload = new HashMap<>();
        JavaObjectDocument javaObjectDocument = new JavaObjectDocument(payload);

        //
        // Execute
        //
        JavaObjectDocument
            result =
            (JavaObjectDocument) mapper
                .transform(javaObjectDocument, Collections.EMPTY_MAP, "application/x-java-object");

        //
        // Verify
        //
        assertTrue((boolean) result.getContentsAs(Boolean.class), "getContentAs(Boolean.class)");
    }
}