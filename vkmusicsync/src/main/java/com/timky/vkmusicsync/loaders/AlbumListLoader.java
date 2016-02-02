package com.timky.vkmusicsync.loaders;
import android.util.Log;

import com.timky.vkmusicsync.adapters.VKAlbumListAdapter;
import com.timky.vkmusicsync.models.IAlbumListLoadListener;
import com.timky.vkmusicsync.models.VKAlbum;
import com.timky.vkmusicsync.models.VKAlbumArray;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;

public final class AlbumListLoader extends ListLoader<VKAlbum> {
    private final IAlbumListLoadListener mAlbumListLoadListener;
    private final VKAlbumListAdapter mAlbumListAdapter;

    public AlbumListLoader(VKAlbumListAdapter adapter, IAlbumListLoadListener listener) {
        mAlbumListAdapter = adapter;
        mAlbumListLoadListener = listener;
    }

    @Override
    protected void preLoad() {
        if (mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadStarted();
    }

    @Override
    protected Result load(int count, int offset) {
        final Result result = new Result();
        VKRequest request = new VKRequest("audio.getAlbums");
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                try {
                    JSONArray json = response.json.getJSONObject("response").getJSONArray("items");
                    // Parsing items to array of audios
                    VKAlbumArray albums = new VKAlbumArray();
                    albums.parse(json);
                    result.loaded = VKAlbum.toList(albums);
                } catch (JSONException e) {
                    e.printStackTrace();
                    result.errorMessage = "JSONException...";
                }
            }
            @Override
            public void onError(VKError error) {
                super.onError(error);
                String message = "Unknown error";

                if (error.apiError != null && error.apiError.errorMessage != null)
                    message = error.apiError.errorMessage;
                else if (error.errorMessage != null)
                    message = error.errorMessage;

                Log.e("Loading list of audio failed: ", message);

                result.errorMessage = message;
                result.errorCode = error.errorCode;
            }
        });
        return result;
    }

    @Override
    protected void postLoad(ListLoader.Result result) {
        if (result.loaded != null)
            mAlbumListAdapter.refresh(result.loaded);

        if (mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadFinished(result);
    }

    @Override
    protected void cancelled() {
        if (mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadCanceled();
    }
}