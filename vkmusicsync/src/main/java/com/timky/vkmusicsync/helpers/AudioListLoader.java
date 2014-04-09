package com.timky.vkmusicsync.helpers;

import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;

import com.timky.vkmusicsync.models.IAlbumListLoadListener;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.VKAlbum;
import com.timky.vkmusicsync.models.VKAlbumArray;
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
import java.util.List;

/**
 * Don't use execute method!
 * Created by timky on 24.03.14.
 */
public class AudioListLoader extends AsyncTask<Void, Void, AudioListLoader.AudioListLoaderResult> {

    private static long mAlbumId;

    private static IAudioListLoadListener mAudioListLoadListener;
    private static IAlbumListLoadListener mAlbumListLoadListener;
    private static VKAlbumListAdapter mAlbumListAdapter;
    private static VKAudioListAdapter mAudioListAdapter;
    private static int mPageSize;
    private static int mCustomRefreshSize;
    private static int mOffset;
    private static int mBackupOffset;
    private static boolean mIsAlbumMode;
    private static boolean mIsLoading;

    public static void initialize(int pageSize){
        mPageSize = pageSize;
        mCustomRefreshSize = -1;
        mOffset = 0;
        mBackupOffset = 0;
        mIsAlbumMode = false;
        mIsLoading = false;
        mAlbumId = 0;
    }

    public static void setAlbumSupport(VKAlbumListAdapter albumListAdapter, IAlbumListLoadListener albumListener){
        mAlbumListAdapter = albumListAdapter;
        mAlbumListLoadListener = albumListener;
    }

    public static void setAudioSupport(VKAudioListAdapter audioListAdapter, IAudioListLoadListener audioListener){
        mAudioListAdapter = audioListAdapter;
        mAudioListLoadListener = audioListener;
    }

    public static void loadAlbums(){
        if (mAlbumListAdapter == null)
            throw new NullPointerException("AlbumListAdapter has not been initialized");

        mIsAlbumMode = true;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void refresh(){
        checkAudioAdapter();

        if (isLoading())
            return;

        mOffset = 0;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void refresh(int customRefreshSize){
        checkAudioAdapter();

        if (isLoading())
            return;

        mCustomRefreshSize = customRefreshSize;
        mOffset = 0;
        AudioListLoader loader = new AudioListLoader();
        loader.execute();
    }

    public static void loadMore(){
        checkAudioAdapter();

        if (isLoading())
            return;

        AudioListLoader loader = new AudioListLoader();

        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB )
            loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
         else
            loader.execute();
    }

    public static void setmAlbumId(long albumId) {
        mAlbumId = albumId;

        if (mAudioListLoadListener != null)
            mAudioListLoadListener.onAlbumSelected();
    }

    private static void checkAudioAdapter(){
        if (mAudioListAdapter == null)
            throw new NullPointerException("AudioListAdapter has not been initialized");
    }

    private static boolean isLoading(){
        return mIsLoading && !mIsAlbumMode;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        mIsLoading = true;

        if (mIsAlbumMode && mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadStarted();
        else
        if (mAudioListLoadListener != null) {
            if (mAudioListAdapter.anyTask() && mOffset == 0)
                cancel(false);
            else
                mAudioListLoadListener.onListLoadStarted(mOffset == 0);
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
                result.errorCode = error.errorCode;
            }
        });
    }

    private void loadAudioList(final AudioListLoaderResult result){
        int count = mCustomRefreshSize == -1 ? mPageSize : mCustomRefreshSize;

        VKRequest request = new VKRequest("audio.get", VKParameters.from(VKApiConst.COUNT, count, VKApiConst.OFFSET, mOffset, VKApiConst.ALBUM_ID, mAlbumId));
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
                result.errorCode = error.errorCode;
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

        if (mIsAlbumMode && mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadStarted();
        else if (mAudioListLoadListener != null)
            mAudioListLoadListener.onListLoadCanceled();

        mIsAlbumMode = false;
        mOffset = mBackupOffset;
        mCustomRefreshSize = -1;
    }

    private void onAlbumLoadPostExecute(AudioListLoaderResult result){
        mIsAlbumMode = false;

        if (result.albumList != null)
            mAlbumListAdapter.refresh(result.albumList);

        if (mAlbumListLoadListener != null)
            mAlbumListLoadListener.onListLoadFinished(result);
    }

    private void onAudioLoadPostExecute(AudioListLoaderResult result){
        if (result.audioInfoList != null && (result.audioInfoList.size() != 0 || mAlbumId != 0))
            if (mOffset == 0) {
                mAudioListAdapter.refresh(result.audioInfoList);

                if (mCustomRefreshSize == -1)
                    mOffset = mPageSize;
                else if (mCustomRefreshSize == 0)
                    mOffset = result.audioInfoList.size();
                else
                    mOffset = mCustomRefreshSize;

                if (result.audioInfoList.size() < mCustomRefreshSize) {
                    mCustomRefreshSize = -1;
                    loadMore();
                    return;
                }
            }
            else {
                mAudioListAdapter.addAll(result.audioInfoList);
                mOffset += mPageSize;
            }
        mBackupOffset = mOffset;
        mCustomRefreshSize = -1;

        if (mAudioListLoadListener != null)
            mAudioListLoadListener.onListLoadFinished(result);
    }

    class AudioListLoaderResult extends TaskResult {
        public List<VKAudioInfo> audioInfoList;
        public List<VKAlbum> albumList;
    }
}