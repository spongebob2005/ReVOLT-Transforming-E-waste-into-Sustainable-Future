package com.example.revolt.util;

import android.app.Activity;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;

import com.example.revolt.R;

public final class SystemBars {
    private SystemBars() {
    }

    /** Green status bar with white icons, for screens that start with the hero banner. */
    public static void hero(Activity activity) {
        set(activity, R.color.hero_start, false);
    }

    /** Status bar matching the page background, with icons following light/dark mode. */
    public static void plain(Activity activity) {
        set(activity, R.color.background, activity.getResources().getBoolean(R.bool.light_system_bars));
    }

    private static void set(Activity activity, @ColorRes int color, boolean lightIcons) {
        Window window = activity.getWindow();
        window.setStatusBarColor(ContextCompat.getColor(activity, color));
        WindowCompat.getInsetsController(window, window.getDecorView()).setAppearanceLightStatusBars(lightIcons);
    }
}
