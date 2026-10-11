package com.example.revolt.util;

import androidx.annotation.Nullable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class Formats {
    private Formats() {
    }

    @Nullable
    private static Date parse(@Nullable String value) {
        if (value == null || value.isEmpty()) return null;
        boolean dateOnly = value.length() == 10;
        SimpleDateFormat parser = new SimpleDateFormat(
                dateOnly ? "yyyy-MM-dd" : "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        // Timestamps from the server are UTC; plain dates are calendar days, so keep them local.
        if (!dateOnly) parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return parser.parse(value);
        } catch (ParseException e) {
            return null;
        }
    }

    /** "12 Oct 2026" */
    public static String date(@Nullable String iso) {
        Date d = parse(iso);
        return d == null ? "" : new SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(d);
    }

    /** "12 Oct, 4:30 pm" */
    public static String dateTime(@Nullable String iso) {
        Date d = parse(iso);
        return d == null ? "" : new SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()).format(d);
    }

    /** "Oct 2026" */
    public static String monthYear(@Nullable String iso) {
        Date d = parse(iso);
        return d == null ? "" : new SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(d);
    }

    /** 12.0 → "12", 0.45 → "0.5" */
    public static String kg(double value) {
        double rounded = Math.round(value * 10) / 10.0;
        if (rounded == Math.floor(rounded)) return String.valueOf((long) rounded);
        return String.format(Locale.getDefault(), "%.1f", rounded);
    }

    /** First letters of the first two words, for avatar circles. */
    public static String initials(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(2, parts.length); i++) sb.append(parts[i].charAt(0));
        return sb.toString().toUpperCase(Locale.getDefault());
    }

    public static String firstName(@Nullable String name) {
        if (name == null || name.trim().isEmpty()) return "";
        return name.trim().split("\\s+")[0];
    }
}
