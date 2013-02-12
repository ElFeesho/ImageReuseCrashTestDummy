package com.test.imagereusecrashtestdummy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;

/**
 * This class is used to load remote images in the background. It handles the
 * case where another image may be set as the target during the meantime and
 * protects from multiple deliveries of bitmaps that may no longer apply.
 * 
 * @author Chris S.
 */
public class CachedImageLoaderTask extends AsyncTask<View, Void, Bitmap>
{
	private static final int MAX_THREADS = 3;

	private final static Executor sImageLoadThreadPool = Executors.newFixedThreadPool(MAX_THREADS, new ThreadFactory()
	{
		private int mThreadCount = 0;

		@Override
		public Thread newThread(Runnable aR)
		{
			return new Thread(aR, "ImageLoadThread-" + (++mThreadCount));
		}
	});

	private File mCacheDir = null;

	private Bitmap mReuseBitmap = null;

	public CachedImageLoaderTask(File aCacheDir, Bitmap aReuseBitmap)
	{
		mCacheDir = aCacheDir;
		mReuseBitmap = aReuseBitmap;
	}

	private File generateCacheFile(Uri aUri)
	{
		return new File(mCacheDir, Uri.encode(aUri.toString()));
	}

	@Override
	protected Bitmap doInBackground(View... aParams)
	{
		Uri uri = (Uri) aParams[0].getTag(R.id.image_uri);

		File cacheDest = generateCacheFile(uri);

		if (cacheDest.exists())
		{
			mReuseBitmap = loadFromCache(cacheDest);
		}
		else
		{
			mReuseBitmap = loadFromNetwork(uri, cacheDest);
		}

		// Make sure that the image is only set on the correct image view
		if (uri.compareTo((Uri) aParams[0].getTag(R.id.image_uri)) == 0)
		{
			return mReuseBitmap;
		}
		return null;
	}

	/*
	 * This method makes the network trip to download a remote image.
	 * 
	 * It also caches the image so the next request to display this image will
	 * result in the image being loaded from cache.
	 */
	private Bitmap loadFromNetwork(Uri aUri, File aCacheDest)
	{
		Bitmap cachedImage = null;
		try
		{
			HttpClient hc = new DefaultHttpClient();
			HttpGet hg = new HttpGet(aUri.toString());
			HttpResponse response = hc.execute(hg);
			if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
			{
				cachedImage = createOrReuseBitmap(response.getEntity().getContent());
				cacheBitmap(cachedImage, aCacheDest);
			}
			else
			{
				response.getEntity().consumeContent();
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		return cachedImage;
	}

	/*
	 * This method loads a bitmap from cache.
	 */
	private Bitmap loadFromCache(File aCacheDest)
	{
		Bitmap cachedBitmap = null;
		try
		{
			FileInputStream fin = new FileInputStream(aCacheDest);
			cachedBitmap = createOrReuseBitmap(fin);
			fin.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		return cachedBitmap;
	}

	private void cacheBitmap(Bitmap aTargetBitmap, File aCacheDest) throws FileNotFoundException, IOException
	{
		FileOutputStream fout = new FileOutputStream(aCacheDest);
		aTargetBitmap.compress(CompressFormat.WEBP, 100, fout);
		fout.close();
	}

	/**
	 * This method creates a Bitmap object or reuses the bitmap passed into the
	 * constructor
	 * 
	 * @param aIs
	 *            An inputstream that points to an image source
	 * @return A bitmap representing the contents of the inputstream
	 * @throws IOException
	 *             If reading data from the inputstream fails.
	 */
	public Bitmap createOrReuseBitmap(InputStream aIs) throws IOException
	{
		Options opts = new Options();
		if (mReuseBitmap == null)
		{
			opts.inSampleSize = 1;
			opts.inMutable = true;
			mReuseBitmap = BitmapFactory.decodeStream(aIs, null, opts);
			return mReuseBitmap;
		}

		if (mReuseBitmap.isMutable())
		{
			// TODO: Make sure the input bitmap is the same size as output
			// bitmap
			opts.inBitmap = mReuseBitmap;
			opts.inMutable = true;
			opts.inSampleSize = 1;
			mReuseBitmap = BitmapFactory.decodeStream(aIs, null, opts);
		}
		else
		{
			throw new IllegalStateException("Reuse bitmap is NOT mutable");
		}

		return mReuseBitmap;
	}

	/**
	 * Delegate method to ensure the task runs on the
	 * {@link #sImageLoadThreadPool}
	 * 
	 * @param aRemoteImageViewArray
	 */
	public void executeInParallel(View... aRemoteImageViewArray)
	{
		executeOnExecutor(sImageLoadThreadPool, aRemoteImageViewArray);
	}
}
