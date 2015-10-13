package com.nexes.manager;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.nexes.manager.util.ImageCache;
import com.nexes.manager.util.ImageFetcher;

public class StartupLogo extends AppCompatActivity {
    private static final String TAG = "StartupLogo";

    /* For Image Cached used only */
    private static final String IMAGE_CACHE_DIR = "thumbs";
    public static ImageFetcher mImageFetcher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup_logo);
        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        // setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        if (mImageFetcher == null) {
        /* ImageCache Init: Before the list image getView, must init the cache first */
            ImageCache.ImageCacheParams cacheParams =
                    new ImageCache.ImageCacheParams(getApplication(), IMAGE_CACHE_DIR);
            cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

            final int width = getResources().getDisplayMetrics().widthPixels;
            int mImageThumbSize;
            if (width == 0) {
                mImageThumbSize = 52;
            } else {
                mImageThumbSize = width / 10;
            }
            Log.d(TAG, "Thumbnail Width: " + mImageThumbSize);

            // The ImageFetcher takes care of loading images into our ImageView children asynchronously
            mImageFetcher = new ImageFetcher(getApplication(), mImageThumbSize);
            mImageFetcher.setLoadingImage(R.drawable.image);
            mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
        }

        Intent intent = new Intent(this, Main.class);
        startActivity(intent);

        finish();
    }
}
