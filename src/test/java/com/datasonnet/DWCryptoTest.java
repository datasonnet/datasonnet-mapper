package com.datasonnet;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DWCryptoTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "DW" +".";
    private final String pack = "Crypto";

    @Disabled
    @Test
    void testDWCrypto_HMACBinary() {
        Mapper mapper = new Mapper(lib + pack + ".HMACBinary(\"key\", \"HelloWorld\", \"HmacSHA1\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("64639162faf67e907eed3a7e574def4f99d405a0\\u".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".HMACBinary(\"key\", \"HelloWorld\", \"HmacSHA256\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abd218eb63aec65e8d7edd41b11a9e61615d30144ec75c867f062a26cb270833".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".HMACBinary(\"key\", \"HelloWorld\", \"HmacSHA512\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68fa6eed41c3d5248c79cfb92db912895582504dd1aeb9456fe1e0081fc5a0fd684189e8c11185db331de16fd6f1565b9cd9ef59b335435219e0c73ff99f6217".toLowerCase(), hash);
    }

    @Test
    void testDWCrypto_HMACWith() {
        Mapper mapper = new Mapper(lib + pack + ".HMACWith(\"key\", \"HelloWorld\", \"HmacSHA1\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("64639162faf67e907eed3a7e574def4f99d405a0".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".HMACWith(\"key\", \"HelloWorld\", \"HmacSHA256\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("abd218eb63aec65e8d7edd41b11a9e61615d30144ec75c867f062a26cb270833".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".HMACWith(\"key\", \"HelloWorld\", \"HmacSHA512\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68fa6eed41c3d5248c79cfb92db912895582504dd1aeb9456fe1e0081fc5a0fd684189e8c11185db331de16fd6f1565b9cd9ef59b335435219e0c73ff99f6217".toLowerCase(), hash);
    }

    @Test
    void testDWCrypto_MD5() {
        Mapper mapper = new Mapper(lib + pack + ".MD5(\"HelloWorld\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68e109f0f40ca72a15e05cc22786f8e6", hash);
    }

    @Test
    void testDWCrypto_SHA1() {
        Mapper mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"SHA-1\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("DB8AC1C259EB89D4A131B253BACFCA5F319D54F2".toLowerCase(), hash);
    }

    @Disabled
    @Test
    void testDWCrypto_hashWith() {
        Mapper mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"MD5\")");
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("68e109f0f40ca72a15e05cc22786f8e6", hash);

        mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"SHA-1\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("DB8AC1C259EB89D4A131B253BACFCA5F319D54F2".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"SHA-256\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("872E4E50CE9990D8B041330C47C9DDD11BEC6B503AE9386A99DA8584E9BB12C4".toLowerCase(), hash);

        mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"SHA-384\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("293cd96eb25228a6fb09bfa86b9148ab69940e68903cbc0527a4fb150eec1ebe0f1ffce0bc5e3df312377e0a68f1950a", hash);

        mapper = new Mapper(lib + pack + ".hashWith(\"HelloWorld\", \"SHA-512\")");
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("8AE6AE71A75D3FB2E0225DEEB004FAF95D816A0A58093EB4CB5A3AA0F197050D7A4DC0A2D5C6FBAE5FB5B0D536A0A9E6B686369FA57A027687C3630321547596".toLowerCase(), hash);
    }
    
}