package com.datasonnet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NumbersTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" +".";
    private final String pack = "numbers";

    @Test
    void testNumbers_fromBinary() {
        Mapper mapper = new Mapper(lib + pack + ".fromBinary(\"-10\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-2", value);

        mapper = new Mapper(lib + pack + ".fromBinary(\"1111111111111111111111111111111\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2147483647", value);

        mapper = new Mapper(lib + pack + ".fromBinary(11)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);

        mapper = new Mapper(lib + pack + ".fromBinary(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".fromBinary(\"100\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4", value);

        /*Needs higher than 32 bit support
        mapper = new Mapper(lib + pack + ".fromBinary(\"1111111111111111111111111111111111111111111111111111111111111\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4611686018427387903", value);
        */

    }

    @Test
    void testNumbers_fromHex() {
        Mapper mapper = new Mapper(lib + pack + ".fromHex(\"-1\")");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"3e3aeb\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4078315", value);

        mapper = new Mapper(lib + pack + ".fromHex(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".fromHex(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".fromHex(\"f\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("15", value);

        /*
        mapper = new Mapper(lib + pack + ".fromHex(\"3e3aeb4ae1383562f4b82261d969f7ac94ca4000000000000000\")");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("100000000000000000000000000000000000000000000000000000000000000", value);
        */
    }

    @Test
    void testNumbers_fromRadixNumber() {
        Mapper mapper = new Mapper(lib + pack + ".fromRadixNumber(10, 2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2", value);

        mapper = new Mapper(lib + pack + ".fromRadixNumber(\"ff\", 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("255", value);


        /* TODO support more than 32 bit number
        mapper = new Mapper(lib + pack + ".fromRadixNumber(\"3e3aeb4ae1383562f4b82261d969f7ac94ca4000000000000000\", 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("100000000000000000000000000000000000000000000000000000000000000", value);
        */
    }

    @Test
    void testNumbers_toBinary() {
        Mapper mapper = new Mapper(lib + pack + ".toBinary(-2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-10", value);

        mapper = new Mapper(lib + pack + ".toBinary(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".toBinary(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".toBinary(2)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        /*
        mapper = new Mapper(lib + pack + ".toBinary(100000000000000000000000000000000000000000000000000000000000000)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("11111000111010111010110100101011100001001110000011010101100010111101001011100000100010011000011101100101101001111101111010110010010100110010100100000000000000000000000000000000000000000000000000000000000000", value);
         */
    }

    @Test
    void testNumbers_toHex() {
        Mapper mapper = new Mapper(lib + pack + ".toHex(-1)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("-1", value);

        mapper = new Mapper(lib + pack + ".toHex(0)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0", value);

        mapper = new Mapper(lib + pack + ".toHex(null)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("null", value);

        mapper = new Mapper(lib + pack + ".toHex(15)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("f", value);

        /*
        mapper = new Mapper(lib + pack + ".toHex(100000000000000000000000000000000000000000000000000000000000000)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3e3aeb4ae1383562f4b82261d969f7ac94ca4000000000000000", value);
         */
    }

    @Test
    void testNumbers_toRadixNumber() {
        Mapper mapper = new Mapper(lib + pack + ".toRadixNumber(2, 2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("10", value);

        mapper = new Mapper(lib + pack + ".toRadixNumber(255, 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("ff", value);

        /*
        mapper = new Mapper(lib + pack + ".toRadixNumber(100000000000000000000000000000000000000000000000000000000000000, 16)");
        value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3e3aeb4ae1383562f4b82261d969f7ac94ca4000000000000000", value);
         */
    }



}