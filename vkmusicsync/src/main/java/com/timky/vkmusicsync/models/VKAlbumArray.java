package com.timky.vkmusicsync.models;

import com.vk.sdk.api.model.VKApiArray;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbumArray extends VKApiArray<VKAlbum> {

    @Override
    protected VKAlbum createObject() {
        return new VKAlbum();
    }
}
