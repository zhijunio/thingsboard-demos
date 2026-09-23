package org.example.thingsboard.mqttdemo.adaptors;

public final class AdaptorException extends Exception {

    public AdaptorException(String message, Throwable cause) {
        super(message, cause);
    }

    public AdaptorException(String message) {
        super(message);
    }
}
