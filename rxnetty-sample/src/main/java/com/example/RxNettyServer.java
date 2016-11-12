package com.example;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpError;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import rx.Observable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RxNettyServer {
    private final ObjectMapper objectMapper;
    private int port;
    private ExchangeConfig.ExchangeRateApi exchangeRateApi;

    public static void main(String[] args) {
        ExchangeConfig exchangeConfig = new ExchangeConfig();
        int port = 8080;

        new RxNettyServer(port, exchangeConfig.buildExchangeRateApi())
                .doMain();
    }

    public RxNettyServer(int port, ExchangeConfig.ExchangeRateApi exchangeRateApi) {
        this.port = port;
        this.exchangeRateApi = exchangeRateApi;
        this.objectMapper = new ObjectMapper();
    }

    private void doMain() {
        RxNetty
                .createHttpServer(port, this::handle)
                .withErrorResponseGenerator(this::errorResponseGenerator)
                .startAndWait();
    }

    private Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        System.out.println("server => Request: " + request.getPath());
        log.info("has content: {}",
                request.getHeaders().hasContent());
        for (String name : request.getHeaders().names()) {
            log.info("{}: {}", name, request.getHeaders().getHeader(name));
        }

        switch (request.getPath()) {
            case "/error":
                throw new RuntimeException("forced error");
            case "/json":
                return parseJson(request, Hello.class)
                        .flatMap(it -> {
                            log.info("Finished reading content: {}", it);
                            response.writeString("Path Requested =>: " + request.getPath() + '\n');
                            return response.close();
                        });
            case "/retrofit2":
                return exchangeRateApi
                        .getExchangeRate("USD", "JPY")
                        .flatMap(this::asJson)
                        .flatMap(
                                outputJson -> {
                                    response.setStatus(HttpResponseStatus.OK);
                                    response.getHeaders().addHeader("Content-Type", "application/json");
                                    response.writeBytes(outputJson);
                                    return response.close();
                                }
                        );
            default:
                response.setStatus(HttpResponseStatus.NOT_FOUND);
                response.writeString("Not found");
                return response.close();
        }
    }

    private <T> Observable<byte[]> asJson(T object) {
        try {
            return Observable.just(objectMapper.writeValueAsBytes(object));
        } catch (IOException e) {
            return Observable.error(e);
        }
    }

    private void errorResponseGenerator(HttpServerResponse<ByteBuf> response, Throwable error) {
        if (error instanceof HttpError) {
            response.setStatus(((HttpError) error).getStatus());
        } else {
            log.error("Unexpected exception", error);
            response.setStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
        }
        response.getHeaders().set(HttpHeaderNames.CONTENT_TYPE, "application/json");

        ApiResponse apiResponse = new ApiResponse(
                error instanceof HttpError ? error.getMessage() : "Unexpected exception"
        );
        try {
            response.writeBytes(objectMapper.writeValueAsBytes(apiResponse));
        } catch (JsonProcessingException e) {
            response.writeString("{\"message\":\"Cannot generate error message\"}");
        }
    }

    @Data
    public static class Hello {
        String name;
    }

    @Data
    @NoArgsConstructor
    public static class ApiResponse {
        String message;

        public ApiResponse(String message) {
            this.message = message;
        }
    }

    private static <T> Observable<T> parseJson(HttpServerRequest<ByteBuf> request, Class<T> klass) {
        String contentType = request.getHeaders()
                .getHeader("Content-Type");
        if (contentType == null) {
            throw new RuntimeException("Missing content-type header");
        }

        if (!contentType.toLowerCase().startsWith("application/json")) {
            throw new RuntimeException("Required application/json");
        }

        ObjectMapper objectMapper = new ObjectMapper();

        return request.getContent()
                .map(byteBuf -> byteBuf.toString(StandardCharsets.UTF_8))
                .collect(StringBuilder::new, StringBuilder::append)
                .map(StringBuilder::toString)
                .flatMap(json -> {
                    try {
                        return Observable.just(objectMapper.readValue(json, klass));
                    } catch (IOException e) {
                        return Observable.error(new IllegalArgumentException("<xmp>"));
                    }
                });
    }
}
