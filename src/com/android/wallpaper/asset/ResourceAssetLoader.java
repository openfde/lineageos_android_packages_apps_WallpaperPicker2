/*
 * Copyright (C) 2017 The Android Open Source Project
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
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;

import com.bumptech.glide.Priority;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Glide ModelLoader which loads InputStreams from ResourceAssets.
 */
public class ResourceAssetLoader implements ModelLoader<ResourceAsset, InputStream> {

    @Override
    public boolean handles(ResourceAsset resourceAsset) {
        return true;
    }

    @Nullable
    @Override
    public LoadData<InputStream> buildLoadData(ResourceAsset resourceAsset, int unusedWidth,
                                               int unusedHeight, Options options) {
        return new LoadData<>(resourceAsset.getKey(), new ResourceAssetFetcher(resourceAsset));
    }

    /**
     * Factory that constructs {@link ResourceAssetLoader} instances.
     */
    public static class ResourceAssetLoaderFactory
            implements ModelLoaderFactory<ResourceAsset, InputStream> {
        public ResourceAssetLoaderFactory() {
        }

        @Override
        public ModelLoader<ResourceAsset, InputStream> build(MultiModelLoaderFactory multiFactory) {
            return new ResourceAssetLoader();
        }

        @Override
        public void teardown() {
            // no-op
        }
    }

    /**
     * Glide DataFetcher for ResourceAsset.
     */
    protected static class ResourceAssetFetcher implements DataFetcher<InputStream> {

        private ResourceAsset mResourceAsset;

        public ResourceAssetFetcher(ResourceAsset resourceAsset) {
            mResourceAsset = resourceAsset;
        }

        @Override
        public void loadData(Priority priority, final DataCallback<? super InputStream> callback) {
            final Resources resources = mResourceAsset.getResources();
            final int resId = mResourceAsset.getResId();
            if (mResourceAsset.mIsThumbnail) {
                if (DrawableResourceUtils.isXmlDrawable(resources, resId)) {
                    InputStream rendered = DrawableResourceUtils.renderToPngStream(
                            resources,
                            resId,
                            DrawableResourceUtils.THUMBNAIL_WIDTH,
                            DrawableResourceUtils.THUMBNAIL_HEIGHT);
                    callback.onDataReady(
                            rendered != null ? rendered : resources.openRawResource(resId));
                } else {
                    callback.onDataReady(resources.openRawResource(resId));
                }
                return;
            }
            if (DrawableResourceUtils.isXmlDrawable(resources, resId)) {
                // Rasterize XML wallpapers (solid colors / gradients) into a modestly sized bitmap
                // for the grid tiles, since BitmapFactory can't decode them directly.
                Bitmap bitmap = DrawableResourceUtils.renderToBitmap(
                        resources,
                        resId,
                        DrawableResourceUtils.THUMBNAIL_WIDTH,
                        DrawableResourceUtils.THUMBNAIL_HEIGHT);
                if (bitmap != null) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, baos);
                    bitmap.recycle();
                    callback.onDataReady(new ByteArrayInputStream(baos.toByteArray()));
                    return;
                }
            }
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 8;
            Bitmap bitmap = BitmapFactory.decodeResource(resources, resId, options);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            callback.onDataReady(new ByteArrayInputStream(baos.toByteArray()));
        }

        @Override
        public DataSource getDataSource() {
            return DataSource.LOCAL;
        }

        @Override
        public void cancel() {
            // no op
        }

        @Override
        public void cleanup() {
            // no op
        }

        @Override
        public Class<InputStream> getDataClass() {
            return InputStream.class;
        }
    }
}
