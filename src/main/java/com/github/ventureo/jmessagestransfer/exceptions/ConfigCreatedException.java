package com.github.ventureo.jmessagestransfer.exceptions;

public class ConfigCreatedException extends Exception {
    public ConfigCreatedException() {
        super();
    }

    public ConfigCreatedException(String message) {
        super(message);
    }

    public ConfigCreatedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigCreatedException(Throwable cause) {
        super(cause);
    }
}
