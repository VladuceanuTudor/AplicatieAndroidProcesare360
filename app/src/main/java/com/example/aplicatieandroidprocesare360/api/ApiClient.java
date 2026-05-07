package com.example.aplicatieandroidprocesare360.api;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit processingRetrofit;
    private static String   currentProcessingUrl;

    private static OkHttpClient buildClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
                .build();
    }

    public static ProcessingService getProcessingService(String baseUrl) {
        if (!baseUrl.endsWith("/")) baseUrl = baseUrl + "/";
        if (processingRetrofit == null || !baseUrl.equals(currentProcessingUrl)) {
            currentProcessingUrl = baseUrl;
            processingRetrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(buildClient())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return processingRetrofit.create(ProcessingService.class);
    }

    public static void resetProcessingClient() {
        processingRetrofit   = null;
        currentProcessingUrl = null;
    }
}
