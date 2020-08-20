package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BinariesTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" +".";
    private final String pack = "binaries";

    @Test
    void testBinaries_fromBase64() {
        Mapper mapper = new Mapper(lib + pack + ".fromBase64(\"SGVsbG8gV29ybGQ=\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Hello World", value);

        mapper = new Mapper(lib + pack + ".fromBase64(\"NDU=\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("45", value);
    }

    @Test
    void testBinaries_fromHex() {
        Mapper mapper = new Mapper(lib + pack + ".fromHex(\"48656C6C6F20576F726C64\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Hello World", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"3435\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("45", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"2D\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-", value);
    }

    //TODO additional testing
    @Test
    void testBinaries_readLinesWith() {
        Mapper mapper = new Mapper(lib + pack + ".readLinesWith(\"Line 1\\nLine 2\\nLine 3\\nLine 4\\nLine 5\\n\", \"UTF-8\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("[Line 1,Line 2,Line 3,Line 4,Line 5]", value);

    }

    @Test
    void testBinaries_toBase64() {
        Mapper mapper = new Mapper(lib + pack + ".toBase64(\"Hello World\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("SGVsbG8gV29ybGQ=", value);

        mapper = new Mapper(lib + pack + ".toBase64(45)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("NDU=", value);

        mapper = new Mapper(lib + pack + ".toBase64(45.0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("NDU=", value);

        mapper = new Mapper(lib + pack + ".toBase64(45.1)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("NDUuMQ==", value);
    }

    @Test
    void testBinaries_toHex() {
        Mapper mapper = new Mapper(lib + pack + ".toHex(\"Hello World\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("48656C6C6F20576F726C64", value);

        mapper = new Mapper(lib + pack + ".toHex(45)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2D", value);

        mapper = new Mapper(lib + pack + ".toHex(45.0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2D", value);

        mapper = new Mapper(lib + pack + ".toHex(45.1)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2D", value);
    }

    //TODO additional testing
    @Test
    void testBinaries_writeLinesWith() {
        Mapper mapper = new Mapper(lib + pack + ".writeLinesWith([\"Line 1\",\"Line 2\",\"Line 3\",\"Line 4\",\"Line 5\"], \"UTF-8\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("Line 1\\nLine 2\\nLine 3\\nLine 4\\nLine 5\\n", value);

    }

}
