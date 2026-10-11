package com.example.revolt.util;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.SessionManager;
import com.example.revolt.databinding.DialogServerBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Map;

import okhttp3.HttpUrl;

/** Lets the user point the app at their backend (emulator vs. phone on the same Wi-Fi). */
public final class ServerDialog {
    private ServerDialog() {
    }

    public static void show(Context context, @Nullable Runnable onSaved) {
        SessionManager session = SessionManager.get(context);
        DialogServerBinding b = DialogServerBinding.inflate(LayoutInflater.from(context));
        b.inputUrl.setText(session.getServerUrl());

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.server_settings)
                .setIcon(R.drawable.ic_tune)
                .setView(b.getRoot())
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.server_test, null)
                .create();

        dialog.setOnShowListener(d -> {
            Button save = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button test = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

            save.setOnClickListener(v -> {
                String url = validUrl(b.inputUrl.getText());
                if (url == null) {
                    b.layoutUrl.setError(context.getString(R.string.server_invalid));
                    return;
                }
                session.setServerUrl(url);
                dialog.dismiss();
                if (onSaved != null) onSaved.run();
            });

            test.setOnClickListener(v -> {
                String url = validUrl(b.inputUrl.getText());
                if (url == null) {
                    b.layoutUrl.setError(context.getString(R.string.server_invalid));
                    return;
                }
                b.layoutUrl.setError(null);
                b.layoutUrl.setHelperText(context.getString(R.string.server_testing));
                test.setEnabled(false);
                ApiClient.forUrl(context, url).health().enqueue(new ApiCallback<Map<String, Object>>(context) {
                    @Override
                    public void onSuccess(@NonNull Map<String, Object> body) {
                        test.setEnabled(true);
                        b.layoutUrl.setHelperText(context.getString(R.string.server_ok));
                    }

                    @Override
                    public void onError(@NonNull String message) {
                        test.setEnabled(true);
                        b.layoutUrl.setHelperText(null);
                        b.layoutUrl.setError(message);
                    }
                });
            });
        });
        dialog.show();
    }

    @Nullable
    private static String validUrl(@Nullable CharSequence text) {
        if (text == null || text.toString().trim().isEmpty()) return null;
        String url = SessionManager.normalizeUrl(text.toString());
        return HttpUrl.parse(url) == null ? null : url;
    }
}
