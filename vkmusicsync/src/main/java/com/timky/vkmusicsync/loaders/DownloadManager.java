package com.timky.vkmusicsync.loaders;

import android.os.AsyncTask;
import android.os.Environment;

import com.mpatric.mp3agic.NotSupportedException;
import com.timky.vkmusicsync.models.AsyncList;
import com.timky.vkmusicsync.models.DownloadInfo;
import com.timky.vkmusicsync.models.IIdObject;
import com.timky.vkmusicsync.models.TaskState;
import com.timky.vkmusicsync.models.ErrorCodes;
import com.timky.vkmusicsync.models.TaskResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Download manager for any task. Executes only 1 task at the same time.
 * Somehow in Android 2.3.3 AsyncTask.execute() == AsyncTask.executeOnExecutor(),
 * that's why this manager controls order of tasks
 * Created by timky on 03.04.14.
 */
public class DownloadManager {
    private String mPostfix;
    private int mTotal = 0;
    private int mProgress = 0;
    private int mCanceled = 0;
    private final AsyncList<DownloadTask> mTasks = new AsyncList<DownloadTask>();
    protected String mFilePath;
    private String mTempFileName;
    private int mNewTaskId = 0;
    public static final int mNoTaskId = -1;

    public DownloadManager(String filePath){
        mFilePath = filePath;
        mPostfix = "";
    }

    public final void setTempFileName(String tempFileName) {
        mTempFileName = tempFileName;
    }

    public final void setFilePath(String filePath){
        mFilePath = filePath;
    }

    public boolean isDownloading(){
        return mTasks.size() != 0;
    }

    public int getDownloadingCount(){
        return mTasks.size();
    }

    public void cancelAllTasks() {
        for (DownloadTask task : mTasks)
            task.cancel();
    }

    public void cancelTask(DownloadInfo downloadInfo){
        int taskId = downloadInfo.getTaskId();
        DownloadTask task = mTasks.findById(taskId);

        if (task != null)
            task.cancel();
    }

    public DownloadInfo getCurrentTask() {
        return mTasks.size() != 0 ? mTasks.get(0).mDownloadInfo : null;
    }

    public int getProgress(){
        return mProgress;
    }

    public int getTotal(){
        return mTotal - mCanceled;
    }

    public int getCanceled(){
        return mCanceled;
    }

    public boolean isAllComplete(){
        return mProgress == mTotal - mCanceled;
    }

    public void download(DownloadInfo downloadInfo) {
        if (isAllComplete()){
            mProgress = 0;
            mTotal = 0;
            mCanceled = 0;
        }
        DownloadTask task = new DownloadTask(downloadInfo);
        mTasks.add(task);
        mTotal++;
        task.prepare();

        if (mTotal == 1) {
            task.execute();
        }
    }

    private void next() {
        if (mTasks.size() != 0) {
            mTasks.get(0).execute();
        }
    }

