package com.example.server;

import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.time.temporal.ChronoField.*;

public class RxNettyAccessLogFilter implements RequestHandler<ByteBuf, ByteBuf> {
    private final RequestHandler<ByteBuf, ByteBuf> next;
    private final Function<HttpServerRequest<ByteBuf>, String> userIdGenerator;
    private final Consumer<String> writer;

    private volatile CacheEntry cacheEntry;

    // 10/Oct/2000:13:55:36 -0700
    private static final DateTimeFormatter COMBINED_LOG_DATE_TIME_FORMATTER =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendValue(DAY_OF_MONTH, 2)
                    .appendLiteral("/")
                    .appendText(ChronoField.MONTH_OF_YEAR, TextStyle.SHORT)
                    .appendLiteral("/")
                    .appendValue(YEAR, 4)
                    .appendLiteral(":")
                    .appendValue(HOUR_OF_DAY, 2)
                    .appendLiteral(":")
                    .appendValue(MINUTE_OF_HOUR, 2)
                    .appendLiteral(":")
                    .appendValue(SECOND_OF_MINUTE, 2)
                    .appendLiteral(" ")
                    .appendOffset("+HHMMss", "Z")
                    .toFormatter();

    public RxNettyAccessLogFilter(RequestHandler<ByteBuf, ByteBuf> next, Function<HttpServerRequest<ByteBuf>, String> userIdGenerator, Consumer<String> writer) {
        this.next = next;
        this.userIdGenerator = userIdGenerator;
        this.writer = writer;
    }


    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        Observable<Void> observable = next.handle(request, response);
        writer.accept(renderLog(request, response, userIdGenerator));
        return observable;
    }

    public String renderLog(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response, Function<HttpServerRequest<ByteBuf>, String> userIdGenerator) {
        return getRemoteAddress(request) + " - - " + userIdGenerator.apply(request)
                + " [" + renderTimestamp() + "] \""
                + request.getHttpMethod()
                + " " + request.getUri()
                + " " + request.getHttpVersion()
                + "\" " + response.getStatus().code()
                + " " + response.getHeaders().getContentLength(0)
                + " \"" + headerOrDefault(request, "Referer", "-") + "\" \""
                + headerOrDefault(request, "User-Agent", "-") + "\"";
    }

    private String renderTimestamp() {
        long now = System.currentTimeMillis() / 1000;
        CacheEntry cache = cacheEntry;
        if (cache != null && cache.timestamp == now) {
            return cache.msg;
        } else {
            String msg = ZonedDateTime.now().format(COMBINED_LOG_DATE_TIME_FORMATTER);
            this.cacheEntry = new CacheEntry(now, msg);
            return msg;
        }
    }

    private static String getRemoteAddress(HttpServerRequest<ByteBuf> request) {
        SocketAddress socketAddress = request.getNettyChannel().remoteAddress();
        if (socketAddress instanceof InetSocketAddress) {
            return ((InetSocketAddress) socketAddress).getAddress()
                    .getHostAddress();
        } else {
            return socketAddress.toString();
        }
    }

    private static String headerOrDefault(HttpServerRequest<ByteBuf> request, String key, String defaultValue) {
        String s = request.getHeaders().get(key);
        return s != null ? s : defaultValue;
    }

    private static class CacheEntry {
        private final long timestamp;
        private final String msg;

        public CacheEntry(long timestamp, String msg) {
            this.timestamp = timestamp;
            this.msg = msg;
        }
    }
}
