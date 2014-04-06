package com.timky.vkmusicsync.models;

import android.os.Environment;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Object with filename. Can notify about state changes
 * Created by timky on 21.03.14.
 */
public class DownloadInfo implements Comparable<DownloadInfo> {

    private final AsyncList<IDownloadListener> mDownloadListenerList = new AsyncList<IDownloadListener>();
    private double mDownloadedSize = 0;
    private double mTotalSize = 0;
    //private TaskState mTaskState = TaskState.NONE;
    private boolean mIsDownloaded = false;
    private final URL mUrl;
    private String mFilePath = "";
    private final String mFileName;
    private final String mFileExtension;
    private int mTaskId = -1;

    public String getFileFullName(String filePath){
        mFilePath = Environment.getExternalStorageDirectory().getPath() + "/" + filePath;
        String fullFileName = mFilePath + mFileName.replaceAll("[|?*<\":>/\\\\]", " ") + mFileExtension;

        return fullFileName;
    }

    public DownloadInfo(String url, String fileName, String fileExtension) throws MalformedURLException {
        mUrl = new URL(url);
        mFileName = fileName;
        mFileExtension = fileExtension.contains(".") ? fileExtension : "." + fileExtension;
    }

//    public TaskState getState() {
//        return mTaskState;
//    }

    public void setDownloaded(boolean isDownloaded, String fullFilePath) {
        mIsDownloaded = isDownloaded;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadedChanged(isDownloaded, fullFilePath);

//        if (mTaskState == TaskState.JUST_DOWNLOADED)
//            mTaskState = TaskState.DOWNLOADED;
    }

//    public final boolean isTaskRunning(){
//        return mTaskState == TaskState.PREPARE ||
//                mTaskState == TaskState.DOWNLOADING;
//    }

//    public final boolean isDownloading() {
//        return mTaskState == TaskState.DOWNLOADING;
//    }
    public final boolean isDownloaded() {
        return mIsDownloaded;
    }

    public void prepareDownload(){
        //mTaskState = TaskState.PREPARE;
        mDownloadedSize = 0;
        mTotalSize = 0;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadPrepare(this);
    }

    public void startDownload(){
        //mTaskState = TaskState.DOWNLOADING;

        for (IDownloadListener listener :mDownloadListenerList)
            listener.onDownloadBegin(this);
    }

    public void completeDownload(){
        //mTaskState = TaskState.COMPLETE;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadComplete(this);
    }

    public void cancelDownload(){
        //mTaskState = TaskState.CANCELED;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadCancel(this);
    }

    public void abortDownload(){
        //mTaskState = TaskState.CANCELED;
    }

    public final double getDownloadedSize() {
        return mDownloadedSize;
    }

    public void setDownloadedSize(double downloadedSize) {
        this.mDownloadedSize = downloadedSize;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onProgressChanged(downloadedSize, this.mTotalSize);
    }

    public final double getTotalSize() {
        return mTotalSize;
    }

    public void setTotalSize(double totalSize) {
        this.mTotalSize = totalSize;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onProgressChanged(this.mDownloadedSize, totalSize);
    }

    public void setProgress(double downloadedSize, double totalSize){
        this.mDownloadedSize = downloadedSize / 1000000;
        this.mTotalSize = totalSize / 1000000;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onProgressChanged(mDownloadedSize, mTotalSize);
    }

    public final URL getUrl(){
        return mUrl;
    }

    public void raiseError(TaskResult result){
        //mTaskState = TaskState.ERROR;

        for (IDownloadListener listener : mDownloadListenerList)
            listener.onDownloadError(result);
    }

    public void subscribe(IDownloadListener listener){
            mDownloadListenerList.add(listener);
    }

    public void unsubscribe(IDownloadListener listener){
            mDownloadListenerList.remove(listener);
    }

    public final String getFileName() {
        return mFileName + mFileExtension;
    }

    public final String getFilePath() {
        return mFilePath;
    }

    //public final boolean isCanceling(){
    //    return mTaskState == TaskState.CANCELING;
    //}

//    public final boolean isCanceled(){
//        return mTaskState == TaskState.CANCELED;
//    }

    public int getTaskId(){
        return mTaskId;
    }

    public void setTaskId(int taskId) {
        mTaskId = taskId;
    }

    @Override
    public int compareTo(DownloadInfo another){
        return getFileName().compareTo(another.getFileName());
    }

    //[\]\[|\\?*<":>+/']
}
