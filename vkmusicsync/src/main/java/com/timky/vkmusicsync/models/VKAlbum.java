package com.timky.vkmusicsync.models;

import android.os.Parcel;

import com.vk.sdk.api.model.Identifiable;
import com.vk.sdk.api.model.VKApiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbum extends VKApiModel implements Identifiable, android.os.Parcelable, Comparable {// extends VKApiModel implements Comparable {
    public int id;       // Album id
    public int owner_id;       // Owner Id
    public String title;        // Album title

    public static List<VKAlbum> toList(VKAlbumArray albumArray){
        List<VKAlbum> result = new ArrayList<VKAlbum>();

        for (int i = 0; i < albumArray.size(); i++)
            result.add(albumArray.get(i));

        return result;
    }

    private boolean mIsSelected = false;
    public boolean isSelected(){
        return mIsSelected;
    }

    public void setIsSelected(boolean isSelected){
        mIsSelected = isSelected;

        if (listener != null){
            listener.onAlbumSelected(isSelected);
        }
    }

    public IAlbumSelectedListener listener;

    @Override
    public int compareTo(Object another) {
        return title.compareTo(((VKAlbum)another).title);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.owner_id);
        dest.writeString(this.title);
    }
}
