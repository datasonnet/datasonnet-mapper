package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CryptoTest {

    @Test
    void testHash() {
        Mapper mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"MD2\")", Collections.emptyList(), true);
        String hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("4227ce10dca49dd2d0ba3f438d1ea9f3".equals(hash));

        mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"MD5\")", Collections.emptyList(), true);
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("68e109f0f40ca72a15e05cc22786f8e6".equals(hash));

        mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"SHA-1\")", Collections.emptyList(), true);
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("db8ac1c259eb89d4a131b253bacfca5f319d54f2".equals(hash));

        mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"SHA-256\")", Collections.emptyList(), true);
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("872e4e50ce9990d8b041330c47c9ddd11bec6b503ae9386a99da8584e9bb12c4".equals(hash));

        mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"SHA-384\")", Collections.emptyList(), true);
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("293cd96eb25228a6fb09bfa86b9148ab69940e68903cbc0527a4fb150eec1ebe0f1ffce0bc5e3df312377e0a68f1950a".equals(hash));

        mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"SHA-512\")", Collections.emptyList(), true);
        hash = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("8ae6ae71a75d3fb2e0225deeb004faf95d816a0a58093eb4cb5a3aa0f197050d7a4dc0a2d5c6fbae5fb5b0d536a0a9e6b686369fa57a027687c3630321547596".equals(hash));

        try {
            mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"DUMMY\")", Collections.emptyList(), true);
            hash = mapper.transform("{}").replaceAll("\"", "");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("Caused by: java.security.NoSuchAlgorithmException: DUMMY MessageDigest not available"));
        }
    }

    @Test
    void testHMAC() {
        Mapper mapper = new Mapper("DS.Crypto.hmac(\"HelloWorld\", \"PortX rules!\", \"HmacSHA1\")", Collections.emptyList(), true);
        String hmac = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("91f27e2a84a9804b101257a2edfd121b02917e1a".equals(hmac));

        mapper = new Mapper("DS.Crypto.hmac(\"HelloWorld\", \"PortX rules!\", \"HmacSHA256\")", Collections.emptyList(), true);
        hmac = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("7854220ef827b07529509f68f391a80bf87fff328dbda140ed582520a1372dc1".equals(hmac));

        mapper = new Mapper("DS.Crypto.hmac(\"HelloWorld\", \"PortX rules!\", \"HmacSHA512\")", Collections.emptyList(), true);
        hmac = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("0acbc9c5d0828f3bc9c30e311311073089f969d7ccbfacc457c8da289bc163fee911f75b3b018b2d0c7a09e758b970fcdc6b6488118fd52dbbf31b9ee0415d4c".equals(hmac));

        try {
            mapper = new Mapper("DS.Crypto.hash(\"HelloWorld\", \"DUMMY\")", Collections.emptyList(), true);
            hmac = mapper.transform("{}").replaceAll("\"", "");
            fail("This should fail with NoSuchAlgorithmException");
        } catch (Exception e) {
            String msg = e.getMessage();
            assertTrue(msg != null && msg.contains("Caused by: java.security.NoSuchAlgorithmException: DUMMY MessageDigest not available"));
        }
    }

    @Test
    void testEncrypt() {
        Mapper mapper = new Mapper("DS.Crypto.encrypt(\"HelloWorld\", \"DataSonnet123\")", Collections.emptyList(), true);
        String encrypted = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("HdK8opktKiK3ero0RJiYbA==".equals(encrypted));

        mapper = new Mapper("DS.Crypto.decrypt(\"HdK8opktKiK3ero0RJiYbA==\", \"DataSonnet123\")", Collections.emptyList(), true);
        String decrypted = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("HelloWorld".equals(decrypted));
    }
}
