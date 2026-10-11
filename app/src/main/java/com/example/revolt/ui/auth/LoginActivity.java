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
import com.example.revolt.databinding.ActivityLoginBinding;
import com.example.revolt.ui.main.MainActivity;
import com.example.revolt.util.ServerDialog;
import com.example.revolt.util.SystemBars;
import com.google.android.material.snackbar.Snackbar;

import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding b;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.hero(this);

        b.header.title.setText(R.string.login_title);
        b.header.subtitle.setText(R.string.login_subtitle);

        b.btnLogin.setOnClickListener(v -> attemptLogin());
        b.inputPassword.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        b.btnGoRegister.setOnClickListener(v -> startActivity(new Intent(this, RegisterActivity.class)));
        b.btnServer.setOnClickListener(v -> ServerDialog.show(this, null));
    }

    private void attemptLogin() {
        boolean ok = AuthForm.email(b.layoutEmail) & AuthForm.password(b.layoutPassword);
        if (!ok) return;

        Map<String, String> body = new HashMap<>();
        body.put("email", AuthForm.text(b.layoutEmail));
        body.put("password", AuthForm.text(b.layoutPassword));

        setLoading(true);
        ApiClient.get(this).login(body).enqueue(new ApiCallback<AuthResponse>(this) {
            @Override
            public void onSuccess(@NonNull AuthResponse auth) {
                SessionManager.get(LoginActivity.this).saveSession(auth);
                Intent intent = new Intent(LoginActivity.this, MainActivity.class)
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
        b.btnLogin.setEnabled(!loading);
        b.progress.setVisibility(loading ? View.VISIBLE : View.INVISIBLE);
    }
}
