package com.datasonnet;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class DWCryptoTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Crypto";

    @Test
    void testDWCryptoMD5() {
        Mapper mapper = new Mapper(lib + pack + ".MD5(\"HelloWorld\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("68e109f0f40ca72a15e05cc22786f8e6".equals(hash));
    }
    
}