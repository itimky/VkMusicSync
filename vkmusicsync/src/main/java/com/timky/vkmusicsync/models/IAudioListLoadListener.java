package com.timky.vkmusicsync.models;

/**
 * Created by timky on 24.03.14.
 */
public interface IAudioListLoadListener extends IListLoadListener {
    void onListLoadStarted();
    void onAlbumSelected(long albumId);
}
