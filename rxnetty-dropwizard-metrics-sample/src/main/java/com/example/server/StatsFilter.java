package com.example.server;

import com.codahale.metrics.MetricRegistry;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.protocol.http.server.HttpServerRequest;
import io.reactivex.netty.protocol.http.server.HttpServerResponse;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import rx.Observable;

public class StatsFilter implements RequestHandler<ByteBuf, ByteBuf> {
    private RequestHandler<ByteBuf, ByteBuf> next;
    private String prefix;
    private MetricRegistry metricRegistry;

    public StatsFilter(RequestHandler<ByteBuf, ByteBuf> next,  MetricRegistry metricRegistry) {
        this(next, metricRegistry, "response_status_");
    }

    public StatsFilter(RequestHandler<ByteBuf, ByteBuf> next, MetricRegistry metricRegistry, String prefix) {
        this.next = next;
        this.prefix = prefix;
        this.metricRegistry = metricRegistry;
    }

    @Override
    public Observable<Void> handle(HttpServerRequest<ByteBuf> request, HttpServerResponse<ByteBuf> response) {
        Observable<Void> observable = next.handle(request, response);
        int status = response.getStatus().code() / 100 * 100;
        metricRegistry.counter(prefix + status)
                .inc();
        return observable;
    }
}
