package com.nexes.manager;

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

	/**
	 * This fragment will populate the children of the ViewPager from
	 * {@link ImageDetailActivity}.
	 */
	private static final String IMAGE_DATA_EXTRA = "extra_image_data";
	private String mImageUrl;
	private TouchImageView mImageView;
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
	 * @param imageUrl
	 *            The image url to load
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

		mShortAnimationDuration = getResources().getInteger(
				android.R.integer.config_shortAnimTime);

		Display display = getActivity().getWindowManager().getDefaultDisplay();

		if ((realWidth == 0) || (realHeight == 0)) {
			if (Build.VERSION.SDK_INT >= 17) {
				// new pleasant way to get real metrics
				DisplayMetrics realMetrics = new DisplayMetrics();
				display.getRealMetrics(realMetrics);
				realWidth = realMetrics.widthPixels;
				realHeight = realMetrics.heightPixels;

			} else if (Build.VERSION.SDK_INT >= 14) {
				// reflection for this weird in-between time
				try {
					Method mGetRawH = Display.class.getMethod("getRawHeight");
					Method mGetRawW = Display.class.getMethod("getRawWidth");
					realWidth = (Integer) mGetRawW.invoke(display);
					realHeight = (Integer) mGetRawH.invoke(display);
				} catch (Exception e) {
					// this may not be 100% accurate, but it's all we've got
					realWidth = display.getWidth();
					realHeight = display.getHeight();
					Log.e("Display Info",
							"Couldn't use reflection to get the real display metrics.");
				}

			} else {
				// This should be close, as lower API devices should not have
				// window
				// navigation bars
				realWidth = display.getWidth();
				realHeight = display.getHeight();
			}
			displayMaxXY = (realWidth > realHeight) ? realHeight : realWidth;
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
			loadImage(mImageUrl, mImageView);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mImageView != null) {
			// Cancel any pending image work
			// ImageWorker.cancelWork(mImageView);
			mImageView.setImageDrawable(null);
		}
	}

	public static int calculateInSampleSize(BitmapFactory.Options options,
			int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (enableSampleDown == false) {
			/* The max picture size should not execced the max_memory_size/8 */
			final long maxMemory = (int) (Runtime.getRuntime().maxMemory()) / 8;

			int pic_width, pic_height;
			pic_width = options.outWidth;
			pic_height = options.outHeight;

			while ((pic_width * pic_height) > maxMemory) {
				inSampleSize *= 2;
				pic_width >>= 1;
				pic_height >>= 1;
			}

			if (inSampleSize > 1) {
				Log.d(TAG, "Picture too large, sample down scale:"
						+ inSampleSize);
			}

			return inSampleSize;
		}

		/*
		 * if (height > reqHeight || width > reqWidth) {
		 * 
		 * final int halfHeight = height / 2; final int halfWidth = width / 2;
		 * 
		 * // Calculate the largest inSampleSize value that is a power of 2 and
		 * // keeps both // height and width larger than the requested height
		 * and width. while ((halfHeight / inSampleSize) > reqHeight &&
		 * (halfWidth / inSampleSize) > reqWidth) { inSampleSize *= 2; } }
		 */
		if ((options.outHeight > reqHeight) || (options.outWidth > reqWidth)) {
			int sample_x;
			int sample_y;

			try {
				sample_x = (options.outHeight + reqHeight) / reqHeight;
				sample_y = (options.outWidth + reqWidth) / reqWidth;
			} catch (Exception E) {
				sample_x = sample_y = 1;
				Log.d(TAG, "calculateInSampleSize -> Divide num is 0");
			}

			inSampleSize = (sample_x > sample_y) ? sample_x : sample_y;
		}

		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res,
			String resId, int reqWidth, int reqHeight) {

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, realWidth,
				realHeight);

		Log.d(TAG, "decode:" + "inSample" + options.inSampleSize + ", width:"
				+ options.outWidth + ", height:" + options.outHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeFile(resId, options);
	}

	class BitmapWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final WeakReference<TouchImageView> imageViewReference;
		private String data;
		private boolean needFadeIn;

		@TargetApi(Build.VERSION_CODES.HONEYCOMB)
		public BitmapWorkerTask(String dat, TouchImageView imageView,
				boolean fadeFlag) {
			// Use a WeakReference to ensure the TouchImageView can be garbage
			// collected
			imageViewReference = new WeakReference<TouchImageView>(imageView);
			needFadeIn = fadeFlag;
			data = dat;
			Log.d(TAG, "create the BitmapWorkerTask");
			if (needFadeIn == true) {
				imageView.setAlpha(0f);
				imageView.setVisibility(View.GONE);

				progressBar.setAlpha(1f);
				progressBar.setVisibility(View.VISIBLE);
			} else {
				imageView.setVisibility(View.VISIBLE);
				progressBar.setVisibility(View.GONE);
			}
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(String... params) {
			// height = params[0].intValue();
			Log.d(TAG, "doing background job");

			final Bitmap bitmap = decodeSampledBitmapFromResource(
					getResources(), data, displayMaxXY, displayMaxXY);
			PictureViewer.imageCached.addBitmapToMemoryCache(
					String.valueOf(params[0]), bitmap);
			return bitmap;
		}

		// Once complete, see if TouchImageView is still around and set bitmap.
		@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (imageViewReference != null && bitmap != null) {

				Log.d(TAG, "update bitmap in the imageView");

				final TouchImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
					if (needFadeIn == true) {
						needFadeIn = false;
						imageView.setAlpha(0f);
						imageView.setVisibility(View.VISIBLE);

						progressBar.setAlpha(0f);
						progressBar.setVisibility(View.GONE);

						imageView.animate().alpha(1f)
								.setDuration(mShortAnimationDuration)
								.setListener(null);
					}
				}
			}
		}
	}

	static class AsyncDrawable extends BitmapDrawable {
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap,
				BitmapWorkerTask bitmapWorkerTask) {
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(
					bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask() {
			return bitmapWorkerTaskReference.get();
		}
	}

	/**
	 * Load an image specified by the data parameter into an TouchImageView
	 * (override {@link ImageWorker#processBitmap(Object)} to define the
	 * processing logic). A memory and disk cache will be used if an
	 * {@link ImageCache} has been added using
	 * {@link ImageWorker#addImageCache(android.support.v4.app.FragmentManager, ImageCache.ImageCacheParams)}
	 * . If the image is found in the memory cache, it is set immediately,
	 * otherwise an {@link AsyncTask} will be created to asynchronously load the
	 * bitmap.
	 * 
	 * @param data
	 *            The URL of the image to download.
	 * @param imageView
	 *            The TouchImageView to bind the downloaded image to.
	 */
	public void loadImage(String data, TouchImageView imageView) {
		if (data == null) {
			return;
		}

		Bitmap bitmap = null;

		bitmap = PictureViewer.imageCached.getBitmapFromMemCache(String
				.valueOf(data));

		if (bitmap != null) {
			// Bitmap found in memory cache
			imageView.setImageBitmap(bitmap);
			progressBar.setVisibility(View.GONE);
		} else if (cancelPotentialWork(data, imageView)) {
			// BEGIN_INCLUDE(execute_background_task)
			final BitmapWorkerTask task = new BitmapWorkerTask(data, imageView,
					true);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(
					getResources(), mLoadingBitmap, task);
			imageView.setImageDrawable(asyncDrawable);

			// NOTE: This uses a custom version of AsyncTask that has been
			// pulled from the
			// framework and slightly modified. Refer to the docs at the top of
			// the class
			// for more info on what was changed.
			task.executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR, data);
			// END_INCLUDE(execute_background_task)
		}
	}

	public static boolean cancelPotentialWork(Object data,
			TouchImageView imageView) {
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null) {
			final Object bitmapData = bitmapWorkerTask.data;
			// If bitmapData is not yet set or it differs from the new data
			if (bitmapData == null || bitmapData != data) {
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			} else {
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the TouchImageView, or an existing task was
		// cancelled
		return true;
	}

	private static BitmapWorkerTask getBitmapWorkerTask(TouchImageView imageView) {
		if (imageView != null) {
			final Drawable drawable = imageView.getDrawable();
			if (drawable instanceof AsyncDrawable) {
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}
		return null;
	}

}
