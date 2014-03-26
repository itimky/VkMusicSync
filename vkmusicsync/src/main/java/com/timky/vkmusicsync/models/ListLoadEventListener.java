package com.timky.vkmusicsync.models;

/**
 * Created by timky on 24.03.14.
 */
public interface ListLoadEventListener {

    public void onAlbumLoadStarted();
    public void onAlbumLoadFinished(TaskResult result);

    public void onAudioLoadStarted(boolean fullRefresh);
    public void onAudioLoadFinished(TaskResult result);

    public void onLoadCanceled();
}
