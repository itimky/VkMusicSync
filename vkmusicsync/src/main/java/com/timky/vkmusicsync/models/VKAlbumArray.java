package com.timky.vkmusicsync.models;

import android.os.Parcel;

import com.vk.sdk.api.model.VKApiArray;
import com.vk.sdk.api.model.VKList;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbumArray extends VKApiArray<VKAlbum> {

    @Override
    protected VKAlbum createObject() {
        return new VKAlbum();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
