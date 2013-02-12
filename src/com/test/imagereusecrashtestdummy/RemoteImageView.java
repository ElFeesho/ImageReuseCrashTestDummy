package com.test.imagereusecrashtestdummy;

import java.io.File;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.AttributeSet;
import android.widget.ImageView;

public class RemoteImageView extends ImageView
{
	private static final float ASPECT_RATIO = 0.35f;
	private final File mCacheDir;

	private Bitmap mReuseBitmap = null;

	public RemoteImageView(Context aContext)
	{
		this(aContext, null, 0);
	}

	public RemoteImageView(Context aContext, AttributeSet aAttrs)
	{
		this(aContext, aAttrs, 0);
	}

	public RemoteImageView(Context aContext, AttributeSet aAttrs, int aDefStyle)
	{
		super(aContext, aAttrs, aDefStyle);
		mCacheDir = aContext.getCacheDir();
	}

	public void setRemoteUri(Uri aUri)
	{
		Uri lastUri = (Uri) getTag(R.id.image_uri);
		if (lastUri != null && lastUri.compareTo(aUri) == 0)
		{
			// Do nothing.
			return;
		}

		setImageBitmap(null);
		setImageDrawable(null);
		// Load the image now

		setTag(R.id.image_uri, aUri);
		new CachedImageLoaderTask(mCacheDir, mReuseBitmap)
		{
			@Override
			protected void onPostExecute(Bitmap result)
			{
				if (result != null)
				{
					setImageBitmap(result);
					mReuseBitmap = result;
				}
			}
		}.executeInParallel(this);
	}

	// Make sure that the list item size doesn't jump around crazily.
	@Override
	protected void onMeasure(int aWidthMeasureSpec, int aHeightMeasureSpec)
	{
		aHeightMeasureSpec = MeasureSpec.makeMeasureSpec((int) (MeasureSpec.getSize(aWidthMeasureSpec) * ASPECT_RATIO), MeasureSpec.EXACTLY);
		super.onMeasure(aWidthMeasureSpec, aHeightMeasureSpec);
	}
}
