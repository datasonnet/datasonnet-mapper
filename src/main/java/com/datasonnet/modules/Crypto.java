package com.datasonnet.modules;

/*-
 * Copyright 2019-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.BlowfishEngine;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;


public class Crypto {

    public static String encrypt(String value, String keyString)
            throws Exception {
        BlowfishEngine engine = new BlowfishEngine();
        PaddedBufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher(engine);
        KeyParameter key = new KeyParameter(keyString.getBytes());
        cipher.init(true, key);
        byte[] in = value.getBytes();
        byte[] out = new byte[cipher.getOutputSize(in.length)];
        int len1 = cipher.processBytes(in, 0, in.length, out, 0);
        try {
            cipher.doFinal(out, len1);
        } catch (CryptoException e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }
        String s = new String(Base64.getEncoder().encode(out));
        return s;
    }

    public static String decrypt(String value, String keyString)
            throws Exception {
        BlowfishEngine engine = new BlowfishEngine();
        PaddedBufferedBlockCipher cipher =
                new PaddedBufferedBlockCipher(engine);
        StringBuffer result = new StringBuffer();
        KeyParameter key = new KeyParameter(keyString.getBytes());
        cipher.init(false, key);
        byte[] out = Base64.getDecoder().decode(value);
        byte[] out2 = new byte[cipher.getOutputSize(out.length)];
        int len2 = cipher.processBytes(out, 0, out.length, out2, 0);
        cipher.doFinal(out2, len2);
        String s2 = new String(out2);
        for (int i = 0; i < s2.length(); i++) {
            char c = s2.charAt(i);
            if (c != 0) {
                result.append(c);
            }
        }

        return result.toString();
    }

    /*
        Algorithm can be one of MD2, MD5, SHA-1, SHA-256, SHA-384, SHA-512
     */
    public static String hash(String value, String algorithm) throws Exception {
        MessageDigest digest = MessageDigest.getInstance(algorithm);
        byte[] hash = digest.digest(
                value.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hash), StandardCharsets.UTF_8);
    }

    /*
        Algorithm can be HmacSHA1, HmacSHA256 or HmacSHA512
     */
    public static String hmac(String value, String secret, String algorithm) throws Exception {
        SecretKeySpec signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm);
        Mac mac = Mac.getInstance(algorithm);
        mac.init(signingKey);
        byte[] hmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        return new String(Hex.encode(hmac), StandardCharsets.UTF_8);
    }
}
