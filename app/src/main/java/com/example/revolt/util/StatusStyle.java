package com.example.revolt.util;

import android.content.Context;
import android.content.res.ColorStateList;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.example.revolt.R;
import com.google.android.material.chip.Chip;

/** Colours and icons for each pickup request status. */
public final class StatusStyle {
    @ColorRes public final int foreground;
    @ColorRes public final int background;
    @DrawableRes public final int icon;

    private StatusStyle(int foreground, int background, int icon) {
        this.foreground = foreground;
        this.background = background;
        this.icon = icon;
    }

    public static StatusStyle of(String status) {
        if (status == null) status = "";
        switch (status) {
            case "scheduled":
                return new StatusStyle(R.color.status_scheduled, R.color.status_scheduled_bg, R.drawable.ic_event);
            case "picked_up":
                return new StatusStyle(R.color.status_picked_up, R.color.status_picked_up_bg, R.drawable.ic_truck);
            case "completed":
                return new StatusStyle(R.color.status_completed, R.color.status_completed_bg, R.drawable.ic_check_circle);
            case "cancelled":
                return new StatusStyle(R.color.status_cancelled, R.color.status_cancelled_bg, R.drawable.ic_cancel);
            case "rejected":
                return new StatusStyle(R.color.status_rejected, R.color.status_rejected_bg, R.drawable.ic_cancel);
            case "pending":
            default:
                return new StatusStyle(R.color.status_pending, R.color.status_pending_bg, R.drawable.ic_pending);
        }
    }

    /** Styles a non-interactive chip as a coloured status pill. */
    public static void apply(Chip chip, String status, String label) {
        Context c = chip.getContext();
        StatusStyle style = of(status);
        int fg = ContextCompat.getColor(c, style.foreground);
        chip.setText(label);
        chip.setTextColor(fg);
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(c, style.background)));
        chip.setChipStrokeWidth(0f);
        chip.setChipIconResource(style.icon);
        chip.setChipIconTint(ColorStateList.valueOf(fg));
        chip.setChipIconVisible(true);
    }
}
