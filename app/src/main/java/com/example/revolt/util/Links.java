package com.example.revolt.util;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.revolt.R;

/** Opens Google Maps directions and web pages, falling back gracefully when no app handles them. */
public final class Links {
    private Links() {
    }

    /** Directions from the user's current location to {@code destination}. */
    public static void directionsTo(Context context, String destination) {
        Uri uri = Uri.parse("https://www.google.com/maps/dir/").buildUpon()
                .appendQueryParameter("api", "1")
                .appendQueryParameter("destination", destination)
                .build();
        open(context, uri);
    }

    public static void open(Context context, Uri uri) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.error_generic, Toast.LENGTH_SHORT).show();
        }
    }
}
