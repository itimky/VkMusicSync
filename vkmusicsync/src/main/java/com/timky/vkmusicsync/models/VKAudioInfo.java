package com.timky.vkmusicsync.models;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import com.vk.sdk.api.model.VKAudio;
import com.vk.sdk.api.model.VKAudioArray;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 3/8/14.
 */
public class VKAudioInfo extends Downloadable {
    public final long id;
    public final String artist;
    public final String title;

    public VKAudioInfo(VKAudio audio) throws MalformedURLException {
        super(audio.url);
        this.id = audio.id;
        this.artist = audio.artist;
        this.title = audio.title;
    }

    @Override
    public String getFileName(){
        return artist + " - " + title + ".mp3";
    }

    public static List<VKAudioInfo> toList(VKAudioArray audios) throws MalformedURLException {
        List<VKAudioInfo> result = new ArrayList<VKAudioInfo>();

        for (int i = 0; i < audios.size(); i++)
            result.add(new VKAudioInfo(audios.get(i)));

        return result;
    }
}