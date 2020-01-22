package com.datasonnet;

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
        Mapper mapper = new Mapper("DS.LocalDateTime.offset(\"2019-07-22T21:00:00\", \"P1Y1D\")", Collections.emptyList(), true);
        String offsetDate = mapper.transform("{}").replaceAll("\"", "");
        assertTrue("2020-07-23T21:00:00".equals(offsetDate));
//        System.out.println("Offset date is " + offsetDate);
    }

    @Test
    void testNow() {
        Instant before = Instant.now();

        Mapper mapper = new Mapper("DS.LocalDateTime.now()", Collections.emptyList(), true);
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
        Mapper mapper = new Mapper("DS.LocalDateTime.format(\"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\", \"d MMM uuuu\")", Collections.emptyList(), true);
        String formattedDate = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("4 Jul 2019".equals(formattedDate));
    }

    @Test
    void testCompare() {
        Mapper mapper = new Mapper("DS.LocalDateTime.compare(\"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\", \"2019-07-04T21:00:00\", \"yyyy-MM-dd'T'HH:mm:ss\")", Collections.emptyList(), true);
        String compareResult = mapper.transform("{}").replaceAll("\"", "");
        //System.out.println("Formatted date is " + formattedDate);
        assertTrue("0".equals(compareResult));
    }

}
