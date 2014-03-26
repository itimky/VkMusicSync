package com.timky.vkmusicsync.helpers;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.VKAudioInfo;

import java.io.File;
import java.io.IOException;

/**
 * Created by timky on 14.03.14.
 */
// This class downloads mp3 file and sets it's tags
public class AudioDownloader extends Downloader{

    private AudioDownloader(VKAudioInfo audioInfo, String filePath){
        super(audioInfo, filePath);
    }

    private String mArtist;
    private String mTitle;

    @Override
    protected TaskResult doInBackground(Object... params) {
        TaskResult result = super.doInBackground(params);

        if (isCancelled() || result.errorMessage != null || result.fullFileName == null)
            return result;

        try {
            Mp3File mp3File = new Mp3File(result.fullFileName);
            String album = null;
            String year = null;
            int genreId = -1;

            if (mp3File.hasId3v1Tag()) {
                ID3v1 id3v1Tag = mp3File.getId3v1Tag();
                album = id3v1Tag.getAlbum();
                year = id3v1Tag.getYear();
                genreId = id3v1Tag.getGenre();
                mp3File.removeId3v1Tag();
            }

            ID3v2 id3v2Tag;
            if (mp3File.hasId3v2Tag()) {
                id3v2Tag = mp3File.getId3v2Tag();
                album = id3v2Tag.getAlbum();
                year = id3v2Tag.getYear();
                genreId = id3v2Tag.getGenre();
                mp3File.removeId3v2Tag();
            }

            id3v2Tag = new ID3v24Tag();

            if (album != null && id3v2Tag.getAlbum() == null)
                id3v2Tag.setAlbum(album);
            if (year != null && id3v2Tag.getYear() == null)
                id3v2Tag.setYear(year);
            if (genreId != -1 && id3v2Tag.getGenre() == -1)
                id3v2Tag.setGenre(genreId);

            id3v2Tag.setArtist(mArtist);
            id3v2Tag.setTitle(mTitle);

            mp3File.setId3v2Tag(id3v2Tag);

            String fullFileName = mDownloadable.getFileFullName(mFilePath);
            mp3File.save(fullFileName);

            File file = new File(result.fullFileName);
            file.delete();

            result.fullFileName = fullFileName;

        } catch (IOException e) {
            e.printStackTrace();
            result.errorMessage = "IOException...";
        } catch (UnsupportedTagException e) {
            e.printStackTrace();
            result.errorMessage = "UnsupportedTagException...";
        } catch (InvalidDataException e) {
            e.printStackTrace();
            result.errorMessage = "InvalidDataException...";
        } catch (NotSupportedException e) {
            e.printStackTrace();
            result.errorMessage = "NotSupportedException... Can't save file \""
                                            + mDownloadable.getFileName() + "\"";
        }

        return result;
    }

    public static void createTask(VKAudioInfo audioInfo, final String filePath) {
        AudioDownloader audioDownloader = new AudioDownloader(audioInfo, filePath);
        audioDownloader.mArtist = audioInfo.artist;
        audioDownloader.mTitle = audioInfo.title;
        audioInfo.task = audioDownloader;
    }
}
