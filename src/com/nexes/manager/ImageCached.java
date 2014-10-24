package com.nexes.manager;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;
import android.util.Log;

public class ImageCached {
	private static final String TAG = "ImageCached";
	private LruCache<String, Bitmap> mMemoryCache;
	private static long max_cached_size;

	public ImageCached() {
		// Get max available VM memory, exceeding this amount will throw an
		// OutOfMemory exception. Stored in kilobytes as LruCache takes an
		// int in its constructor.
		final long maxMemory = (int) (Runtime.getRuntime().maxMemory());

		// Use 1/4th of the available memory for this memory cache.
		final long cacheSize = maxMemory / 4;

		max_cached_size = cacheSize / 16;

		Log.d(TAG, "Create grid view, Cache memory size(byte):" + cacheSize);

		mMemoryCache = new LruCache<String, Bitmap>((int) (cacheSize / 1024)) {
			@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				// The cache size will be measured in kilobytes rather than
				// number of items.
				return bitmap.getByteCount() / 1024;
			}
		};
	}

	public boolean ImageCachedDelete() {
		if (mMemoryCache != null) {
			// clear the memory cache
			mMemoryCache.evictAll();
			mMemoryCache = null;
			return true;
		}
		return false;
	}

	/**
	 * Cache function
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		long bitmap_size;
		bitmap_size = bitmap.getByteCount();
		if ((getBitmapFromMemCache(key) == null) && (bitmap_size < max_cached_size)) {
			mMemoryCache.put(key, bitmap);
			Log.d(TAG,
					"cached memory:" + key + ", size:" + bitmap_size
							+ ", width:" + bitmap.getWidth() + ", height:"
							+ bitmap.getHeight());
		}
	}

	public Bitmap getBitmapFromMemCache(String key) {
		Bitmap bmp = null;
		if (mMemoryCache == null) {
			return bmp;
		}
		bmp = mMemoryCache.get(key);
		if (bmp != null) {
			Log.d(TAG, "hit memory cached" + key);
		}
		return bmp;
	}
}
