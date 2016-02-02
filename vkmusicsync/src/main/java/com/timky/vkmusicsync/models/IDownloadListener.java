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
    void onProgressChanged(double downloadedSize, double totalSize);
    void onDownloadPrepare(DownloadInfo downloadInfo);
    void onDownloadBegin(DownloadInfo downloadInfo);
    void onDownloadComplete(DownloadInfo downloadInfo);
    void onDownloadCancel(DownloadInfo downloadInfo);
    void onDownloadedChanged(boolean isDownloaded, String fullFileName);
    void onDownloadError(TaskResult result);
}
