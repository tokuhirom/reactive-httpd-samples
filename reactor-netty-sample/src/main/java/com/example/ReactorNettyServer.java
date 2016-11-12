package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.ipc.netty.http.HttpServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class ReactorNettyServer {
    public static void main(String[] args) {
        HttpServer.create(8011)
                .get("/hi", httpChannel -> httpChannel.sendString(Mono.just("Hi!")))
                .get("/500", httpChannel -> {
                    throw new RuntimeException("ORZ");
                })
                .post("/hello", httpChannel -> {
                    ObjectMapper objectMapper = new ObjectMapper();
                    return httpChannel
                            .header("Content-Type", "application/json")
                            .sendString(
                                    httpChannel.receive()
                                            .take(1)
                                            .flatMap(it -> {
                                                log.info("YAAAAAAAAAAAAAAAAAAA");
                                                String json = it.toString(StandardCharsets.UTF_8);
                                                try {
                                                    Hello hello = objectMapper.readValue(json, Hello.class);
                                                    return Mono.just("Hello, " + hello.getName());
                                                } catch (IOException e) {
                                                    return Mono.error(e);
                                                }
                                            })
                            );
                })
                .post("/mono-error-stuck", httpChannel -> httpChannel
                        .header("Content-Type", "application/json")
                        .sendString(Mono.error(new IllegalArgumentException())))
                .start(httpChannel -> httpChannel.status(HttpResponseStatus.NOT_FOUND)
                        .header("Content-type", "application/json")
                        .sendString(Mono.just("{\"message\":\"Not found\"}")))
                .log("httpd")
                .block();
        // start method accepts default http handler.
    }

    @Data
    public static class Hello {
        String name;
    }
}
