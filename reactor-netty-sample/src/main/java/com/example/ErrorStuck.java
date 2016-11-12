package com.example;

import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.util.concurrent.TimeoutException;

public class ErrorStuck {
    public static void main(String[] args) throws TimeoutException {
        // This code stucks.
        HttpServer.create(8011)
                .get("/mono-error-stuck", httpChannel -> httpChannel
                        .header("Content-Type", "application/json")
                        .sendString(Mono.error(new IllegalArgumentException())))
                .startAndAwait();
    }
}
