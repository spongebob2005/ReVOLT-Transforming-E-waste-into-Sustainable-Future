package com.example.revolt.data;

import android.content.Context;

import androidx.annotation.Nullable;

import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Builds the Retrofit client, rebuilding it whenever the server address changes. */
public final class ApiClient {
    private static ApiService service;
    private static String serviceBaseUrl;

    private ApiClient() {
    }

    public static synchronized ApiService get(Context context) {
        SessionManager session = SessionManager.get(context);
        String baseUrl = session.getServerUrl();
        if (service == null || !baseUrl.equals(serviceBaseUrl)) {
            service = create(baseUrl, session);
            serviceBaseUrl = baseUrl;
        }
        return service;
    }

    /** A one-off client for checking an address before saving it. */
    public static ApiService forUrl(Context context, String baseUrl) {
        return create(baseUrl, SessionManager.get(context));
    }

    private static ApiService create(String baseUrl, SessionManager session) {
        OkHttpClient http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(chain -> {
                    Request.Builder request = chain.request().newBuilder();
                    String token = session.getToken();
                    if (token != null) request.header("Authorization", "Bearer " + token);
                    return chain.proceed(request.build());
                })
                .build();

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(http)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService.class);
    }

    /** Turns a server-relative path such as "/uploads/abc.jpg" into a full URL. */
    @Nullable
    public static String absoluteUrl(Context context, @Nullable String path) {
        if (path == null) return null;
        HttpUrl base = HttpUrl.parse(SessionManager.get(context).getServerUrl());
        if (base == null) return null;
        HttpUrl resolved = base.resolve(path.startsWith("/") ? path.substring(1) : path);
        return resolved == null ? null : resolved.toString();
    }
}
