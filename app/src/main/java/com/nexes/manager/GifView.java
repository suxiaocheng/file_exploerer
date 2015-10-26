package com.nexes.manager;

/**
 * Created by suxch on 2015/10/25.
 */

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Movie;
import android.util.AttributeSet;
import android.view.View;

public class GifView extends View {
    private long movieStart;
    private Movie mMovie;

    private float mScale;
    private int mMeasuredMovieWidth, mMeasuredMovieHeight;

    private float mLeft, mTop;

    private boolean mVisible;

    public GifView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
    }

    public GifView(Context context) {
        super(context);
        mMovie = Movie.decodeStream(getResources().openRawResource(
                R.raw.animation));
    }

    public void setFile(String file) {
        mMovie = Movie.decodeFile(file);
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mMovie != null) {
            int movieWidth = mMovie.width();
            int movieHeight = mMovie.height();
            int maximumWidth = MeasureSpec.getSize(widthMeasureSpec);
            float scaleW = (float) movieWidth / (float) maximumWidth;
            mScale = 1f / scaleW;
            mMeasuredMovieWidth = maximumWidth;
            mMeasuredMovieHeight = (int) (movieHeight * mScale);
            setMeasuredDimension(mMeasuredMovieWidth, mMeasuredMovieHeight);
        } else {
            setMeasuredDimension(getSuggestedMinimumWidth(),
                    getSuggestedMinimumHeight());
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mLeft = (getWidth() - mMeasuredMovieWidth) / 2f;
        mTop = (getHeight() - mMeasuredMovieHeight) / 2f;
        mVisible = getVisibility() == View.VISIBLE;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long curTime = android.os.SystemClock.uptimeMillis();
        if (movieStart == 0) {
            movieStart = curTime;
        }
        if (mMovie != null) {
            int duration = mMovie.duration();
            int relTime = (int) ((curTime - movieStart) % duration);
            mMovie.setTime(relTime);
            mMovie.draw(canvas, 0, 0);
            invalidate();
        }
        super.onDraw(canvas);
    }
}