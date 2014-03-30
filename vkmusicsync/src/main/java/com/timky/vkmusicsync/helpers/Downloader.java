package com.timky.vkmusicsync.helpers;

import android.os.AsyncTask;
import android.os.Environment;

import com.timky.vkmusicsync.models.ErrorCodes;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.Downloadable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.List;

/**
 * Created by timky on 21.03.14.
 */
public class Downloader extends AsyncTask<Object, Integer, TaskResult> {
    public Downloader(Downloadable downloadable, String filePath){
        mDownloadable = downloadable;
        mFilePath = filePath;
        mUseTempName = true;
        mPostfix = "";
    }

    protected final Downloadable mDownloadable;
    protected String mFilePath;
    public static final String tempName = "sync.tmp";
    private boolean mUseTempName;
    private String mPostfix;

    public void setUseTempName(boolean useTempName){
        mUseTempName = useTempName;
    }

    public void setmPostfix(String postfix){
        mPostfix = postfix;
    }

    @Override
    protected TaskResult doInBackground(Object... params) {
        TaskResult result =  new TaskResult();

        if (isCancelled())
            return result;

        mDownloadable.startDownload();

        String directoryPath = Environment.getExternalStorageDirectory().getPath() + "/" + mFilePath;
        String fileName = mUseTempName ? "vk_sync.tmp" : mDownloadable.getFileName() + mPostfix;

        try {

            //create the new connection
            HttpURLConnection urlConnection = (HttpURLConnection) mDownloadable.getUrl().openConnection();

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
            int totalSize = urlConnection.getContentLength();
            //variable to store total downloaded bytes
            int downloadedSize = 0;

            //create a buffer...
            byte[] buffer = new byte[64 * 1024];
            int bufferLength; //used to store a temporary size of the buffer

            //now, read through the input buffer and write the contents to the file
            while ((bufferLength = inputStream.read(buffer)) > 0) {

                if (isCancelled()){
                    fileOutput.flush();
                    fileOutput.close();

                    file.deleteOnExit();
                    file.delete();

                    return result;
                }
                //add the data in the buffer to the file in the file output stream (the file on the sd card
                fileOutput.write(buffer, 0, bufferLength);
                //add up the size so we know how much is downloaded
                downloadedSize += bufferLength;
                //this is where you would do something to report the prgress, like this maybe
                publishProgress(downloadedSize, totalSize);

            }
            //close the output stream when done
            fileOutput.flush();
            fileOutput.close();
            result.fullFileName = directoryPath + fileName;

        } catch (UnknownHostException e) {
            e.printStackTrace();
            result.errorMessage = "No internet access...";
            result.errorCode = ErrorCodes.connectionRefused;
        } catch (IOException e) {
            e.printStackTrace();
            result.errorMessage = "IOException...";
        }

        return result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mDownloadable.prepareDownload();
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
        super.onProgressUpdate(progress);
        double downloaded = (double) progress[0] / 1000000;
        double total = (double) progress[1] / 1000000;
        mDownloadable.setProgress(downloaded, total);
    }

    @Override
    protected void onPostExecute(TaskResult result) {
        super.onPostExecute(result);

        // IsDownloading must be true to refresh sd-card media
        checkIsDownloaded(this.mDownloadable, mFilePath);

        if (result.errorMessage != null)
            mDownloadable.raiseError(result);
        else
            mDownloadable.completeDownload();

        mDownloadable.task = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        // Setting IsDownloading = false to aviod sd-card media refresh
        mDownloadable.cancelDownload();
        checkIsDownloaded(mDownloadable, mFilePath);
        mDownloadable.task = null;
    }

    public static void checkIsDownloaded(Downloadable downloadable, String filePath){
        File file = new File(downloadable.getFileFullName(filePath));
        if(file.exists())
            downloadable.setDownloaded(true, filePath);
    }

    public static void checkIsDownloaded(List<? extends Downloadable> downloadableList, String filePath){
        for (Downloadable downloadable : downloadableList)
            checkIsDownloaded(downloadable, filePath);
    }
}