    protected class DownloadTask extends AsyncTask<Void, Integer, TaskResult>
                                            implements Comparable<DownloadInfo>, IIdObject {
        protected final DownloadInfo mDownloadInfo;
        private TaskState mState = TaskState.PREPARE;
        public final int taskId;

        public DownloadTask(DownloadInfo downloadInfo){
            mDownloadInfo = downloadInfo;
            taskId = mNewTaskId++;
            mDownloadInfo.setTaskId(taskId);
        }

        public void cancel() {
            if (mState == TaskState.PREPARE) {
                checkIsDownloaded(mDownloadInfo);
                mDownloadInfo.cancelDownload();
                mTasks.remove(this);
            }
            else if (mState == TaskState.DOWNLOADING)
                mDownloadInfo.abortDownload();

            mDownloadInfo.setTaskId(mNoTaskId);
            mState = TaskState.CANCELED;
            mCanceled++;
        }

        public void prepare() {
            mState = TaskState.PREPARE;
            mDownloadInfo.prepareDownload();
        }

        public TaskState getState() {
            return mState;
        }

        protected void setState(TaskState state) {
            mState = state;
        }

        @Override
        protected TaskResult doInBackground(Void... params) {
            TaskResult result =  new TaskResult();

            if (mDownloadInfo.getTaskId() != taskId) {
                return result;
            }

            mDownloadInfo.startDownload();
            mState = TaskState.DOWNLOADING;

            String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/" + mFilePath;
            String fileName = mTempFileName != null ? mTempFileName : mDownloadInfo.getFileName()
                    + (mPostfix != null ? mPostfix : "");

            try {

                //create the new connection
                HttpURLConnection urlConnection = (HttpURLConnection) mDownloadInfo.getUrl().openConnection();

                //set up some things on the connection
                urlConnection.setRequestMethod("GET");
                //urlConnection.setDoOutput(true);

                //and connect!
                urlConnection.connect();

                //set the path where we want to save the file
                File directory = new File(directoryPath);

                if (!directory.isDirectory() || !directory.exists()) {
                    directory.mkdir();
                }
                //create a new file, specifying the path, and the filename
                //which we want to save the file as.
                File file = new File(directory, fileName);

                //this will be used to write the downloaded data into the file we created
                FileOutputStream fileOutput = new FileOutputStream(file);

                //this will be used in reading the data from the internet
                InputStream inputStream = urlConnection.getInputStream();

                //this is the total size of the file
                final int totalSize = urlConnection.getContentLength();
                //variable to store total downloaded bytes
                int downloadedSize = 0;

                //create a buffer...
                byte[] buffer = new byte[64 * 1024];
                int bufferLength; //used to store a temporary size of the buffer

                //now, read through the input buffer and write the contents to the file
                while ((bufferLength = inputStream.read(buffer)) > 0) {

                    if (mDownloadInfo.getTaskId() != taskId){
                        fileOutput.flush();
                        fileOutput.close();

                        file.deleteOnExit();
                        file.delete();

                        //result.taskState = mDownloadInfo.getState();
                        return result;
                    }
                    //add the data in the buffer to the file in the file output stream (the file on the sd card
                    fileOutput.write(buffer, 0, bufferLength);
                    //add up the size so we know how much is downloaded
                    downloadedSize += bufferLength;
                    publishProgress(downloadedSize, totalSize);
                }
                //close the output stream when done
                fileOutput.flush();
                fileOutput.close();
                result.fullFileName = directoryPath + fileName;
                mState = TaskState.COMPLETE;

            } catch (UnknownHostException e) {
                e.printStackTrace();
                result.errorMessage = "No internet access...";
                result.errorCode = ErrorCodes.connectionRefused;
                mState = TaskState.ERROR;
            } catch (IOException e) {
                e.printStackTrace();
                result.errorMessage = "IOException...";
                mState = TaskState.ERROR;
            }

            completeExecuteInBackground(this, result);

            return result;
        }

        @Override
        protected void onProgressUpdate(Integer... progress){
            mDownloadInfo.setProgress(progress[0], progress[1]);
        }

        @Override
        protected void onPostExecute(TaskResult result){

            switch (mState){
                case COMPLETE:
                    mProgress++;
                    mDownloadInfo.setTaskId(mNoTaskId);
                    mDownloadInfo.completeDownload();
                    checkIsDownloaded(mDownloadInfo);
                    break;

                case ERROR:
                    mProgress++;
                    mDownloadInfo.setTaskId(mNoTaskId);
                    mDownloadInfo.raiseError(result);
                    checkIsDownloaded(mDownloadInfo);
                    break;

                case CANCELED:
                    mDownloadInfo.cancelDownload();
                    checkIsDownloaded(mDownloadInfo);
                    break;
            }

            mTasks.remove(this);
            next();
        }

        @Override
        public int compareTo(DownloadInfo another) {
            return mDownloadInfo.compareTo(another);
        }

        @Override
        public int getId() {
            return taskId;
        }

        @Override
        public void setId(int id) throws NotSupportedException {
            throw new NotSupportedException();
        }
    }

    /**
     * Implement here background work after download
     * @param task
     * @param result
     * @return
     */
    protected TaskResult completeExecuteInBackground(DownloadTask task, TaskResult result){
        return result;
    }

    public void delete(DownloadInfo downloadInfo) {
        File file = new File(downloadInfo.getFileFullName(mFilePath));
        file.delete();

        downloadInfo.setDownloaded(false, mFilePath);
    }

    public void checkIsDownloaded(DownloadInfo downloadInfo){
        File file = new File(downloadInfo.getFileFullName(mFilePath));
        if(file.exists())
            downloadInfo.setDownloaded(true, mFilePath);
    }

    public void checkIsDownloaded(List<? extends DownloadInfo> downloadableList){
        for (DownloadInfo downloadInfo : downloadableList)
            checkIsDownloaded(downloadInfo);
    }

    public static void checkIsDownloaded(DownloadInfo downloadInfo, String filePath){
        File file = new File(downloadInfo.getFileFullName(filePath));
        if(file.exists())
            downloadInfo.setDownloaded(true, filePath);
    }

    public static void checkIsDownloaded(List<? extends DownloadInfo> downloadableList, String filePath){
        for (DownloadInfo downloadInfo : downloadableList)
            checkIsDownloaded(downloadInfo, filePath);
    }
}
