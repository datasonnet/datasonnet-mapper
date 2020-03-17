package com.datasonnet.spi;

public class PluginException extends Exception {
    public PluginException(String message) {
        super(message);
    }
    public PluginException(Throwable e) {
        super(e);
    }
    public PluginException(String message, Throwable e) {
        super(message, e);
    }
}
