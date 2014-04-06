package com.timky.vkmusicsync.models;

/**
 * Created by timky on 20.03.14.
 */
public interface IDownloadListener {
    /**
     *
     * @param downloadedSize in MB
     * @param totalSize in MB
     */
    public void onProgressChanged(double downloadedSize, double totalSize);
    public void onDownloadPrepare(DownloadInfo downloadInfo);
    public void onDownloadBegin(DownloadInfo downloadInfo);
    public void onDownloadComplete(DownloadInfo downloadInfo);
    public void onDownloadCancel(DownloadInfo downloadInfo);
    public void onDownloadedChanged(boolean isDownloaded, String fullFileName);
    public void onDownloadError(TaskResult result);
}
