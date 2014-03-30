package com.timky.vkmusicsync.helpers;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;

import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.Downloadable;
import com.timky.vkmusicsync.models.IDownloadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.VKAudioInfo;
import com.timky.vkmusicsync.models.ViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Smart adapter for audio list amd smart ViewHolder. They are subscribe to AudioInfos' events
 * to notify about downloading states.
 * Created by timky on 3/8/14.
 */
public class VKAudioListAdapter extends BaseAdapter implements IDownloadListener {
    private List<VKAudioInfo> mAudioInfoList = new ArrayList<VKAudioInfo>();
    private final Context mContext;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    private int mTotal;
    private int mProgress;

    public String filePath;

    public VKAudioListAdapter(Context context, String filePath){
        this.filePath = filePath;

        mContext = context;
        mBuilder = new NotificationCompat.Builder(context);
        mNotifyManager = (NotificationManager) context.
                getSystemService(Context.NOTIFICATION_SERVICE);

        mTotal = 0;
        mProgress = 0;
    }

    public void refresh(List<VKAudioInfo> audioInfoList){
        AudioDownloader.checkIsDownloaded(audioInfoList, filePath);
        mAudioInfoList = audioInfoList;
        notifyDataSetChanged();
    }

    public void addAll(List<VKAudioInfo> audioInfoList){
        AudioDownloader.checkIsDownloaded(audioInfoList, filePath);
        mAudioInfoList.addAll(audioInfoList);
        notifyDataSetChanged();
    }

    public boolean anyTask(){
        for (Downloadable downloadable : mAudioInfoList)
            if (downloadable.task != null)
                return true;

        return false;
    }

    public void forceSync(int count){
        for (int i = 0; i < count; i++){
            VKAudioInfo audioInfo = mAudioInfoList.get(i);
            if (audioInfo.isDownloaded())
                continue;

            if (audioInfo.task != null)
                audioInfo.task.cancel(false);

            audioInfo.subscribe(this);
            AudioDownloader.createTask(audioInfo, filePath);
            audioInfo.task.execute();
        }
    }

    public void cancelAllTasks(){
        for (VKAudioInfo audioInfo : mAudioInfoList)
            if (audioInfo.task != null)
                audioInfo.task.cancel(false);

        mProgress = 0;
        mTotal = 0;
    }

    @Override
    public int getCount() {
        return mAudioInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return mAudioInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return mAudioInfoList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VKAudioInfo audioInfo = mAudioInfoList.get(position);

        if (convertView == null)
            convertView = LayoutInflater.from(mContext)
                    .inflate(R.layout.vkinfo_list_item, parent, false);

        ViewHolder viewHolder = getViewHolder(convertView);
        viewHolder.setAudioInfo(audioInfo);

        setOnItemClickListener(convertView);

        return convertView;
    }

    private ViewHolder getViewHolder(View convertView){
        if (convertView.getTag() == null){
            TextView artist = (TextView) convertView.findViewById(R.id.artist);
            TextView title = (TextView) convertView.findViewById(R.id.title);
            ProgressBar barProgress = (ProgressBar) convertView.findViewById(R.id.bar_progress);
            TextView textProgress = (TextView) convertView.findViewById(R.id.text_progress);
            ImageView isDownloaded = (ImageView) convertView.findViewById(R.id.is_downloaded);

            ViewHolder viewHolder = new ViewHolder(
                    mContext, artist, title, isDownloaded, barProgress, textProgress);

            convertView.setTag(viewHolder);
            return viewHolder;
        }

        return (ViewHolder) convertView.getTag();
    }

