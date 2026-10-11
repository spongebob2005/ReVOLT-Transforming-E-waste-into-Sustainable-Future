package com.example.revolt.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.revolt.R;
import com.example.revolt.data.ApiCallback;
import com.example.revolt.data.ApiClient;
import com.example.revolt.data.SessionManager;
import com.example.revolt.data.model.AuthResponse;
import com.example.revolt.databinding.ActivityRegisterBinding;
import com.example.revolt.ui.main.MainActivity;
import com.example.revolt.util.SystemBars;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ActivityRegisterBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.hero(this);

        b.header.title.setText(R.string.register_title);
        b.header.subtitle.setText(R.string.register_subtitle);

        b.btnRegister.setOnClickListener(v -> attemptRegister());
        b.inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptRegister();
                return true;
            }
            return false;
        });
        b.btnGoLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        boolean ok = AuthForm.name(b.layoutName)
                & AuthForm.email(b.layoutEmail)
                & AuthForm.phone(b.layoutPhone)
                & AuthForm.password(b.layoutPassword);
        if (!ok) return;

        Map<String, String> body = new HashMap<>();
        body.put("name", AuthForm.text(b.layoutName));
        body.put("email", AuthForm.text(b.layoutEmail));
        body.put("phone", AuthForm.text(b.layoutPhone));
        body.put("password", AuthForm.text(b.layoutPassword));

        setLoading(true);
        ApiClient.get(this).register(body).enqueue(new ApiCallback<AuthResponse>(this) {
            @Override
            public void onSuccess(@NonNull AuthResponse auth) {
                SessionManager.get(RegisterActivity.this).saveSession(auth);
                Intent intent = new Intent(RegisterActivity.this, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
            }

            @Override
            public void onError(@NonNull String message) {
                if (isFinishing()) return;
                setLoading(false);
                Snackbar.make(b.getRoot(), message, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        b.btnRegister.setEnabled(!loading);
        b.progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }
}
