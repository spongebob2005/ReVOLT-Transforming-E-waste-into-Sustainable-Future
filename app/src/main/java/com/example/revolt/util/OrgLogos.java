package com.example.revolt.util;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;

import com.example.revolt.R;

/** Maps the backend's organization {@code logoKey} to the bundled logo drawables. */
public final class OrgLogos {
    private OrgLogos() {
    }

    @DrawableRes
    public static int forKey(@Nullable String key) {
        if (key == null) return R.drawable.ic_business;
        switch (key) {
            case "green_peace":
                return R.drawable.green_peace;
            case "navdanya":
                return R.drawable.navdanya;
            case "aaranyak":
                return R.drawable.aaranyak;
            case "kalpavriksh":
                return R.drawable.kalpavriksh;
            default:
                return R.drawable.ic_business;
        }
    }
}
