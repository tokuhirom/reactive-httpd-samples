package com.example;

import lombok.Data;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

import java.util.Map;

public class ExchangeConfig {
    public ExchangeRateApi buildExchangeRateApi() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor()
                        .setLevel(HttpLoggingInterceptor.Level.BASIC))
                .followRedirects(true)
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://api.fixer.io")
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(httpClient)
                .build();

        return retrofit.create(ExchangeRateApi.class);
    }

    public interface ExchangeRateApi {
        String URL = "/latest";

        @GET(URL)
        Observable<ExchangeRateResponse> getExchangeRate(@Query("base") String base,
                                                         @Query("symbols") String symbols);
    }

    @Data
    public static class ExchangeRateResponse {
        private String base;
        private String date;
        private Map<String, Double> rates;
    }

}
