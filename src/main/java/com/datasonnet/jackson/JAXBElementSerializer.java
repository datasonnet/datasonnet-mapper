package com.datasonnet.jackson;

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
