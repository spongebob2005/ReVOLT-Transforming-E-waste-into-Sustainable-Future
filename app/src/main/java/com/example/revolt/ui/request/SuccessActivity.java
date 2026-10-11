package com.example.revolt.ui.request;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.revolt.R;
import com.example.revolt.databinding.ActivitySuccessBinding;
import com.example.revolt.ui.main.MainActivity;
import com.example.revolt.util.SystemBars;

public class SuccessActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "id";
    private static final String EXTRA_ORG = "org";

    public static Intent intent(Context context, long requestId, String organizationName) {
        return new Intent(context, SuccessActivity.class)
                .putExtra(EXTRA_ID, requestId)
                .putExtra(EXTRA_ORG, organizationName);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivitySuccessBinding b = ActivitySuccessBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        SystemBars.plain(this);

        long id = getIntent().getLongExtra(EXTRA_ID, 0);
        b.body.setText(getString(R.string.success_body, (int) id, getIntent().getStringExtra(EXTRA_ORG)));

        b.btnView.setOnClickListener(v -> {
            startActivity(MainActivity.intent(this, R.id.nav_requests));
            startActivity(RequestDetailActivity.intent(this, id));
            finish();
        });
        b.btnHome.setOnClickListener(v -> goHome());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                goHome();
            }
        });
    }

    private void goHome() {
        startActivity(MainActivity.intent(this, R.id.nav_home));
        finish();
    }
}
