package com.example.revolt.ui.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.revolt.R;
import com.example.revolt.databinding.ActivityMainBinding;
import com.example.revolt.util.SystemBars;

/** Hosts the Home, My requests and Profile tabs. */
public class MainActivity extends AppCompatActivity {
    private static final String EXTRA_TAB = "tab";

    private ActivityMainBinding b;

    public static Intent intent(Context context, @IdRes int tab) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        if (savedInstanceState == null) {
            int tab = getIntent().getIntExtra(EXTRA_TAB, R.id.nav_home);
            b.bottomNav.getMenu().findItem(tab).setChecked(true);
            show(tab);
        } else {
            updateSystemBars(b.bottomNav.getSelectedItemId());
        }

        b.bottomNav.setOnItemSelectedListener(item -> {
            show(item.getItemId());
            return true;
        });
        b.bottomNav.setOnItemReselectedListener(item -> {
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (b.bottomNav.getSelectedItemId() != R.id.nav_home) {
                    selectTab(R.id.nav_home);
                } else {
                    finish();
                }
            }
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(EXTRA_TAB)) selectTab(intent.getIntExtra(EXTRA_TAB, R.id.nav_home));
    }

    public void selectTab(@IdRes int tab) {
        if (b.bottomNav.getSelectedItemId() == tab) {
            show(tab);
        } else {
            b.bottomNav.setSelectedItemId(tab);
        }
    }

    private void show(@IdRes int tab) {
        Fragment fragment;
        if (tab == R.id.nav_requests) {
            fragment = new RequestsFragment();
        } else if (tab == R.id.nav_profile) {
            fragment = new ProfileFragment();
        } else {
            fragment = new HomeFragment();
        }
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                .replace(R.id.fragmentContainer, fragment)
                .commit();
        updateSystemBars(tab);
    }

    private void updateSystemBars(@IdRes int tab) {
        if (tab == R.id.nav_home) {
            SystemBars.hero(this);
        } else {
            SystemBars.plain(this);
        }
    }
}
