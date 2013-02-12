package com.test.imagereusecrashtestdummy;

import android.app.ListActivity;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class MainActivity extends ListActivity
{
	public static class ImageListAdapter extends BaseAdapter
	{
		private static final Uri URIS[] = { Uri.parse("http://placekitten.com/640/230"), Uri.parse("http://placepuppy.it/640/230"), Uri.parse("http://placehold.it/640x230") };

		@Override
		public int getCount()
		{
			return 1000;
		}

		@Override
		public Uri getItem(int aPosition)
		{
			return URIS[aPosition % 3];
		}

		@Override
		public long getItemId(int aPosition)
		{
			return 0;
		}

		@Override
		public View getView(int aPosition, View aConvertView, ViewGroup aParent)
		{
			if (aConvertView == null)
			{
				aConvertView = new RemoteImageView(aParent.getContext());
			}

			((RemoteImageView) aConvertView).setRemoteUri(getItem(aPosition));

			return aConvertView;
		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setListAdapter(new ImageListAdapter());
	}
}
