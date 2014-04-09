package com.timky.vkmusicsync.models;

import com.vk.sdk.api.model.VKApiArray;
import com.vk.sdk.api.model.VKApiAudio;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 3/8/14.
 */
public class VKAudioInfo extends DownloadInfo{
    public final long id;
    public final String artist;
    public final String title;

    public VKAudioInfo(VKApiAudio audio) throws MalformedURLException {
        super(audio.url, audio.artist + " - " + audio.title, ".mp3");
        this.id = audio.id;
        this.artist = audio.artist;
        this.title = audio.title;
    }

    public static List<VKAudioInfo> toList(VKApiArray<VKApiAudio> audios) throws MalformedURLException {
        List<VKAudioInfo> result = new ArrayList<VKAudioInfo>();

        for (int i = 0; i < audios.size(); i++)
            result.add(new VKAudioInfo(audios.get(i)));

        return result;
    }
}