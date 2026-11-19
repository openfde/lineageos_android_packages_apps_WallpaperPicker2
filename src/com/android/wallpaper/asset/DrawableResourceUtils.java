/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.wallpaper.asset;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Utilities for rendering vector / shape (XML) drawable resources into bitmaps.
 *
 * <p>The built-in wallpaper collections are now defined as resolution-independent XML
 * drawables (e.g. {@code <shape>} with {@code <solid>} or {@code <gradient>}). Because the
 * wallpaper pipeline (thumbnails, preview and set-wallpaper) ultimately needs decodable
 * bitmap data, these utilities rasterize an XML drawable at a requested resolution whenever
 * the resource cannot be decoded as a plain bitmap.
 */
public final class DrawableResourceUtils {

    /** Default resolution used when rasterizing vector wallpaper sources. */
    public static final int DEFAULT_WIDTH = 2160;
    public static final int DEFAULT_HEIGHT = 3840;

    /** Resolution used when rasterizing low-res thumbnails for grids. */
    public static final int THUMBNAIL_WIDTH = 540;
    public static final int THUMBNAIL_HEIGHT = 960;

    private DrawableResourceUtils() {
    }

    /**
     * Returns {@code true} if the given resource inflates to a drawable that is not a plain
     * bitmap (e.g. a {@code <shape>}, {@code <layer-list>} or {@code <vector>} drawable) and
     * therefore cannot be decoded with {@link android.graphics.BitmapFactory}.
     */
    public static boolean isXmlDrawable(Resources res, int resId) {
        Drawable drawable;
        try {
            drawable = res.getDrawable(resId, null);
        } catch (RuntimeException e) {
            return false;
        }
        return drawable != null && !(drawable instanceof BitmapDrawable);
    }

    /**
     * Rasterizes the given drawable resource into a bitmap of the given size.
     *
     * @return the rendered bitmap, or {@code null} if the resource could not be loaded.
     */
    @Nullable
    public static Bitmap renderToBitmap(Resources res, int resId, int width, int height) {
        Drawable drawable;
        try {
            drawable = res.getDrawable(resId, null);
        } catch (RuntimeException e) {
            return null;
        }
        if (drawable == null || width <= 0 || height <= 0) {
            return null;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, width, height);
        drawable.draw(canvas);
        canvas.setBitmap(null);
        return bitmap;
    }

    /**
     * Rasterizes the given drawable resource into a PNG input stream so the result can be fed
     * through the standard bitmap decoding / streaming wallpaper pipeline.
     *
     * @return the PNG stream, or {@code null} if the resource could not be rendered.
     */
    @Nullable
    public static ByteArrayInputStream renderToPngStream(
            Resources res, int resId, int width, int height) {
        Bitmap bitmap = renderToBitmap(res, resId, width, height);
        if (bitmap == null) {
            return null;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 90, output);
        bitmap.recycle();
        return new ByteArrayInputStream(output.toByteArray());
    }
}
