package com.timky.vkmusicsync.models;

import com.vk.sdk.api.model.VKApiModel;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbum extends VKApiModel {
    public long album_id;       // Album id
    public long owner_id;       // Owner Id
    public String title;        // Album title

    public static List<VKAlbum> toList(VKAlbumArray albumArray){
        List<VKAlbum> result = new ArrayList<VKAlbum>();

        for (int i = 0; i < albumArray.size(); i++)
            result.add(albumArray.get(i));

        return result;
    }
}
