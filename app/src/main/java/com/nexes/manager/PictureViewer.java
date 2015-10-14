package com.nexes.manager;

import java.io.File;
import java.util.ArrayList;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.nexes.manager.util.ImageCache;
import com.nexes.manager.util.ImageFetcher;

public class PictureViewer extends FragmentActivity {
	final static String TAG = "PictureViewer";

	private String filePath;
	private ViewPager mPager;
	ImagePagerAdapter mAdapter;

    public static ImageFetcher mImageFetcher;

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_picture_viewer);

		// getActionBar().setDisplayHomeAsUpEnabled(true);

		String filename = getIntent().getStringExtra(Main.EXTRA_PIC_LOCATION);
		int lastSlashPosition = filename.lastIndexOf('/');
		if (lastSlashPosition != -1) {
			filePath = filename.substring(0, lastSlashPosition);
		} else {
			filePath = null;
		}

		Log.d(TAG, "going to open dir:" + filePath + ", picture file :"
				+ filename);

        // Fetch screen height and width, to use as our max size when loading images as this
        // activity runs full screen
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        final int height = displayMetrics.heightPixels;
        final int width = displayMetrics.widthPixels;

        // For this sample we'll use half of the longest width to resize our images. As the
        // image scaling ensures the image is larger than this, we should be left with a
        // resolution that is appropriate for both portrait and landscape. For best image quality
        // we shouldn't divide by 2, but this will use more memory and require a larger memory
        // cache.
        final int longest = (height > width ? height : width);

        ImageCache.ImageCacheParams cacheParams =
                new ImageCache.ImageCacheParams(this, StartupLogo.IMAGE_CACHE_DIR);
        cacheParams.setMemCacheSizePercent(0.25f); // Set memory cache to 25% of app memory

        // The ImageFetcher takes care of loading images into our ImageView children asynchronously
        mImageFetcher = new ImageFetcher(this, longest);
        mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);

        //mImageFetcher.clearCache();

		// Set up ViewPager and backing adapter
		mAdapter = new ImagePagerAdapter(getSupportFragmentManager(), filePath);
		mPager = (ViewPager) findViewById(R.id.pager);
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(mAdapter.getCurrentItemNumber(filename.substring(
                lastSlashPosition + 1, filename.length())));

        Log.d(TAG, "onCreate");
		// mPager.setOffscreenPageLimit(2);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.picture_viewer, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * The main adapter that backs the ViewPager. A subclass of
	 * FragmentStatePagerAdapter as there could be a large number of items in
	 * the ViewPager and we don't want to retain them all in memory at once but
	 * create/destroy them on the fly.
	 */
	private class ImagePagerAdapter extends FragmentStatePagerAdapter {
		private final int mSize;
		private File picFilePath;
		private String pathDirectory;
		private String[] fileList;
		private ArrayList<String> picFilenameList = new ArrayList<String>();
		private final String[] picExtName = { "jpeg", "jpg", "png", "gif",
				"tiff" };

		public ImagePagerAdapter(FragmentManager fragmentManager, String path) {
			super(fragmentManager);
			picFilePath = new File(path);
			pathDirectory = path;
			fileList = picFilePath.list();

			String ext;
			int dotPosition;
			for (int i = 0; i < fileList.length; i++) {
				ext = null;
				dotPosition = fileList[i].lastIndexOf('.');
				if ((dotPosition != -1) && (dotPosition != 0)) {
					ext = new String(fileList[i].substring(dotPosition + 1,
							fileList[i].length()));
					Log.d(TAG, "ImagePagerAdapter count:" + i
							+ ", currentfile:" + fileList[i] + ", ext:" + ext);
					for (int j = 0; j < picExtName.length; j++) {
						if (ext.compareToIgnoreCase(picExtName[j]) == 0) {
							picFilenameList.add(fileList[i]);
							break;
						}
					}
				}
			}
			mSize = picFilenameList.size();
		}

		@Override
		public int getCount() {
			return mSize;
		}

		@Override
		public Fragment getItem(int position) {
			return ImageDetailFragment.newInstance(new String(pathDirectory
					+ '/' + picFilenameList.get(position)));
		}

		public int getCurrentItemNumber(String str) {
			int count = 0;
			if (!picFilenameList.isEmpty()) {
				for (int i = 0; i < picFilenameList.size(); i++) {
					if (picFilenameList.get(i).equals(str) == true) {
						count = i;
						break;
					}
				}
			}
			return count;
		}
	}

    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);

        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();

        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
        mImageFetcher = null;

        Log.d(TAG, "onDestroy");
    }
}
