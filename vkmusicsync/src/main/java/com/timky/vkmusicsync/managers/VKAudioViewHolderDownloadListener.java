package com.timky.vkmusicsync.managers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.View;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.DownloadInfo;
import com.timky.vkmusicsync.models.IDownloadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.ViewHolder;

/**
 * Created by timky on 28.04.14.
 */
public class VKAudioViewHolderDownloadListener implements IDownloadListener {
    private final ViewHolder mAudioViewHolder;
    private final Context mContext;

    public VKAudioViewHolderDownloadListener(ViewHolder mAudioViewHolder) {
        this.mAudioViewHolder = mAudioViewHolder;
        this.mContext = null;
    }


    @Override
    public void onDownloadPrepare(DownloadInfo downloadInfo) {
        mAudioViewHolder.barProgress.setProgress(0);
        mAudioViewHolder.barProgress.setVisibility(View.VISIBLE);
        mAudioViewHolder.barProgress.setIndeterminate(true);

        mAudioViewHolder.textProgress.setVisibility(View.VISIBLE);
        mAudioViewHolder.textProgress.setText(
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
        mAudioViewHolder.barProgress.setIndeterminate(false);
        mAudioViewHolder.barProgress.setProgress((int)(downloadedSize  * 100 / totalSize));
        mAudioViewHolder.textProgress.setText(mContext.getString(R.string.audio_list_item_progress_template, downloadedSize, totalSize));
    }

    @Override
    public void onDownloadedChanged(boolean isDownloaded, String fileUri) {

        mAudioViewHolder.downloaded.setVisibility(isDownloaded ? View.VISIBLE : View.GONE);

        // Updating media cache
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                Uri.parse(fileUri)));
        //Uri.parse("file://" + audioInfo.getFileFullName(fullFilePath))));
    }

    @Override
    public void onDownloadError(TaskResult result) {
        completeDownload();
        result.handleError(mContext);
    }

    private void completeDownload(){
        mAudioViewHolder.barProgress.setProgress(0);
        mAudioViewHolder.barProgress.setVisibility(View.GONE);
        mAudioViewHolder.barProgress.setIndeterminate(true);

        mAudioViewHolder.textProgress.setVisibility(View.GONE);
        mAudioViewHolder.textProgress.setText("");
    }
}