    private void setOnItemClickListener(View convertView){
        ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        final VKAudioInfo audioInfo = viewHolder.getAudioInfo();
        //final DownloadInfo downloadInfo = audioInfo.getDownloadInfo();

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (audioInfo.isDownloaded() && !audioInfo.isDownloading()){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        AudioDownloader.createTask(audioInfo, filePath);
                                        audioInfo.subscribe(VKAudioListAdapter.this);
                                        audioInfo.task.execute();
                                    }
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.message_already_downloaded)
                            .setPositiveButton(R.string.dialog_yes, dialogClickListener)
                            .setNegativeButton(R.string.dialog_no, dialogClickListener).show();

                }
                else if (audioInfo.isDownloading()){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        audioInfo.task.cancel(false);
                                    }
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.message_cancel_download)
                            .setNegativeButton(R.string.dialog_no, dialogClickListener)
                            .setPositiveButton(R.string.dialog_yes, dialogClickListener).show();
                }
                else {
                    AudioDownloader.createTask(audioInfo, filePath);
                    audioInfo.subscribe(VKAudioListAdapter.this);
                    audioInfo.task.execute();
                }
            }
        });
    }

//    @Override
//    public void onDownloadingChanged(Downloadable audInfo) {
//        if (!audInfo.isDownloading()) {
//
//        }
//    }

    @Override
    public void onDownloadPrepare(Downloadable downloadable) {
        mTotal++;
    }

    @Override
    public void onDownloadBegin(Downloadable downloadable) {
        VKAudioInfo audioInfo = (VKAudioInfo)downloadable;
        //mBuilder.setProgress(100, incr, false);
        mBuilder.setContentTitle(audioInfo.getFileName())
                .setContentText(
                        mContext.getString(R.string.notification_progress_template,
                                audioInfo.getDownloadedSize(), audioInfo.getDownloadedSize(),
                                mProgress, mTotal)
                )
                .setSmallIcon(R.drawable.ic_vkmusicsync_content)
                .setProgress(0, 0, true);
        // Displays the progress bar for the first time.
        mNotifyManager.notify(0, mBuilder.build());
        //barProgress.setProgress(0);
        //barProgress.setVisibility(visibility);
        //barProgress.setIndeterminate(isDownloading);

        //textProgress.setVisibility(visibility);
        //textProgress.setText("");

    }

    @Override
    public void onDownloadComplete(Downloadable downloadable) {
        VKAudioInfo audioInfo = (VKAudioInfo)downloadable;
        audioInfo.unsubscribe(this);
        mProgress++;

        if (mProgress == mTotal) {
            clearBuilder();

            if (mTotal == 1) {
                mBuilder.setContentTitle(audioInfo.getFileName())
                        .setContentText(
                                mContext.getString(R.string.notification_download_complete));
            }
            else
                mBuilder.setContentTitle(
                        mContext.getString(R.string.notification_download_all_complete_template,
                                mProgress, mTotal)
                );

            mNotifyManager.notify(0, mBuilder.build());
            mProgress = 0;
            mTotal = 0;
        }
    }

    @Override
    public void onDownloadCancel(Downloadable downloadable) {

        if (mTotal == 1){
            clearBuilder();
            mBuilder.setContentTitle(mContext.getString(R.string.notification_download_canceled));
            mNotifyManager.notify(0, mBuilder.build());
        }

        mTotal--;

    }

    @Override
    public void onProgressChanged(double downloadedSize, double totalSize) {
        mBuilder.setProgress(100, (int)(downloadedSize  * 100 / totalSize), false)
        .setContentText(
                mContext.getString(R.string.notification_progress_template,
                        downloadedSize, totalSize, mProgress, mTotal));
        mNotifyManager.notify(0, mBuilder.build());

    }

    @Override
    public void onDownloadedChanged(boolean isDownloaded, String fullFileName) {

    }

    @Override
    public void onDownloadError(TaskResult result) {
        cancelAllTasks();
        clearBuilder();
        mBuilder.setContentTitle(mContext.getString(R.string.notification_download_error));
        mNotifyManager.notify(0, mBuilder.build());
    }

    private void clearBuilder(){
        mBuilder.setContentTitle(null)
                .setContentText(null)
                .setProgress(0, 0, false);
    }
}
