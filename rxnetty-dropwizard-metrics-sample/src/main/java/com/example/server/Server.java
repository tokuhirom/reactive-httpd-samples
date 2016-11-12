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

@lombok.extern.slf4j.Slf4j
public class Server {
    public static void main(String[] args) {
        RxNetty.useMetricListenersFactory(new SpectatorEventsListenerFactory());

        MetricRegistry metricRegistry = new MetricRegistry();
        MetricsRegistry registry = new MetricsRegistry(Clock.SYSTEM, metricRegistry);
        Spectator.globalRegistry().add(registry);

        JmxReporter.forRegistry(metricRegistry).build().start();
        RequestHandler<ByteBuf, ByteBuf> handler = new RxNettyAccessLogFilter((request, response) -> {
//                try {
//            log.info("Sleeping");
//                    Thread.sleep(100000);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            response.writeString("Hello");
            return response.close();
        }, request -> "-", log::info);
        handler  = new StatsFilter(handler, metricRegistry);
        HttpServer<ByteBuf, ByteBuf> hello = RxNetty.createHttpServer(8080, handler);
        hello.startAndWait();
    }
}
