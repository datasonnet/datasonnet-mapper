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
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZonedDateTimeTest {

    @Test
    void testOffset() {
        Mapper mapper = new Mapper("ds.datetime.offset(\"2019-07-22T21:00:00Z\", \"P1Y1D\")");
        String offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2020-07-23T21:00:00Z".equals(offsetDate));
//        System.out.println("Offset date is " + offsetDate);
    }

    @Test
    void testNow() {
        Instant before = Instant.now();

        Mapper mapper = new Mapper("ds.datetime.now()");
        // getting rid of quotes so the Instant parser works
        Instant mapped = Instant.parse(mapper.transform("{}").replaceAll("\"", ""));

        Instant after = Instant.now();

        assertTrue(before.compareTo(mapped) <= 0);
        assertTrue(after.compareTo(mapped) >= 0);
    }

    @Test
    void testFormat() {
        Mapper mapper = new Mapper("ds.datetime.format(\"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\", \"d MMM uuuu\")");
        String formattedDate = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("4 Jul 2019".equals(formattedDate));
    }

    @Test
    void testCompare() {
        Mapper mapper = new Mapper("ds.datetime.compare(\"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\", \"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\")");
        String compareResult = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("0".equals(compareResult));
    }

    @Test
    void testTimezone() {
        Mapper mapper = new Mapper("ds.datetime.changeTimeZone(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\", \"America/Los_Angeles\")");
        String newTimezone = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("New date is " + newTimezone);
        assertTrue("2019-07-04T19:00:00-0700".equals(newTimezone));
    }

    @Test
    void testLocalDT() {
        Mapper mapper = new Mapper("ds.datetime.toLocalDate(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2019-07-04".equals(newDate));

        mapper = new Mapper("ds.datetime.toLocalTime(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\")");
        String newTime = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("21:00:00".equals(newTime));

    }
}
