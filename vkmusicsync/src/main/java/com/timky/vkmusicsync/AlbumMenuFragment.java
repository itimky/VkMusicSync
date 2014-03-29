package com.timky.vkmusicsync;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.timky.vkmusicsync.helpers.AudioListLoader;
import com.timky.vkmusicsync.helpers.VKAlbumListAdapter;
import com.timky.vkmusicsync.models.IAlbumListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;

public class AlbumMenuFragment extends ListFragment implements IAlbumListLoadListener {
    private RelativeLayout mAlbumLoadingLayout;

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_album_list, null);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        mAlbumLoadingLayout = (RelativeLayout)activity.findViewById(R.id.album_loading_layout);

        VKAlbumListAdapter albumListAdapter = new VKAlbumListAdapter(activity);
        setListAdapter(albumListAdapter);

        AudioListLoader.setAlbumSupport(albumListAdapter, this);
        AudioListLoader.loadAlbums();
	}

    @Override
    public void onListLoadStarted() {
        mAlbumLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onListLoadCanceled() {
        mAlbumLoadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void onListLoadFinished(TaskResult result) {
        if (result.errorMessage != null)
            Toast.makeText(getActivity(), result.errorMessage,
                    Toast.LENGTH_SHORT).show();

        mAlbumLoadingLayout.setVisibility(View.GONE);
    }
}
