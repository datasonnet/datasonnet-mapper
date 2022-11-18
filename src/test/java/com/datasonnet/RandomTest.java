package com.datasonnet;

/*-
 * Copyright 2019-2022 the original author or authors.
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
import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class RandomTest {

    @Test
    void testUUID() {
        Mapper mapper = new Mapper("ds.random.uuid()");
        String uuidString = mapper.transform("{}").replaceAll("\"", "");
        //Not much we can do about testing the UUID value,
        // but if UUID is valid, there will be no exception and variant will be 2
        UUID uuid = UUID.fromString(uuidString);
        assertEquals(2, uuid.variant());
    }

    @Test
    void testRandomInt() {
        Mapper mapper = new Mapper("ds.random.randomInt()");
        String randomInt = mapper.transform("{}").replaceAll("\"", "");
        try {
            Integer.valueOf(randomInt);
        } catch (NumberFormatException e) {
            fail("Unable to parse value '" + randomInt + "'", e);
        }

        mapper = new Mapper("ds.random.randomInt(10, 20)");
        randomInt = mapper.transform("{}").replaceAll("\"", "");
        try {
            int number = Integer.valueOf(randomInt);
            assertTrue(10 <= number && 20 >= number);
        } catch (NumberFormatException e) {
            fail("Unable to parse value '" + randomInt + "'", e);
        }
    }

    @Test
    void testRandomDouble() {
        Mapper mapper = new Mapper("ds.random.randomDouble()");
        String randomDouble = mapper.transform("{}").replaceAll("\"", "");
        try {
            Double.valueOf(randomDouble);
        } catch (NumberFormatException e) {
            fail("Unable to parse value '" + randomDouble + "'", e);
        }

        mapper = new Mapper("ds.random.randomDouble(10, 20)");
        randomDouble = mapper.transform("{}").replaceAll("\"", "");
        try {
            double number = Double.valueOf(randomDouble);
            assertTrue(10 <= number && 20 >= number);
        } catch (NumberFormatException e) {
            fail("Unable to parse value '" + randomDouble + "'", e);
        }
    }

    @Test
    void testRandomString() {
        Mapper mapper = new Mapper(" { " +
                    "randomStrLen: ds.random.randomString(10)," +
                    "randomStrAlpha: ds.random.randomString(10, true, false, false)," +
                    "randomStrNum: ds.random.randomString(10, false, true, false)," +
                    "randomStrOther: ds.random.randomString(10, false, false, true)" +
                "}");

        Document<String> data = new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON);
        Document<Map> transformResult = mapper.transform(data, new HashMap<>(), MediaTypes.APPLICATION_JAVA, Map.class);
        Map<String, String> stringsMap = transformResult.getContent();

        assertTrue(stringsMap.get("randomStrLen").length() == 10);
        assertTrue(stringsMap.get("randomStrAlpha").matches("^[A-Za-z]+$"));
        assertTrue(stringsMap.get("randomStrNum").matches("^[\\d]+$"));
        assertTrue(stringsMap.get("randomStrOther").matches("^[^A-Za-z\\d]+$"));
    }
}
