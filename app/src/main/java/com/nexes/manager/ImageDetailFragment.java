package com.nexes.manager;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.FloatMath;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.MediaController;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class ImageDetailFragment extends Fragment {
    private static final String TAG = "ImageDetailFragment";

    // We can be in one of these 3 states
    public static final int NONE = 0;
    public static final int DRAG = 1;
    public static final int ZOOM = 2;
    public static int mode = NONE;

    Matrix matrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    PointF start = new PointF();
    public static PointF mid = new PointF();

    float oldDist;

    private boolean display_gif = false;

    /**
     * This fragment will populate the children of the ViewPager from
     * {@link Fragment}.
     */
    private static final String IMAGE_DATA_EXTRA = "extra_image_data";
    private String mImageUrl;
    private TouchImageView mImageView;
    private GifImageView mGifImageView;
    private View progressBar;
    private static boolean enableSampleDown = false;
    private static boolean enableCachedImage = true;
    private int mShortAnimationDuration;
    private Bitmap mLoadingBitmap;
    private static int realWidth;
    private static int realHeight;
    private static int displayMaxXY;

    /**
     * Factory method to generate a new instance of the fragment given an image
     * number.
     *
     * @param imageUrl The image url to load
     * @return A new instance of ImageDetailFragment with imageNum extras
     */
    public static ImageDetailFragment newInstance(String imageUrl) {
        final ImageDetailFragment f = new ImageDetailFragment();

        final Bundle args = new Bundle();
        args.putString(IMAGE_DATA_EXTRA, imageUrl);
        f.setArguments(args);

        return f;
    }

    /**
     * Empty constructor as per the Fragment documentation
     */
    public ImageDetailFragment() {
    }

    /**
     * Populate image using a url from extras, use the convenience factory
     * method {@link ImageDetailFragment#newInstance(String)} to create this
     * fragment.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageUrl = getArguments() != null ? getArguments().getString(
                IMAGE_DATA_EXTRA) : null;

        int dotPosition = mImageUrl.lastIndexOf('.');
        if ((dotPosition != -1) && (dotPosition != 0)) {
            String ext = mImageUrl.substring(dotPosition + 1, mImageUrl.length());
            if (ext.compareToIgnoreCase("gif") == 0) {
                display_gif = true;
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate and locate the main TouchImageView
        final View v = inflater.inflate(R.layout.fragment_image_detail,
                container, false);
        mImageView = (TouchImageView) v.findViewById(R.id.imageView);
        progressBar = (View) v.findViewById(R.id.loading_spinner);
        mGifImageView = (GifImageView) v.findViewById(R.id.gif_display_view);

        progressBar.setVisibility(View.INVISIBLE);
        if (display_gif == true) {
            mImageView.setVisibility(View.GONE);
            mGifImageView.setVisibility(View.VISIBLE);
        } else {
            mImageView.setVisibility(View.VISIBLE);
            mGifImageView.setVisibility(View.GONE);
        }

        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Use the parent activity to load the image asynchronously into the
        // TouchImageView (so a single
        // cache can be used over all pages in the ViewPager
        if (PictureViewer.class.isInstance(getActivity())) {
            Log.d(TAG, "onActivityCreated:" + mImageUrl);
            if (!display_gif) {
                PictureViewer.mImageFetcher.loadImage(mImageUrl, mImageView);
            } else {
                FileDescriptor fd;
                try {
                    fd = new RandomAccessFile(mImageUrl, "r" ).getFD();
                    GifDrawable gifFromFd = new GifDrawable( fd );
                    mGifImageView.setImageDrawable(gifFromFd);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*
                final MediaController mc = new MediaController(getContext());
                mc.setMediaPlayer((GifDrawable) mGifImageView.getDrawable());
                mc.setAnchorView(mGifImageView);
                mGifImageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mc.show();
                    }
                });
                */
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (display_gif) {
        } else {
            if (mImageView != null) {
                // Cancel any pending image work
                mImageView.setImageDrawable(null);
            }
        }
    }
}
