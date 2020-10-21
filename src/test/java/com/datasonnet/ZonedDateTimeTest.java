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

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void testDateTime_atBeginningOfDay(){
        Mapper mapper = new Mapper("ds.datetime.atBeginningOfDay(\"2020-10-21T16:08:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-21T00:00:00.000Z", newDate );
    }

    @Test
    void testDateTime_atBeginningOfHour(){
        Mapper mapper = new Mapper("ds.datetime.atBeginningOfHour(\"2020-10-21T16:08:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-21T16:00:00.000Z", newDate );
    }

    @Test
    void testDateTime_atBeginningOfMonth(){
        Mapper mapper = new Mapper("ds.datetime.atBeginningOfMonth(\"2020-10-21T16:08:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-01T00:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.atBeginningOfMonth(\"2020-10-01T16:08:07.131Z\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-01T00:00:00.000Z", newDate );
    }

    @Test
    void testDateTime_atBeginningOfWeek(){
        Mapper mapper = new Mapper("ds.datetime.atBeginningOfWeek(\"2020-10-21T16:08:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-18T00:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.atBeginningOfWeek(\"2020-10-18T16:08:07.131Z\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-10-18T00:00:00.000Z", newDate );
    }

    @Test
    void testDateTime_atBeginningOfYear(){
        Mapper mapper = new Mapper("ds.datetime.atBeginningOfYear(\"2020-10-21T16:08:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-01-01T00:00:00.000Z", newDate );
    }

    @Test
    void testDateTime_date(){
        Mapper mapper = new Mapper("ds.datetime.date({\"year\":2020})");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-01-01T00:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.date({\"month\":12})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-12-01T00:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.date({\"day\":20})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-20T00:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.date({\"hour\":23})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T23:00:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.date({\"minute\":23})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:23:00.000Z", newDate );

        mapper = new Mapper("ds.datetime.date({\"second\":23})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:23.000Z", newDate );

        /*
        mapper = new Mapper("ds.datetime.date({\"nanosecond\":1, \"second\": 1})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:00.555Z", newDate );*/

        mapper = new Mapper("ds.datetime.date({\"timezone\":\"UTC\"})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:00.000UTC", newDate );
    }
}
