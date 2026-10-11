package com.example.revolt.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.Nullable;

import com.example.revolt.R;
import com.example.revolt.data.model.AuthResponse;
import com.example.revolt.data.model.User;
import com.google.gson.Gson;

/** Stores the auth token, the signed-in user and the backend address. */
public final class SessionManager {
    private static final String PREFS = "revolt_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER = "user";
    private static final String KEY_SERVER = "server_url";

    private static SessionManager instance;

    private final SharedPreferences prefs;
    private final String defaultServerUrl;
    private final Gson gson = new Gson();

    private SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        defaultServerUrl = context.getString(R.string.default_server_url);
    }

    public static synchronized SessionManager get(Context context) {
        if (instance == null) instance = new SessionManager(context.getApplicationContext());
        return instance;
    }

    public boolean isLoggedIn() {
        return getToken() != null && getUser() != null;
    }

    @Nullable
    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    @Nullable
    public User getUser() {
        String json = prefs.getString(KEY_USER, null);
        return json == null ? null : gson.fromJson(json, User.class);
    }

    public void saveSession(AuthResponse auth) {
        prefs.edit()
                .putString(KEY_TOKEN, auth.token)
                .putString(KEY_USER, gson.toJson(auth.user))
                .apply();
    }

    public void updateUser(User user) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply();
    }

    public void clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).apply();
    }

    public String getServerUrl() {
        return prefs.getString(KEY_SERVER, defaultServerUrl);
    }

    public void setServerUrl(String url) {
        prefs.edit().putString(KEY_SERVER, normalizeUrl(url)).apply();
    }

    /** Adds a scheme and trailing slash so Retrofit accepts the URL. */
    public static String normalizeUrl(String raw) {
        String url = raw.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
        if (!url.endsWith("/")) url = url + "/";
        return url;
    }
}
