package com.timky.vkmusicsync.models;

import android.os.Environment;

import com.timky.vkmusicsync.helpers.Downloader;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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

    private final List<IDownloadListener> mDownloadListenerList = new ArrayList<IDownloadListener>();
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

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadedChanged(isDownloaded, fullFilePath);
    }

    public boolean isDownloading() {
        return isDownloading;
    }

    public void prepareDownload(){
        isDownloading = true;
        downloadedSize = 0;
        totalSize = 0;

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onDownloadPrepare(this);
    }

    public void startDownload(){
        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener :toList(mDownloadListenerList))
            listener.onDownloadBegin(this);
    }

    public void completeDownload(){
        finishDownload();

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onDownloadComplete(this);
    }

    public void cancelDownload(){
        finishDownload();

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onDownloadCancel(this);
    }

    public double getDownloadedSize() {
        return downloadedSize;
    }

    public void setDownloadedSize(double downloadedSize) {
        this.downloadedSize = downloadedSize;

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onProgressChanged(downloadedSize, this.totalSize);
    }

    public double getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(double totalSize) {
        this.totalSize = totalSize;

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onProgressChanged(this.downloadedSize, totalSize);
    }

    public void setProgress(double downloadedSize, double totalSize){
        this.downloadedSize = downloadedSize;
        this.totalSize = totalSize;

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onProgressChanged(downloadedSize, totalSize);
    }

    public static <T> List<T> toList(List<T> list){
        List<T> result = new ArrayList<T>(list);

        return result;
    }

//    public String getFileFullPath() {
//        return fullFilePath;
//    }
    public URL getUrl(){
        return url;
    }

    public void raiseError(TaskResult result){
        finishDownload();

        // toList is needed to prevent exception if
        // one of subscribers will unsubscribe before iteration end
        for (IDownloadListener listener : toList(mDownloadListenerList))
            listener.onDownloadError(result);
    }

    public void subscribe(IDownloadListener listener){
        mDownloadListenerList.add(listener);
    }

    public void unsubscribe(IDownloadListener listener){
        mDownloadListenerList.remove(listener);
    }

    private void finishDownload() {
        isDownloading = false;
    }

    //[\]\[|\\?*<":>+/']
}
