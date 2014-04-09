package com.timky.vkmusicsync.models;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.helpers.DownloadManager;

/**
 * Created by timky on 14.03.14.
 */
public class ViewHolder implements IDownloadListener {

    protected final Context mContext;
    public final TextView artist;
    public final TextView title;
    public final ImageView downloaded;
    public final ProgressBar barProgress;
    public final TextView textProgress;
    protected VKAudioInfo audioInfo;

    public ViewHolder(Context context, TextView artist, TextView title, ImageView downloaded, ProgressBar barProgress, TextView textProgress){
        this.mContext = context;
        this.artist = artist;
        this.title = title;
        this.downloaded = downloaded;
        this.barProgress = barProgress;
        this.textProgress = textProgress;
    }

    public VKAudioInfo getAudioInfo(){
        return audioInfo;
    }

    public void setAudioInfo(VKAudioInfo audioInfo){
        // If audioInfo (ai) != null - unsubscribe
        if (this.audioInfo != null)
            this.audioInfo.unsubscribe(this);

        this.audioInfo = audioInfo;

        // if ai downloading - progress is visible
        int downloading = audioInfo.getTaskId() != DownloadManager.mNoTaskId ? View.VISIBLE : View.GONE;
        int isDownloaded = audioInfo.isDownloaded() ? View.VISIBLE : View.GONE;

        // if downloading hasn't started - progress is indeterminate
        boolean isIndeterminate = false;
        if (audioInfo.getTotalSize() == 0){
            isIndeterminate = true;
            textProgress.setText("");
        }

        artist.setText(audioInfo.artist);
        title.setText(audioInfo.title);
        barProgress.setIndeterminate(isIndeterminate);
        barProgress.setVisibility(downloading);
        textProgress.setVisibility(downloading);
        downloaded.setVisibility(isDownloaded);
        audioInfo.subscribe(this);
    }

    @Override
    public void onDownloadPrepare(DownloadInfo downloadInfo) {
        barProgress.setProgress(0);
        barProgress.setVisibility(View.VISIBLE);
        barProgress.setIndeterminate(true);

        textProgress.setVisibility(View.VISIBLE);
        textProgress.setText(
                mContext.getString(R.string.audio_list_item_progress_template,
                                downloadInfo.getDownloadedSize(), downloadInfo.getTotalSize()));
    }

    @Override
    public void onDownloadBegin(DownloadInfo downloadInfo) {

    }

    @Override
    public void onDownloadComplete(DownloadInfo downloadInfo) {
        completeDownload();
    }

    @Override
    public void onDownloadCancel(DownloadInfo downloadInfo) {
        completeDownload();
    }

    @Override
    public void onProgressChanged(double downloadedSize, double totalSize) {
        barProgress.setIndeterminate(false);
        barProgress.setProgress((int)(downloadedSize  * 100 / totalSize));
        textProgress.setText(mContext.getString(R.string.audio_list_item_progress_template, downloadedSize, totalSize));
    }

    @Override
    public void onDownloadedChanged(boolean isDownloaded, String fullFilePath) {

        this.downloaded.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);

        // Updating media cache
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse("file://" + audioInfo.getFileFullName(fullFilePath))));
    }

    @Override
    public void onDownloadError(TaskResult result) {
        completeDownload();
        result.handleError(mContext);
    }

    private void completeDownload(){
        barProgress.setProgress(0);
        barProgress.setVisibility(View.GONE);
        barProgress.setIndeterminate(true);

        textProgress.setVisibility(View.GONE);
        textProgress.setText("");
    }
}
