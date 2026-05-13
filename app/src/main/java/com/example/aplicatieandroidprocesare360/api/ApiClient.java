package com.example.aplicatieandroidprocesare360.api;

import android.content.Context;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static Retrofit processingRetrofit;
    private static String   currentProcessingUrl;
    private static String   authToken;

    /** Call once at app startup to restore a previously saved JWT. */
    public static void loadToken(Context context) {
        authToken = context.getSharedPreferences("panorama_prefs", Context.MODE_PRIVATE)
                .getString("api_token", null);
    }

    /** Persist a new JWT and rebuild the client so the header is picked up immediately. */
    public static void saveToken(Context context, String token) {
        authToken = token;
        context.getSharedPreferences("panorama_prefs", Context.MODE_PRIVATE)
                .edit().putString("api_token", token).apply();
        processingRetrofit = null;
    }

    public static String getAuthToken() { return authToken; }

    private static OkHttpClient buildClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    okhttp3.Request.Builder req = chain.request().newBuilder();
                    if (authToken != null) {
                        req.addHeader("Authorization", "Bearer " + authToken);
                    }
                    return chain.proceed(req.build());
                })
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

    /**
     * Normalizes the server URL entered by the user so it always ends with /api/.
     * Input examples:  "192.168.1.5", "http://192.168.1.5", "http://192.168.1.5:80"
     * Output:          "http://192.168.1.5/api/"
     * If the URL already ends with /api or /api/, it is left unchanged (plus trailing slash).
     */
    public static String normalizeApiUrl(String url) {
        if (url == null || url.isEmpty()) return url;
        if (!url.startsWith("http")) url = "http://" + url;
        url = url.replaceAll("/+$", "");
        if (!url.endsWith("/api")) {
            try {
                java.net.URL parsed = new java.net.URL(url);
                url = parsed.getProtocol() + "://" + parsed.getAuthority() + "/api";
            } catch (Exception ignored) {
                url = url + "/api";
            }
        }
        return url + "/";
    }

    public static void clearToken(Context context) {
        authToken = null;
        context.getSharedPreferences("panorama_prefs", Context.MODE_PRIVATE)
                .edit().remove("api_token").apply();
        processingRetrofit = null;
    }

    public static void resetProcessingClient() {
        processingRetrofit   = null;
        currentProcessingUrl = null;
    }
}
