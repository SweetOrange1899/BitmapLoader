/*
 * Copyright (C) 2014 The Android Open Source Project.
 *
 *        yinglovezhuzhu@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.opensource.bitmaploader.test.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.opensource.bitmaploader.ImageCache;
import com.opensource.bitmaploader.ImageFetcher;
import com.opensource.bitmaploader.ImageWorker;
import com.opensource.bitmaploader.Utils;
import com.opensource.bitmaploader.test.R;
import com.opensource.bitmaploader.test.provider.Images;

import java.io.File;

@SuppressLint("NewApi")
public class ImageDetailActivity extends FragmentActivity implements OnClickListener {
    public static final String EXTRA_IMAGE = "extra_image";
    private static final String IMAGE_CACHE_DIR = "images";
    private static final String THUMB_CACHE_DIR = "thumbs";
    private ImagePagerAdapter mAdapter;
    private ImageWorker mPicWorker;
    private ImageWorker mThumbWorker;
    private ViewPager mPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_detail_pager);

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);

        // The ImageWorker takes care of loading images into our ImageView children asynchronously
//        mPicWorker = new ImageFetcher(this, displaymetrics.widthPixels, displaymetrics.heightPixels);
        mPicWorker = new ImageFetcher(this, getResources().getDisplayMetrics().widthPixels);
        File cachePath = null;
        if (Utils.hasExternalStorage()) {
            File appRoot = new File(Environment.getExternalStorageDirectory(), "BitmapLoader");
            cachePath = new File(appRoot, ".cache");
        }
        mPicWorker = new ImageFetcher(this, displaymetrics.widthPixels, displaymetrics.heightPixels);
        ImageCache.ImageCacheParams picCacheParams = new ImageCache.ImageCacheParams(cachePath, IMAGE_CACHE_DIR);
        picCacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 3;
        picCacheParams.diskCacheEnabled = true;
        mPicWorker.setAdapter(Images.imageWorkerUrlsAdapter);
        mPicWorker.setImageCache(new ImageCache(this, picCacheParams));
        mPicWorker.setImageFadeIn(false);

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(cachePath, THUMB_CACHE_DIR);
//        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(cachePath, IMAGE_CACHE_DIR);
        cacheParams.memCacheSize = 1024 * 1024 * Utils.getMemoryClass(this) / 3;
        cacheParams.diskCacheEnabled = false;
        mThumbWorker = new ImageFetcher(this, 150);
        mThumbWorker.setAdapter(Images.imageWorkerUrlsAdapter);
        mThumbWorker.setLoadingImage(R.drawable.empty_photo);
        mThumbWorker.setImageCache(new ImageCache(this, cacheParams));


        // Set up ViewPager and backing adapter
        mAdapter = new ImagePagerAdapter(getSupportFragmentManager(),
                mPicWorker.getAdapter().getSize());
        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
        mPager.setPageMargin((int) getResources().getDimension(R.dimen.image_detail_pager_margin));

        // Set up activity to go full screen
        getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN);

        // Enable some additional newer visibility and ActionBar features to create a more immersive
        // photo viewing experience
        if (Utils.hasActionBar()) {
            final ActionBar actionBar = getActionBar();

            if(null != actionBar) {
                // Enable "up" navigation on ActionBar icon and hide title text
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setDisplayShowTitleEnabled(false);

                // Start low profile mode and hide ActionBar
                mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
                actionBar.hide();

                // Hide and show the ActionBar as the visibility changes
                mPager.setOnSystemUiVisibilityChangeListener(
                        new View.OnSystemUiVisibilityChangeListener() {
                            @Override
                            public void onSystemUiVisibilityChange(int vis) {
                                if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
                                    actionBar.hide();
                                } else {
                                    actionBar.show();
                                }
                            }
                        }
                );
            }
        }

        // Set the current item based on the extra passed in to this activity
        final int extraCurrentItem = getIntent().getIntExtra(EXTRA_IMAGE, -1);
        if (extraCurrentItem != -1) {
            mPager.setCurrentItem(extraCurrentItem);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Home or "up" navigation
                final Intent intent = new Intent(this, ImageGridActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.clear_cache:
                final ImageCache cache = mPicWorker.getImageCache();
                if (cache != null) {
                    mPicWorker.getImageCache().cleanCaches();
                    mPicWorker.getImageCache().cleanDiskCache();
                    mThumbWorker.getImageCache().cleanCaches();
                    mThumbWorker.getImageCache().cleanDiskCache();
//                    DiskLruCache.clearCache(this, cache.getImageCacheParams().cachePath, IMAGE_CACHE_DIR);
                    Toast.makeText(this, R.string.clear_cache_complete,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    /**
     * Called by the ViewPager child fragments to load images via the one ImageWorker
     *
     * @return
     */
    public ImageWorker getPicWorker() {
        return mPicWorker;
    }


    public ImageWorker getThumbWorker() {
        return mThumbWorker;
    }

    /**
     * Set on the ImageView in the ViewPager children fragments, to enable/disable low profile mode
     * when the ImageView is touched.
     */
    @SuppressLint("NewApi")
    @Override
    public void onClick(View v) {
        final int vis = mPager.getSystemUiVisibility();
        if ((vis & View.SYSTEM_UI_FLAG_LOW_PROFILE) != 0) {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        } else {
            mPager.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE);
        }
    }

    /**
     * The main adapter that backs the ViewPager. A subclass of FragmentStatePagerAdapter as there
     * could be a large number of items in the ViewPager and we don't want to retain them all in
     * memory at once but create/destroy them on the fly.
     */
    private class ImagePagerAdapter extends FragmentStatePagerAdapter {
        private final int mSize;

        public ImagePagerAdapter(FragmentManager fm, int size) {
            super(fm);
            mSize = size;
        }

        @Override
        public int getCount() {
            return mSize;
        }

        @Override
        public Fragment getItem(int position) {
            return ImageDetailFragment.newInstance(position);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            final ImageDetailFragment fragment = (ImageDetailFragment) object;
            // As the item gets destroyed we try and cancel any existing work.
            fragment.cancelWork();
            super.destroyItem(container, position, object);
        }
    }
}
