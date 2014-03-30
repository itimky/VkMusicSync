package com.timky.vkmusicsync.models;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.timky.vkmusicsync.R;

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
        int downloading = audioInfo.isDownloading() ? View.VISIBLE : View.GONE;
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

//    @Override
//    public void onDownloadingChanged(Downloadable downloadable) {
//        boolean isDownloading = downloadable.isDownloading();
//        int visibility = isDownloading ? View.VISIBLE : View.GONE;
//
//        barProgress.setProgress(0);
//        barProgress.setVisibility(visibility);
//        barProgress.setIndeterminate(isDownloading);
//
//        textProgress.setVisibility(visibility);
//        textProgress.setText("");
//    }

    @Override
    public void onDownloadPrepare(Downloadable downloadable) {
        barProgress.setProgress(0);
        barProgress.setVisibility(View.VISIBLE);
        barProgress.setIndeterminate(true);

        textProgress.setVisibility(View.VISIBLE);
        textProgress.setText(
                mContext.getString(R.string.audio_list_item_progress_template,
                                downloadable.getDownloadedSize(), downloadable.getTotalSize()));
    }

    @Override
    public void onDownloadBegin(Downloadable downloadable) {

    }

    @Override
    public void onDownloadComplete(Downloadable downloadable) {
        completeDownload();
    }

    @Override
    public void onDownloadCancel(Downloadable downloadable) {
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

        // IsDownloading must be true to refresh sd-card media
        if (isDownloaded && fullFilePath != null && audioInfo.isDownloading())
            mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://"
                    + audioInfo.getFileFullName(fullFilePath))));
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
