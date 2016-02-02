package com.timky.vkmusicsync;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.timky.vkmusicsync.adapters.VKAlbumListAdapter;
import com.timky.vkmusicsync.models.IAlbumListLoadListener;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.loaders.AlbumListLoader;

public class AlbumMenuFragment extends ListFragment implements IAlbumListLoadListener {
    private VKAlbumListAdapter mAlbumListAdapter;
    private RelativeLayout mAlbumLoadingLayout;
    private AlbumListLoader mAlbumListLoader;
    private AlbumListLoader getAlbumListLoader() {
        if (mAlbumListLoader == null)
            mAlbumListLoader = new AlbumListLoader(mAlbumListAdapter, this);
        return mAlbumListLoader;
    }
    private IAudioListLoadListener mAlbumListener;
    public void setAlbumListener(IAudioListLoadListener listener) {
        mAlbumListener = listener;
    }

	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_album_list, null);
	}

	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        FragmentActivity activity = getActivity();
        mAlbumLoadingLayout = (RelativeLayout)activity.findViewById(R.id.album_loading_layout);

        mAlbumListAdapter = new VKAlbumListAdapter(activity, this);
        setListAdapter(mAlbumListAdapter);
        getAlbumListLoader().execute(20, 0);
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
        mAlbumLoadingLayout.setVisibility(View.GONE);
        result.handleError(getActivity());
    }

    public void onAlbumSelected(long albumId){
        mAlbumListener.onAlbumSelected(albumId);
    }
}
