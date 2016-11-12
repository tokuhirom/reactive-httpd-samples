package com.example.sample;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpMethod;
import io.reactivex.netty.RxNetty;
import io.reactivex.netty.pipeline.PipelineConfigurator;
import io.reactivex.netty.pipeline.PipelineConfigurators;
import io.reactivex.netty.protocol.http.client.HttpClient;
import io.reactivex.netty.protocol.http.client.HttpClientRequest;
import io.reactivex.netty.protocol.http.client.HttpClientResponse;
import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

@Slf4j
public class Client {
    public static void main(String[] args) throws MalformedURLException {
        PipelineConfigurator<HttpClientResponse<ByteBuf>, HttpClientRequest<String>> pipelineConfigurator
                = PipelineConfigurators.httpClientConfigurator();

        URL url = new URL("http://mixi.jp");

        HttpClient<String, ByteBuf> client = RxNetty.<String, ByteBuf>newHttpClientBuilder(
                url.getHost(),
                url.getPort() == -1 ? url.getDefaultPort() : url.getPort()
        ).pipelineConfigurator(pipelineConfigurator)
                .build();
//        HttpClient<String, ByteBuf> client = RxNetty.createHttpClient("mixi.jp", 80, pipelineConfigurator);

        HttpClientRequest<String> request = HttpClientRequest.create(HttpMethod.GET, "http://mixi.jp");

        String result = client.submit(request,
                HttpClient.HttpClientConfig.Builder.fromDefaultConfig()
                        .followRedirect(5)
                        .build())
                .flatMap(response -> {
                    log.info("{}", response.getStatus());
                    return response.getContent()
                            .map(byteBuf -> byteBuf.toString(Charset.defaultCharset()));
                })
                .toBlocking()
                .single();
        log.info("result: {}", result);

    }
}
