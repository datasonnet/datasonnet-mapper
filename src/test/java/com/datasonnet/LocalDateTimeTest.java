package com.datasonnet;

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

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalDateTimeTest {

    @Test
    void testOffset() {
        Mapper mapper = new Mapper("ds.localdatetime.offset(\"2019-07-22T21:00:00\", \"P1Y1D\")");
        String offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2020-07-23T21:00:00".equals(offsetDate));
//        System.out.println("Offset date is " + offsetDate);
    }

    @Test
    void testNow() {
        Instant before = Instant.now();

        Mapper mapper = new Mapper("ds.localdatetime.now()");
        // getting rid of quotes so the Instant parser works
        String mapped = mapper.transform("{}").replaceAll("\"", "");
        String today = java.time.LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        //Removed time portion
        mapped = mapped.substring(0, mapped.lastIndexOf("T"));
        today = today.substring(0, today.lastIndexOf("T"));
        assertTrue(today.equals(mapped));
    }

    @Test
    void testFormat() {
        Mapper mapper = new Mapper("ds.localdatetime.format(\"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\", \"d MMM uuuu\")");
        String formattedDate = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("4 Jul 2019".equals(formattedDate));
    }

    @Test
    void testCompare() {
        Mapper mapper = new Mapper("ds.localdatetime.compare(\"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\", \"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\")");
        String compareResult = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("0".equals(compareResult));
    }

}
