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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PeriodTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String lib = "ds";
    private final String pack = ".period";

    @Test
    void testPeriod_between(){
        Mapper mapper = new Mapper(lib + pack + ".between(\"2020-10-21T16:08:07.131Z\", \"2020-10-22T10:20:07.131Z\")");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1D", newDate );
    }

    @Test
    void testPeriod_days(){
        Mapper mapper = new Mapper(lib + pack + ".days(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1D", newDate );
    }

    @Test
    void testPeriod_duration(){
        Mapper mapper = new Mapper(lib + pack + ".duration({\"days\":2})");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT48H", newDate );

        mapper = new Mapper(lib + pack + ".duration({\"hours\":2})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT2H", newDate );

        mapper = new Mapper(lib + pack + ".duration({\"minutes\":2})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT2M", newDate );

        mapper = new Mapper(lib + pack + ".duration({\"seconds\":2})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT2S", newDate );

        mapper = new Mapper(lib + pack + ".duration({\"days\":1,\"hours\":2,\"minutes\":3,\"seconds\":4})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT26H3M4S", newDate );
    }

    @Test
    void testPeriod_hours(){
        Mapper mapper = new Mapper(lib + pack + ".hours(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT1H", newDate );
    }

    @Test
    void testPeriod_minutes(){
        Mapper mapper = new Mapper(lib + pack + ".minutes(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT1M", newDate );
    }

    @Test
    void testPeriod_months(){
        Mapper mapper = new Mapper(lib + pack + ".months(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1M", newDate );
    }

    @Test
    void testPeriod_period(){
        Mapper mapper = new Mapper(lib + pack + ".period({\"years\":1})");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1Y", newDate );

        mapper = new Mapper(lib + pack + ".period({\"months\":1})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1M", newDate );

        mapper = new Mapper(lib + pack + ".period({\"days\":1})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1D", newDate );

        mapper = new Mapper(lib + pack + ".period({\"years\":1,\"months\":2,\"days\":3})");
        newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1Y2M3D", newDate );
    }

    @Test
    void testPeriod_seconds(){
        Mapper mapper = new Mapper(lib + pack + ".seconds(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("PT1S", newDate );
    }

    @Test
    void testPeriod_years(){
        Mapper mapper = new Mapper(lib + pack + ".years(1)");
        String newDate = mapper.transform("{}").replaceAll("\"", "");
        assertEquals("P1Y", newDate );
    }
}
