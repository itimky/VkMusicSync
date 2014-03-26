package com.timky.vkmusicsync.models;

import android.os.Environment;

import com.timky.vkmusicsync.helpers.AudioDownloader;
import com.timky.vkmusicsync.helpers.Downloader;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by timky on 21.03.14.
 */
public abstract class Downloadable {
    //private final DownloadInfo downloadInfo = new DownloadInfo();

    public abstract String getFileName();

    public String getFileFullName(String filePath){
        String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/" + filePath;
        String fullFileName = directoryPath + getFileName().replaceAll("[|?*<\":>/\\\\]", " ");

        return fullFileName;
    }

    public DownloadEventListener downloadListener;
    public Downloader task;
    private boolean isDownloaded = false;
    private boolean isDownloading = false;
    private double downloadedSize = 0;
    private double totalSize = 0;
    //private String fullFilePath;
    private final URL url;

    protected Downloadable(URL url){
        this.url = url;
    }

    protected Downloadable(String url) throws MalformedURLException {
        this.url = new URL(url);
    }

    public boolean isDownloaded() {
        return isDownloaded;
    }

    public void setDownloaded(boolean isDownloaded, String fullFilePath) {
        this.isDownloaded = isDownloaded;

        if (downloadListener != null)
            downloadListener.onDownloadedChanged(isDownloaded, fullFilePath);
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void setDownloading(boolean isDownloading) {
        this.isDownloading = isDownloading;

        if (isDownloading){
            downloadedSize = 0;
            totalSize = 0;
        }

        if (downloadListener != null)
            downloadListener.onDownloadingChanged(isDownloading);
    }

    public double getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(double downloadedSize) {
        this.downloadedSize = downloadedSize;

        if (downloadListener != null)
            downloadListener.onProgressChanged(downloadedSize, this.totalSize);
    }

    public double getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(double totalSize) {
        this.totalSize = totalSize;

        if (downloadListener != null)
            downloadListener.onProgressChanged(this.downloadedSize, totalSize);
    }

    public void setProgress(double downloadedSize, double totalSize){
        this.downloadedSize = downloadedSize;
        this.totalSize = totalSize;

        if (downloadListener != null)
            downloadListener.onProgressChanged(downloadedSize, totalSize);
    }

//    public String getFileFullPath() {
//        return fullFilePath;
//    }
    public URL getUrl(){
        return url;
    }

    public void raiseError(String errorMessage){
        if (downloadListener != null)
            downloadListener.onDownloadError(errorMessage);
    }
    //[\]\[|\\?*<":>+/']
}
