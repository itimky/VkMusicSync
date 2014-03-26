package com.timky.vkmusicsync.helpers;

import android.os.AsyncTask;
import android.util.Log;

import com.timky.vkmusicsync.models.ListLoadEventListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.VKAlbum;
import com.timky.vkmusicsync.models.VKAlbumArray;
import com.timky.vkmusicsync.models.VKAudioInfo;
import com.vk.sdk.api.VKApiConst;
import com.vk.sdk.api.VKError;
import com.vk.sdk.api.VKParameters;
import com.vk.sdk.api.VKRequest;
import com.vk.sdk.api.VKResponse;
import com.vk.sdk.api.model.VKAudioArray;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.List;

/**
 * Don't use execute method!
 * Created by timky on 24.03.14.
 */
public class AudioListLoader extends AsyncTask<Void, Void, AudioListLoader.AudioListLoaderResult> {

    public static long albumId;

    private static ListLoadEventListener mListLoadListener;
    private static VKAlbumListAdapter mAlbumAdapter;
    private static VKAudioListAdapter mAudioListAdapter;
    private static int mPageSize;
    private static int mCustomRefreshSize;
    private static int mOffset;
    private static boolean mIsAlbumMode;
    private static boolean mIsLoading;

    public static void init(ListLoadEventListener listener, VKAlbumListAdapter albumAdapter, VKAudioListAdapter audioAdapter, int pageSize){
        mListLoadListener = listener;
        mAlbumAdapter = albumAdapter;
        mAudioListAdapter = audioAdapter;
        mPageSize = pageSize;
        mCustomRefreshSize = 0;
        mOffset = 0;
        mIsAlbumMode = false;
        mIsLoading = false;

        albumId = 0;
    }

    public static void loadAlbums(){
        mIsAlbumMode = true;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void refresh(){
        mOffset = 0;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void refresh(int customRefreshSize){
        mCustomRefreshSize = customRefreshSize;
        mOffset = 0;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void loadMore(){
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        if (mIsLoading || mAudioListAdapter.anyTask())
            cancel(false);
        else {
            mIsLoading = true;

            if (mListLoadListener != null)
                if (mIsAlbumMode)
                    mListLoadListener.onAlbumLoadStarted();
                else
                    mListLoadListener.onAudioLoadStarted(mOffset == 0);
        }

    }

    /**
     *
     * @param   params
     *          params[0] - Count
     *          params[1] - Offset
     */
    @Override
    protected AudioListLoaderResult doInBackground(Void... params) {
        final AudioListLoaderResult result = new AudioListLoaderResult();

        if (isCancelled())
            return result;

        if (mIsAlbumMode)
            loadAlbumList(result);
        else
            loadAudioList(result);

        return result;
    }

    private void loadAlbumList(final AudioListLoaderResult result){
        VKRequest request = new VKRequest("audio.getAlbums");
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                try {
                    // Gettig json array of items
                    JSONArray json = response.json.getJSONObject("response").getJSONArray("items");

                    // Parsing items to array of audios
                    VKAlbumArray albums = new VKAlbumArray();

                    albums.parse(json);
                    result.albumList = VKAlbum.toList(albums);
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
            }
        });
    }

    private void loadAudioList(final AudioListLoaderResult result){
        int count = mCustomRefreshSize == 0 ? mPageSize : mCustomRefreshSize;

        VKRequest request = new VKRequest("audio.get", VKParameters.from(VKApiConst.COUNT, count, VKApiConst.OFFSET, mOffset, VKApiConst.ALBUM_ID, albumId));
        request.executeWithListener(new VKRequest.VKRequestListener() {
            @Override
            public void onComplete(VKResponse response) {
                super.onComplete(response);

                try {
                    // Gettig json array of items
                    JSONArray json = response.json.getJSONObject("response").getJSONArray("items");

                    // Parsing items to array of audios
                    VKAudioArray audios = new VKAudioArray();

                    audios.parse(json);
                    result.audioInfoList = VKAudioInfo.toList(audios);
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
            }
        });
    }



    @Override
    protected void onPostExecute(AudioListLoaderResult result) {
        super.onPostExecute(result);
        mIsLoading = false;

        if (mIsAlbumMode)
            onAlbumLoadPostExecute(result);
        else
            onAudioLoadPostExecute(result);


    }

    @Override
    protected void onCancelled() {
        super.onCancelled();

        mIsLoading = false;

        if (mListLoadListener != null)
            mListLoadListener.onLoadCanceled();
        //mPullToRefreshView.onRefreshComplete();
    }



    private void onAlbumLoadPostExecute(AudioListLoaderResult result){
        mIsAlbumMode = false;

        if (result.albumList != null)
            mAlbumAdapter.refresh(result.albumList);

        if (mListLoadListener != null)
            mListLoadListener.onAlbumLoadFinished(result);
    }

    private void onAudioLoadPostExecute(AudioListLoaderResult result){
        if (result.audioInfoList != null && (result.audioInfoList.size() != 0 || albumId != 0))
            if (mOffset == 0) {
                mAudioListAdapter.refresh(result.audioInfoList);
                mOffset = mCustomRefreshSize == 0 ? mPageSize : mCustomRefreshSize;

                if (result.audioInfoList.size() < mCustomRefreshSize) {
                    mCustomRefreshSize = 0;
                    loadMore();
                    return;
                }
            }
            else {
                mAudioListAdapter.addAll(result.audioInfoList);
                mOffset += mPageSize;
            }

        if (mListLoadListener != null)
            mListLoadListener.onAudioLoadFinished(result);
    }

    class AudioListLoaderResult extends TaskResult {
        public List<VKAudioInfo> audioInfoList;
        public List<VKAlbum> albumList;
    }
}