package com.datasonnet;

import org.junit.jupiter.api.Test;

import com.datasonnet.Mapper;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class DWCryptoTest {
    
    @Test
    void testDWCryptoMD5() {
        Mapper mapper = new Mapper("DW.Crypto.MD5(\"HelloWorld\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("68e109f0f40ca72a15e05cc22786f8e6".equals(hash));
    }
    
}