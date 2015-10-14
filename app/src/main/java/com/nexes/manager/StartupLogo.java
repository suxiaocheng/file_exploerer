package com.nexes.manager;

import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import com.nexes.manager.util.ImageCache;
import com.nexes.manager.util.ImageFetcher;

public class StartupLogo extends AppCompatActivity {
    private static final String TAG = "StartupLogo";

    /* For Image Cached used only */
    public static final String IMAGE_CACHE_DIR = "thumbs";

    /**/
    public static int mImageThumbSize;

    /**/
    public static FragmentManager mFragmentManager;

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

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), R.drawable.image, options);
        mImageThumbSize = options.outWidth;

        mFragmentManager = getSupportFragmentManager();

        Intent intent = new Intent(this, Main.class);
        startActivity(intent);

        finish();
    }
}
