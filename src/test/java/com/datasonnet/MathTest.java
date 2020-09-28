package com.datasonnet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MathTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds" + ".";
    private final String pack = "math";

    @Test
    void testMath_mantissa() {
        Mapper mapper = new Mapper(lib + pack + ".mantissa(2)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0.5", value);
    }

    @Test
    void testMath_exponent() {
        Mapper mapper = new Mapper(lib + pack + ".exponent(5)");
        String value = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("3", value);
    }
}
