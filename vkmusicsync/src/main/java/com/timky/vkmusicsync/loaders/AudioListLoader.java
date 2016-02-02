package com.timky.vkmusicsync.loaders;
import android.util.Log;

import com.timky.vkmusicsync.adapters.VKAudioListAdapter;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.VKAudioArray;
import com.timky.vkmusicsync.models.VKAudioInfo;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.MalformedURLException;

public class AudioListLoader extends ListLoader<VKAudioInfo> {
    private final VKAudioListAdapter mAudioListAdapter;
    private final IAudioListLoadListener mAudioListLoadListener;
    private long mAlbumId;
    public long getAlbumId() {
        return mAlbumId;
    }
    public void setAlbumId(long albumId) {
        this.mAlbumId = albumId;
    }

    private String mSearchQuery;
    public String getSearchQuery() {
        return mSearchQuery;
    }
    public void setSearchQuery(String searchQuery) {
        this.mSearchQuery = searchQuery;
    }


    public AudioListLoader(VKAudioListAdapter audioListAdapter, IAudioListLoadListener audioListLoadListener) {
        this.mAudioListAdapter = audioListAdapter;
        this.mAudioListLoadListener = audioListLoadListener;
    }

    @Override
    protected void preLoad() {
        if (mAudioListLoadListener != null) {
            if (mAudioListAdapter.anyTask())
                cancel(false);
            else
                mAudioListLoadListener.onListLoadStarted();
        }
    }

    @Override
    protected void cancelled() {
        if (mAudioListLoadListener != null)
            mAudioListLoadListener.onListLoadCanceled();
    }

    @Override
    protected Result load(int count, int offset) {
        final Result result = new Result();
        VKRequest request;
        if (mSearchQuery != null)
            request = new VKRequest("audio.get", VKParameters.from(VKApiConst.COUNT, count, VKApiConst.OFFSET, offset, VKApiConst.ALBUM_ID, mAlbumId));
        else
            request = new VKRequest("audio.get");

        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                try {
                    JSONArray json = response.json.getJSONObject("response").getJSONArray("items");
                    // Parsing items to array of audios
                    VKAudioArray audios = new VKAudioArray();

                    audios.parse(json);
                    result.loaded = VKAudioInfo.toList(audios);
                } catch (JSONException e) {
                    e.printStackTrace();
                    result.errorMessage = "JSONException...";
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    result.errorMessage = "MalformedURLException... Can't parse URL";
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
        if (result.loaded != null && (result.loaded.size() != 0 || mAlbumId != 0))
                mAudioListAdapter.addAll(result.loaded);

        if (mAudioListLoadListener != null)
            mAudioListLoadListener.onListLoadFinished(result);
    }

    class AudioRequestListener extends VKRequest.VKRequestListener {
        private final Result result;
        protected AudioRequestListener(Result result)
        {
            this.result = result;
        }

        @Override
        public void onComplete(VKResponse response) {
            super.onComplete(response);

            try {
                JSONArray json = response.json.getJSONObject("response").getJSONArray("items");
                // Parsing items to array of audios
                VKAudioArray audios = new VKAudioArray();

                audios.parse(json);
                result.loaded = VKAudioInfo.toList(audios);
            } catch (JSONException e) {
                e.printStackTrace();
                result.errorMessage = "JSONException...";
            } catch (MalformedURLException e) {
                e.printStackTrace();
                result.errorMessage = "MalformedURLException... Can't parse URL";
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
    }
}