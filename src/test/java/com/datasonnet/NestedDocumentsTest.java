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
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class NestedDocumentsTest {

    @Test
    public void testNestedDocs() {
        String xml = "<root/>";
        String json = "{ \"hello\": \"world!\" }";

        Map<String, Document<String>> nested = new HashMap<>(2);
        nested.put("xml", new DefaultDocument<>(xml, MediaTypes.APPLICATION_XML));
        nested.put("json", new DefaultDocument<>(json, MediaTypes.APPLICATION_JSON));

        Map<String, Document<?>> inputs = Collections.singletonMap("nested", new DefaultDocument<>(nested, MediaTypes.APPLICATION_JAVA));
        String result = new MapperBuilder("nested")
                .withInputNames("nested")
                .build()
                .transform(DefaultDocument.NULL_INSTANCE, inputs, MediaTypes.APPLICATION_JSON)
                .getContent();
        Assert.assertEquals("{\"json\":{\"hello\":\"world!\"},\"xml\":{\"root\":{\"$\":\"\",\"~\":1}}}", result);
    }
}
