package com.datasonnet.badgerfish;

import javax.xml.stream.XMLStreamException;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import static javax.xml.stream.XMLStreamReader.*;

public class XMLEventPipe {

    private final XMLStreamReader2 reader;
    private final XMLStreamWriter2 writer;

    public XMLEventPipe(XMLStreamReader2 reader, XMLStreamWriter2 writer) {
        this.reader = reader;
        this.writer = writer;
    }

    public void pipe() throws XMLStreamException {
        for(int event = reader.getEventType(); reader.hasNext(); event = reader.next()) {
            if(event == START_DOCUMENT) {
                writer.writeStartDocument(writer.getEncoding(), reader.getVersion());
            } else {
                writer.copyEventFromReader(reader, true);
            }
        }
        // and the last event
        writer.copyEventFromReader(reader, true);
    }
}
