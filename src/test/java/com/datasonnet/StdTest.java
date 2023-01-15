package com.datasonnet;

/*-
 * Copyright 2019-2023 the original author or authors.
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
import com.datasonnet.util.TestResourceReader;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class StdTest {

    @Test
    void testStdGet() throws IOException, URISyntaxException, JSONException {
        Mapper mapper = new Mapper(TestResourceReader.readFileAsString("stdGet.ds"));
        Document<String>  response = mapper.transform(new DefaultDocument<>("{}", MediaTypes.APPLICATION_JSON));
        JSONAssert.assertEquals("{\"hidden\":\"HiddenMessage\",\"noHidden\":\"NONE\",\"obj\":{\"Hello\":\"World\"},\"nonExistent\":null,\"nonExistentD\":\"DefaultNonExistent\",\"a\":[1,2,3],\"b\":[9,8,7]}", response.getContent(), true);
    }
}
