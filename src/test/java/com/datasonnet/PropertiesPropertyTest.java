package com.datasonnet;


import com.datasonnet.header.Properties;
import com.datasonnet.util.PropertiesGenerator;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;


@RunWith(JUnitQuickcheck.class)
public class PropertiesPropertyTest {

    @Property
    public void idempotentRead(@From(PropertiesGenerator.class) String properties) throws Exception {
        Properties first = new Properties();
        first.load(new StringReader(properties));

        StringWriter writer = new StringWriter();
        first.store(writer, "");

        Properties second = new Properties();
        second.load(new StringReader(writer.toString()));

        assertEquals("On second write was:\n\n" + writer.toString(), first.stringPropertyNames(), second.stringPropertyNames());

        for(String name : first.stringPropertyNames()) {
            assertEquals("On second write was:\n\n" + writer.toString(), first.getProperty(name), second.getProperty(name));
        }
    }
}
