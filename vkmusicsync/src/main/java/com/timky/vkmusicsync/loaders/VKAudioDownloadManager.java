package com.timky.vkmusicsync.loaders;

import com.mpatric.mp3agic.ID3v1;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.ID3v24Tag;
import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.NotSupportedException;
import com.mpatric.mp3agic.UnsupportedTagException;

import com.timky.vkmusicsync.models.DownloadInfo;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.TaskState;
import com.timky.vkmusicsync.models.VKAudioInfo;

import java.io.File;
import java.io.IOException;

/**
 * Created by timky on 14.03.14.
 */
// This class downloads mp3 file and sets it's tags
public class VKAudioDownloadManager extends DownloadManager {

    public VKAudioDownloadManager(String filePath, String tempFileName){
        super(filePath);
        setTempFileName(tempFileName);
    }

    public void download(VKAudioInfo audioInfo) {
        super.download(audioInfo);
    }

    @Override
    protected TaskResult completeExecuteInBackground(DownloadTask task, TaskResult result){
        //if (m.taskState != TaskState.DOWNLOADING)
        //    return result;

        VKAudioInfo audioInfo = (VKAudioInfo)task.mDownloadInfo;

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

            id3v2Tag.setArtist(audioInfo.artist);
            id3v2Tag.setTitle(audioInfo.title);

            mp3File.setId3v2Tag(id3v2Tag);

            String fullFileName = audioInfo.getFileFullName(mFilePath);
            mp3File.save(fullFileName);

            File file = new File(result.fullFileName);
            file.delete();

            result.fullFileName = fullFileName;
            task.setState(TaskState.COMPLETE);

        } catch (IOException e) {
            e.printStackTrace();
            result.errorMessage = "IOException...";
            task.setState(TaskState.ERROR);
        } catch (UnsupportedTagException e) {
            e.printStackTrace();
            result.errorMessage = "UnsupportedTagException...";
            task.setState(TaskState.ERROR);
        } catch (InvalidDataException e) {
            e.printStackTrace();
            result.errorMessage = "InvalidDataException...";
            task.setState(TaskState.ERROR);
        } catch (NotSupportedException e) {
            e.printStackTrace();
            result.errorMessage = "NotSupportedException... Can't save file \""
                    + audioInfo.getFileName() + "\"";
            task.setState(TaskState.ERROR);
        }

        return result;
    }

    /**
     *
     * Use "download(VKAudioInfo audioInfo)"
     */
    @Deprecated
    @Override
    public void download(DownloadInfo downloadInfo) {
    }
}
