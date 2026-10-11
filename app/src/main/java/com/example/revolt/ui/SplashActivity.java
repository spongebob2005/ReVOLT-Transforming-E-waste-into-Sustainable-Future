package com.example.revolt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.revolt.data.SessionManager;
import com.example.revolt.databinding.ActivitySplashBinding;
import com.example.revolt.ui.auth.LoginActivity;
import com.example.revolt.ui.main.MainActivity;

public class SplashActivity extends AppCompatActivity {
    private static final long SPLASH_MS = 1400;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proceed = () -> {
        boolean loggedIn = SessionManager.get(this).isLoggedIn();
        startActivity(new Intent(this, loggedIn ? MainActivity.class : LoginActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySplashBinding b = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        b.logoPlate.setAlpha(0f);
        b.logoPlate.setScaleX(0.85f);
        b.logoPlate.setScaleY(0.85f);
        b.logoPlate.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(600).start();

        b.slogan.setAlpha(0f);
        b.slogan.setTranslationY(40f);
        b.slogan.animate().alpha(1f).translationY(0f).setStartDelay(300).setDuration(600).start();

        handler.postDelayed(proceed, SPLASH_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(proceed);
        super.onDestroy();
    }
}
