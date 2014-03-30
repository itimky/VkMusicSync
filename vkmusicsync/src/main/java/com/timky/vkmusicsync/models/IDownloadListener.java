package com.timky.vkmusicsync.models;

/**
 * Created by timky on 20.03.14.
 */
public interface IDownloadListener {
    public void onDownloadPrepare(Downloadable downloadable);
    public void onDownloadBegin(Downloadable downloadable);
    public void onDownloadComplete(Downloadable downloadable);
    public void onDownloadCancel(Downloadable downloadable);
    public void onProgressChanged(double downloadedSize, double totalSize);
    public void onDownloadedChanged(boolean isDownloaded, String fullFileName);
    public void onDownloadError(TaskResult result);
}
