package com.datasonnet;

import com.datasonnet.Mapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ZonedDateTimeTest {

    @Test
    void testOffset() {
        Mapper mapper = new Mapper("DS.ZonedDateTime.offset(\"2019-07-22T21:00:00Z\", \"P1Y1D\")", Collections.emptyList(), true);
        String offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2020-07-23T21:00:00Z".equals(offsetDate));
//        System.out.println("Offset date is " + offsetDate);
    }

    @Test
    void testNow() {
        Instant before = Instant.now();

        Mapper mapper = new Mapper("DS.ZonedDateTime.now()", Collections.emptyList(), true);
        // getting rid of quotes so the Instant parser works
        Instant mapped = Instant.parse(mapper.transform("{}").replaceAll("\"", ""));

        Instant after = Instant.now();

        assertTrue(before.compareTo(mapped) <= 0);
        assertTrue(after.compareTo(mapped) >= 0);
    }

    @Test
    void testFormat() {
        Mapper mapper = new Mapper("DS.ZonedDateTime.format(\"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\", \"d MMM uuuu\")", Collections.emptyList(), true);
        String formattedDate = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("4 Jul 2019".equals(formattedDate));
    }

    @Test
    void testCompare() {
        Mapper mapper = new Mapper("DS.ZonedDateTime.compare(\"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\", \"2019-07-04T21:00:00Z\", \"yyyy-MM-dd'T'HH:mm:ssVV\")", Collections.emptyList(), true);
        String compareResult = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("0".equals(compareResult));
    }

    @Test
    void testTimezone() {
        Mapper mapper = new Mapper("DS.ZonedDateTime.changeTimeZone(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\", \"America/Los_Angeles\")", Collections.emptyList(), true);
        String newTimezone = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("New date is " + newTimezone);
        assertTrue("2019-07-04T19:00:00-0700".equals(newTimezone));
    }

    @Test
    void testLocalDT() {
        Mapper mapper = new Mapper("DS.ZonedDateTime.toLocalDate(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\")", Collections.emptyList(), true);
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2019-07-04".equals(newDate));

        mapper = new Mapper("DS.ZonedDateTime.toLocalTime(\"2019-07-04T21:00:00-0500\", \"yyyy-MM-dd'T'HH:mm:ssZ\")", Collections.emptyList(), true);
        String newTime = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("21:00:00".equals(newTime));

    }
}
