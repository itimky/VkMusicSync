package com.timky.vkmusicsync.models;

/**
 * Created by timky on 20.03.14.
 */
public interface DownloadEventListener {
    public void onDownloadingChanged(boolean isDownloading);
    public void onProgressChanged(double downloadedSize, double totalSize);
    public void onDownloadedChanged(boolean isDownloaded, String fullFileName);
    public void onDownloadError(String errorMessage);
}
