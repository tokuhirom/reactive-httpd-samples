package com.example.bug;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import rx.Observable;

public class XSS {
    public static void main(String[] args) {
        HttpServer<ByteBuf, ByteBuf> server = RxNetty.createHttpServer(8080, (request, response) -> {
            return Observable.error(new IllegalArgumentException("<script>alert(\"HELLO\");</script>"));
        });
        server.startAndWait();
    }
}

