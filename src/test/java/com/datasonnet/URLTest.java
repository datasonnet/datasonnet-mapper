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
import com.datasonnet.document.MediaTypes;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class URLTest {

    @Test
    void testEncodeDecode() throws Exception {
        String data = "Hello World";
        String encodedData = "Hello+World";
        Mapper mapper = new Mapper("ds.url.encode(payload)");
        String result = mapper.transform(new DefaultDocument<String>(data, MediaTypes.TEXT_PLAIN), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();
        assertEquals(encodedData, result);
        mapper = new Mapper("ds.url.decode(payload)");
        result = mapper.transform(new DefaultDocument<String>(result, MediaTypes.TEXT_PLAIN), Collections.emptyMap(), MediaTypes.TEXT_PLAIN).getContent();
        assertEquals(data, result);
    }

}
