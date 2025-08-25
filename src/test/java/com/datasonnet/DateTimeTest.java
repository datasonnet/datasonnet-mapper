package com.datasonnet;

/*-
 * Copyright 2019-2025 the original author or authors.
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateTimeTest {

    @Test
    void testOffset() {
        Mapper mapper = new Mapper("ds.datetime.parse(\"2019-07-22\", \"yyyy-MM-dd\").plus(\"P1Y2M5D\").format(\"yyyy-MM-dd\")");
        String offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-09-27", offsetDate);

        mapper = new Mapper("ds.datetime.parse(\"2019-07-22\", \"yyyy-MM-dd\").plus(\"P1DT10H\").format(\"yyyy-MM-dd HH\")");
        offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2019-07-23 10", offsetDate);
    }

    @Test
    void testNow() throws Exception {
        ZonedDateTime before = ZonedDateTime.now();
        Thread.sleep(100);

        Mapper mapper = new Mapper("ds.datetime.now().toISO()");
        // getting rid of quotes so the Instant parser works
        ZonedDateTime mapped = ZonedDateTime.parse(mapper.transform("{}").replaceAll("\"", ""));

        Thread.sleep(100);
        ZonedDateTime after = ZonedDateTime.now();

        assertTrue(before.compareTo(mapped) <= 0);
        assertTrue(after.compareTo(mapped) >= 0);
    }

    @Test
    void testMilliseconds() throws Exception {
        Mapper mapper = new Mapper("ds.datetime.now().asMilliseconds()");
        Document<Long> mapped = mapper.transform(new DefaultDocument<String>("{}", MediaTypes.APPLICATION_JSON), new HashMap(), MediaTypes.APPLICATION_JAVA, java.lang.Long.class);
        Long mappedValue = mapped.getContent();

        DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
        Date today = new Date();
        Date mappedDate = new Date(Long.valueOf(mappedValue));

        assertEquals(simple.format(today), simple.format(mappedDate));
    }

    @Test
    void testFormat() {
        Mapper mapper = new Mapper("ds.datetime.parse(\"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssX\").format(\"d MMM uuuu\")");
        String formattedDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("4 Jul 2019", formattedDate);
    }

    @Test
    void testTimezone() {
        Mapper mapper = new Mapper("ds.datetime.parse(\"2019-07-04T21:00:00-05:00\", \"yyyy-MM-dd'T'HH:mm:ssXXX\").toTimeZone(\"America/Los_Angeles\").format(\"yyyy-MM-dd'T'HH:mm:ssXXX\")");
        String newTimezone = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2019-07-04T19:00:00-07:00", newTimezone);
    }

    @Test
    void testFromObject() {
        Mapper mapper = new Mapper("ds.datetime.fromObject({\"year\":2020}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-01-01T00:00:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"month\":12}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-12-01T00:00:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"day\":20}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-20T00:00:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"hour\":23}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T23:00:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"minute\":23}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:23:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"second\":23}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:23Z", newDate );

        /*
        mapper = new Mapper("ds.datetime.date({\"nanosecond\":1, \"second\": 1})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0000-01-01T00:00:00.555Z", newDate );*/

        mapper = new Mapper("ds.datetime.fromObject({\"zoneId\":\"UTC\"}).format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:00Z", newDate );

        mapper = new Mapper("ds.datetime.fromObject({\"zoneOffset\":\"+07:00\"}).format(\"yyyy-MM-dd'T'HH:mm:ssVV\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("0001-01-01T00:00:00+07:00", newDate );
    }

    @Test
    void testParse() {
        Mapper mapper = new Mapper("ds.datetime.parse(\"1577836800\", \"timestamp\").format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-01-01T00:00:00Z", newDate);

        mapper = new Mapper("ds.datetime.parse(\"1577836800\", \"epoch\").format(\"yyyy-MM-dd'T'HH:mm:ssX\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2020-01-01T00:00:00Z", newDate);

        mapper = new Mapper("ds.datetime.parse(\"12/31/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\").format(\"yyyy-MM-dd'T'HH:mm:ss\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("1990-12-31T10:10:10", newDate);

        mapper = new Mapper("ds.datetime.parse(\"2011-12-03T10:15:30+01:00\", \"iso\").format(\"yyyy-MM-dd\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2011-12-03", newDate);

        mapper = new Mapper("ds.datetime.parse(\"2011-12-03T10:15:30.123Z\").format(\"yyyy-MM-dd\")");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("2011-12-03", newDate);
    }

    @Test
    void testDaysBetween() {
        Mapper mapper = new Mapper("local myDate = ds.datetime.parse(\"01/01/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "local otherDate = ds.datetime.parse(\"12/31/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "myDate.daysBetween(otherDate)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("364", newDate);
    }

    @Test
    void testCompare() {
        Mapper mapper = new Mapper("local myDate = ds.datetime.parse(\"01/01/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "local otherDate = ds.datetime.parse(\"12/31/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "myDate.compare(otherDate)");
        assertEquals("-1", mapper.transform("{}"));

        mapper = new Mapper("local myDate = ds.datetime.parse(\"01/01/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "local otherDate = ds.datetime.parse(\"12/31/1980 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "myDate.compare(otherDate)");
        assertEquals("1", mapper.transform("{}"));

        mapper = new Mapper("local myDate = ds.datetime.parse(\"01/01/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "local otherDate = ds.datetime.parse(\"01/01/1990 10:10:10\", \"MM/dd/yyyy HH:mm:ss\");" +
                "myDate.compare(otherDate)");
        assertEquals("0", mapper.transform("{}"));
    }
}
