package com.datasonnet.plugins.jackson;

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import javax.xml.bind.JAXBElement;
import java.io.IOException;

public class JAXBElementSerializer extends StdSerializer<JAXBElement> {

    public JAXBElementSerializer() {
        this(null);
    }

    public JAXBElementSerializer(Class<JAXBElement> t) {
        super(t);
    }

    @Override
    public void serialize(JAXBElement value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeObjectField("name", value.getName().toString());
        gen.writeStringField("declaredType", value.getDeclaredType().getName());
        gen.writeObjectField("value", value.getValue());
        gen.writeEndObject();
    }
}
