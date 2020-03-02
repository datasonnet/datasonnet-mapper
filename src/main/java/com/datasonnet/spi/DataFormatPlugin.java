package com.datasonnet.spi;

import com.datasonnet.document.Document;

import java.util.Map;

public interface DataFormatPlugin<T> {
    // TODO needs to take a Document on read, but _any_ Document... I think?
    // Oh this is interesting. Okay, so, we want plugins to be able to choose how they handle things...
    // but we also need to be able to express partial incompatibilities
    // Okay, let me see. We need to be able to work with different types and because Java doesn't
    // allow that sort of dynamic type-based dispatch, that means taking a Document makes the most sense.
    // Hrmmm, we _don't_ actually care what the fundamental underlying thing in the document is, only
    // what ways we can get it, really

    // actually, that's also true on output!!!
    // okay, so let me see...
    // I think we really want a combo of supportsString/getString for a bunch of types...

    // final output is a bit different, though. We need to know more coherently, what is this? Or rather,
    // what should it be returned as?

    // that's also useful on intake, but mostly with the darn Object/not Object boundary.
    // okay, so I've figured out we need: Object, File, Bytes, String (and the middle two not yet).
    // That leaves the question, which end governs the connections between those three?
    // That is, does a converter say "give me a file, no matter what" and we make it a file or throw an error?
    // or do we sometimes give it a string. I think the former sounds good?
    // okay, so now we've got java type erasure to deal with
    ujson.Value read(T input, Map<String, Object> params) throws PluginException;
    Document<T> write(ujson.Value input, Map<String, Object> params, String mimeType) throws PluginException;

    String[] getSupportedIdentifiers();

    Map<String, String> getReadParameters();
    Map<String, String> getWriteParameters();

    String getPluginId();
}
