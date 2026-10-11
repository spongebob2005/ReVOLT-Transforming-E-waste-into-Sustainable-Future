package com.example.revolt.ui.auth;

import android.util.Patterns;

import androidx.annotation.StringRes;

import com.example.revolt.R;
import com.google.android.material.textfield.TextInputLayout;

/** Field validation shared by the login and registration screens. */
final class AuthForm {
    private AuthForm() {
    }

    static String text(TextInputLayout layout) {
        return layout.getEditText() == null || layout.getEditText().getText() == null
                ? "" : layout.getEditText().getText().toString().trim();
    }

    /** Shows {@code error} on the field when {@code valid} is false; returns {@code valid}. */
    static boolean check(TextInputLayout layout, boolean valid, @StringRes int error) {
        layout.setError(valid ? null : layout.getContext().getString(error));
        return valid;
    }

    static boolean email(TextInputLayout layout) {
        return check(layout, Patterns.EMAIL_ADDRESS.matcher(text(layout)).matches(), R.string.error_email);
    }

    static boolean password(TextInputLayout layout) {
        return check(layout, text(layout).length() >= 6, R.string.error_password);
    }

    static boolean name(TextInputLayout layout) {
        return check(layout, text(layout).length() >= 2, R.string.error_name);
    }

    static boolean phone(TextInputLayout layout) {
        return check(layout, text(layout).replaceAll("[\\s-]", "").matches("\\+?\\d{10,13}"), R.string.error_phone);
    }
}
