package com.timky.vkmusicsync.models;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.timky.vkmusicsync.R;

/**
 * Created by timky on 14.03.14.
 */
public class ViewHolder implements DownloadEventListener {

    protected final Context context;
    public final TextView artist;
    public final TextView title;
    public final CheckBox checkBox;
    public final ProgressBar barProgress;
    public final TextView textProgress;
    protected VKAudioInfo audioInfo;

    public ViewHolder(Context context, TextView artist, TextView title, CheckBox checkBox, ProgressBar barProgress, TextView textProgress){
        this.context = context;
        this.artist = artist;
        this.title = title;
        this.checkBox = checkBox;
        this.barProgress = barProgress;
        this.textProgress = textProgress;
    }

    public VKAudioInfo getAudioInfo(){
        return audioInfo;
    }

    public void setAudioInfo(VKAudioInfo audioInfo){
        // If audioInfo (ai) != null - desubscribe
        if (this.audioInfo != null)
            this.audioInfo.downloadListener = null;

        this.audioInfo = audioInfo;

        // if ai downloading - progress is visible
        int visibility = audioInfo.isDownloading() ? View.VISIBLE : View.GONE;

        // if downloading hasn't started - progress is indeterminate
        boolean isIndeterminate = false;
        if (audioInfo.getTotalSize() == 0){
            isIndeterminate = true;
            textProgress.setText("");
        }

        artist.setText(audioInfo.artist);
        title.setText(audioInfo.title);
        barProgress.setIndeterminate(isIndeterminate);
        barProgress.setVisibility(visibility);
        textProgress.setVisibility(visibility);
        checkBox.setChecked(audioInfo.isDownloaded());
        audioInfo.downloadListener = this;
    }

    @Override
    public void onDownloadingChanged(boolean isDownloading) {
        int visibility = isDownloading ? View.VISIBLE : View.GONE;

        barProgress.setProgress(0);
        barProgress.setVisibility(visibility);
        barProgress.setIndeterminate(isDownloading);

        textProgress.setVisibility(visibility);
        textProgress.setText("");
    }

    @Override
    public void onProgressChanged(double downloadedSize, double totalSize) {
        barProgress.setIndeterminate(false);
        barProgress.setProgress((int)(downloadedSize  * 100 / totalSize));
        textProgress.setText(context.getString(R.string.progress_state, downloadedSize, totalSize));
    }

    @Override
    public void onDownloadedChanged(boolean isDownloaded, String fullFilePath) {
        checkBox.setChecked(isDownloaded);

        // IsDownloading must be true to refresh sd-card media
        if (isDownloaded && fullFilePath != null && audioInfo.isDownloading())
            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                    + audioInfo.getFileFullName(fullFilePath))));
    }

    @Override
    public void onDownloadError(String errorMessage) {
        Toast.makeText(context, errorMessage,
                Toast.LENGTH_SHORT).show();
    }
}
