package com.example.server;

import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.netflix.spectator.api.Clock;
import com.netflix.spectator.api.Spectator;
import com.netflix.spectator.metrics3.MetricsRegistry;
import io.netty.buffer.ByteBuf;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.protocol.http.server.HttpServer;
import io.reactivex.netty.protocol.http.server.RequestHandler;
import io.reactivex.netty.spectator.SpectatorEventsListenerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.io.IOException;

@lombok.extern.slf4j.Slf4j
public class Server {
    public static void main(String[] args) {
        RxNetty.useMetricListenersFactory(new SpectatorEventsListenerFactory());

        MetricRegistry metricRegistry = new MetricRegistry();
        MetricsRegistry registry = new MetricsRegistry(Clock.SYSTEM, metricRegistry);
        Spectator.globalRegistry().add(registry);

        JmxReporter.forRegistry(metricRegistry).build().start();

        Logger accessLogLogger = LoggerFactory.getLogger("access_log");
        RequestHandler<ByteBuf, ByteBuf> handler = new RxNettyAccessLogFilter((request, response) -> {
            response.writeString("Hello");
            return response.close();
        }, request -> "-", accessLogLogger::info);
        handler  = new StatsFilter(handler, metricRegistry);
        HttpServer<ByteBuf, ByteBuf> hello = RxNetty.createHttpServer(8080, handler).withErrorHandler(throwable -> {
            if (throwable instanceof IOException && "Connection reset by peer".equals(throwable.getMessage())) {
                // Connection reset by peer is not a critial issue.
                log.info("Connection reset by peer");
            } else {
                log.error("Error", throwable);
            }
            return Observable.error(throwable); // If we do not return an error observable then the error is swallowed.
        });
        hello.startAndWait();
    }
}
