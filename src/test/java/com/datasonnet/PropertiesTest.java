package com.datasonnet;

import com.datasonnet.header.Properties;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class PropertiesTest {

    @Test
    void simpleExample() {
        String header = "version 1\n    output.application/xml\\.extra.NamespaceDeclarations.ア1  =   http://example.com/1\n" +
                "     other\\tstuff   :Г\n" +
                "    # commented: header\n" +
                "    uncommented: header";
        Properties properties = makeProperties(header);
        assertEquals("1", properties.getProperty("version"));
        assertEquals("http://example.com/1", properties.getProperty("output.application/xml\\.extra.NamespaceDeclarations.ア1"));
        assertEquals("Г", properties.getProperty("other\tstuff"));
        // all three of these last effectively check the same thing, but by demonstrating different specific things
        // that do not happen
        assertNull(properties.getProperty("commented"));
        assertNull(properties.getProperty("# commented"));
        assertEquals(4, properties.size());  // version, the xml namespace, other stuff, and uncommented, but not commented
    }

    private Properties makeProperties(String header) {
        Properties properties = new Properties();
        properties.load(new StringReader(header));
        return properties;
    }

    /**
     * There was a bug where any non-u escaped character followed by four hex characters
     * caused the hex characters to be dropped. This replicates the generated test case that discovered the bug.
     */
    @Test
    void overEagerEscape() {
        String header = "A = valuevalue\n" +
                "a\\ AEC0\\u09051a\\u0a85 = value";
        Properties properties = makeProperties(header);
        assertEquals("value", properties.getProperty("a AEC0अ1aઅ"));

    }
}
