package com.timky.vkmusicsync.models;

import android.os.Parcel;

import com.vk.sdk.api.model.VKApiArray;
import com.vk.sdk.api.model.VKApiAudio;

/**
 * Created by timky on 10.04.14.
 */
public class VKAudioArray extends VKApiArray<VKApiAudio> {
    @Override
    protected VKApiAudio createObject() {
        return new VKApiAudio();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

    }
}
