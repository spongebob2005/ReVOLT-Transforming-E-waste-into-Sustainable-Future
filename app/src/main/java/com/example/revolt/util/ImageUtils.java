package com.example.revolt.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;

import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class ImageUtils {
    private static final int MAX_EDGE_PX = 1600;

    private ImageUtils() {
    }

    /** A fresh, shareable file for the camera app to write into. */
    public static Uri newCameraUri(Context context) throws IOException {
        File dir = new File(context.getCacheDir(), "photos");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create " + dir);
        File file = File.createTempFile("capture_", ".jpg", dir);
        return FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
    }

    /**
     * Downscales and re-encodes a photo as JPEG so uploads stay small, applying the EXIF rotation
     * so the server stores it upright. Call off the main thread.
     */
    public static File compressForUpload(Context context, Uri source) throws IOException {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(context, source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) throw new IOException("Not an image");

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / (opts.inSampleSize * 2) >= MAX_EDGE_PX) {
            opts.inSampleSize *= 2;
        }
        Bitmap bitmap;
        try (InputStream in = open(context, source)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) throw new IOException("Could not decode image");

        int rotation;
        try (InputStream in = open(context, source)) {
            rotation = new ExifInterface(in).getRotationDegrees();
        }
        float scale = Math.min(1f, (float) MAX_EDGE_PX / Math.max(bitmap.getWidth(), bitmap.getHeight()));
        if (rotation != 0 || scale < 1f) {
            Matrix m = new Matrix();
            m.postScale(scale, scale);
            m.postRotate(rotation);
            Bitmap transformed = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
            if (transformed != bitmap) bitmap.recycle();
            bitmap = transformed;
        }

        File out = File.createTempFile("upload_", ".jpg", context.getCacheDir());
        try (OutputStream os = new FileOutputStream(out)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, os);
        } finally {
            bitmap.recycle();
        }
        return out;
    }

    private static InputStream open(Context context, Uri uri) throws IOException {
        InputStream in = context.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("Cannot open " + uri);
        return in;
    }
}
