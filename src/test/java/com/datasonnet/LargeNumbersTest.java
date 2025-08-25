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

import com.datasonnet.document.DefaultDocument;
import com.datasonnet.document.Document;
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LargeNumbersTest {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Test
    void testLargeNumberHandling() {
        Mapper mapper = new Mapper("payload");
        Document<Long> mapped = mapper.transform(new DefaultDocument<String>("102506060000000002", MediaTypes.APPLICATION_JSON), new HashMap(), MediaTypes.APPLICATION_JAVA, java.lang.Long.class);
        assertEquals("102506060000000002", mapped.getContent().toString());
    }


}