package com.example.revolt.data;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.revolt.R;
import com.example.revolt.ui.auth.LoginActivity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Retrofit callback that turns failures into a readable message and sends the user back to
 * the login screen when their token is no longer valid.
 */
public abstract class ApiCallback<T> implements Callback<T> {
    private final Context appContext;

    protected ApiCallback(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public abstract void onSuccess(@NonNull T body);

    public abstract void onError(@NonNull String message);

    @Override
    public final void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
        T body = response.body();
        if (response.isSuccessful() && body != null) {
            onSuccess(body);
            return;
        }
        SessionManager session = SessionManager.get(appContext);
        if (response.code() == 401 && session.getToken() != null) {
            session.clear();
            Toast.makeText(appContext, R.string.error_session, Toast.LENGTH_LONG).show();
            Intent intent = new Intent(appContext, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            appContext.startActivity(intent);
        }
        onError(errorMessage(response.errorBody()));
    }

    @Override
    public final void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
        if (call.isCanceled()) return;
        onError(appContext.getString(R.string.error_network));
    }

    private String errorMessage(ResponseBody errorBody) {
        try {
            if (errorBody != null) {
                JsonObject json = JsonParser.parseString(errorBody.string()).getAsJsonObject();
                if (json.has("error")) return json.get("error").getAsString();
            }
        } catch (Exception ignored) {
            // Not our JSON error shape (e.g. a proxy page); fall through to the generic message.
        }
        return appContext.getString(R.string.error_generic);
    }
}
