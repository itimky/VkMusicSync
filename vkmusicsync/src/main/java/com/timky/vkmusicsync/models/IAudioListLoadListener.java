package com.timky.vkmusicsync.models;

import com.timky.vkmusicsync.helpers.AudioListLoader;

/**
 * Created by timky on 24.03.14.
 */
public interface IAudioListLoadListener extends IListLoadListener {
    public void onListLoadStarted(boolean fullRefresh);
    public void onAlbumSelected();
}
