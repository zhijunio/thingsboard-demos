package org.example.thingsboard.mqttdemo;

public interface TransportServiceCallback<T> {

    void onSuccess(T value);

    void onError(Throwable error);
}
