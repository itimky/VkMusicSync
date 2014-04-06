package com.timky.vkmusicsync.helpers;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.Tracker;
import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.DownloadInfo;
import com.timky.vkmusicsync.models.TaskState;
import com.timky.vkmusicsync.models.Events;
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
    private final VKAudioDownloadManager mAudioDownloadManager;
    private final Context mContext;
    private NotificationCompat.Builder mBuilder;
    private NotificationManager mNotifyManager;
    //private int mTotal;
    //private int mProgress;
    private double mProgressSizeSinceUpdate = 0;
    private double mLastProgressSize = 0;

    public VKAudioListAdapter(Context context, VKAudioDownloadManager audioDownloadManager){
        mContext = context;
        mAudioDownloadManager = audioDownloadManager;
        mBuilder = new NotificationCompat.Builder(context);
        mNotifyManager = (NotificationManager) context.
                getSystemService(Context.NOTIFICATION_SERVICE);

        //mTotal = 0;
        //mProgress = 0;
    }

    public void refresh(List<VKAudioInfo> audioInfoList){
        mAudioDownloadManager.checkIsDownloaded(audioInfoList);
        mAudioInfoList = audioInfoList;
        notifyDataSetChanged();
    }

    public void addAll(List<VKAudioInfo> audioInfoList){
        mAudioDownloadManager.checkIsDownloaded(audioInfoList);
        mAudioInfoList.addAll(audioInfoList);
        notifyDataSetChanged();
    }

    public boolean anyTask(){
        return mAudioDownloadManager.isDownloading();
    }

    public void forceSync(int count){
        for (int i = 0; i < count; i++){
            VKAudioInfo audioInfo = mAudioInfoList.get(i);
            if (audioInfo.isDownloaded())
                continue;

            if (audioInfo.getTaskId() != DownloadManager.mNoTaskId)
                continue;

            audioInfo.subscribe(this);
            mAudioDownloadManager.download(audioInfo);
        }

        invalidateMenu();
    }
//
//    public void cancelAllTasks(){
//        for (VKAudioInfo audioInfo : mAudioInfoList)
//            audioInfo.cancelDownload();
//
//        //mProgress = 0;
//        //mTotal = 0;
//    }

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

                if (audioInfo.isDownloaded() && audioInfo.getTaskId() == DownloadManager.mNoTaskId){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        audioInfo.subscribe(VKAudioListAdapter.this);
                                        mAudioDownloadManager.download(audioInfo);
                                        invalidateMenu();
                                    }
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.message_already_downloaded)
                            .setPositiveButton(R.string.dialog_yes, dialogClickListener)
                            .setNegativeButton(R.string.dialog_no, dialogClickListener).show();

                }
                else if (audioInfo.getTaskId() != DownloadManager.mNoTaskId){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE) {
                                        mAudioDownloadManager.cancelTask(audioInfo);
                                        invalidateMenu();
                                    }
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setMessage(R.string.message_cancel_download)
                            .setNegativeButton(R.string.dialog_no, dialogClickListener)
                            .setPositiveButton(R.string.dialog_yes, dialogClickListener).show();
                }
                else {
                    audioInfo.subscribe(VKAudioListAdapter.this);
                    mAudioDownloadManager.download(audioInfo);
                    invalidateMenu();
                }
            }
        });
    }

    @Override
    public void onDownloadPrepare(DownloadInfo downloadInfo) {
        // Updating total downloads
        DownloadInfo current = mAudioDownloadManager.getCurrentTask();
        mBuilder.setContentText(mContext.getString(R.string.notification_progress_template,
                                current.getDownloadedSize(), current.getTotalSize(),
                                mAudioDownloadManager.getProgress(),
                                mAudioDownloadManager.getTotal())
                );
        mNotifyManager.notify(0, mBuilder.build());
    }

    @Override
    public void onDownloadBegin(DownloadInfo downloadInfo) {
        notifyAction(Events.DOWNLOAD_BEGIN);
        clearBuilder();
        VKAudioInfo audioInfo = (VKAudioInfo) downloadInfo;
        mBuilder.setContentTitle(audioInfo.getFileName())
                .setContentText(
                        mContext.getString(R.string.notification_progress_template,
                                audioInfo.getDownloadedSize(), audioInfo.getTotalSize(),
                                mAudioDownloadManager.getProgress(),
                                mAudioDownloadManager.getTotal())
                )
                .setSmallIcon(R.drawable.ic_vkmusicsync_logo)
                .setProgress(0, 0, true);
        // Displays the progress bar for the first time.
        mNotifyManager.notify(0, mBuilder.build());
    }

    @Override
    public void onDownloadComplete(DownloadInfo downloadInfo) {
        notifyAction(Events.DOWNLOAD_COMPLETE);
        VKAudioInfo audioInfo = (VKAudioInfo) downloadInfo;
        audioInfo.unsubscribe(this);
        clearBuilder();

        if (mAudioDownloadManager.isAllComplete()) {

            if (mAudioDownloadManager.getTotal() == 1) {
                mBuilder.setContentTitle(audioInfo.getFileName())
                        .setContentText(
                                mContext.getString(R.string.notification_download_complete));
            }
            else
                mBuilder.setContentTitle(
                        mContext.getString(R.string.notification_download_all_complete_template,
                                mAudioDownloadManager.getProgress(),
                                mAudioDownloadManager.getTotal())
                );

            mNotifyManager.notify(0, mBuilder.build());
        }
    }

    @Override
    public void onDownloadCancel(DownloadInfo downloadInfo) {

        // Displaying "Canceled" message
        if (mAudioDownloadManager.getDownloadingCount() == 1){
            clearBuilder();
            if (mAudioDownloadManager.getCanceled() == 1){
                mBuilder.setContentTitle(mContext.getString(R.string.notification_download_canceled));
                mNotifyManager.notify(0, mBuilder.build());
            }
            else {
                mBuilder.setContentTitle(mContext.getString(R.string.notification_download_all_canceled));
                mNotifyManager.notify(0, mBuilder.build());
            }
        }
        else {
            // Updating total downloads
            DownloadInfo current = mAudioDownloadManager.getCurrentTask();
            mBuilder.setContentText(mContext.getString(R.string.notification_progress_template,
                            current.getDownloadedSize(), current.getTotalSize(),
                            mAudioDownloadManager.getProgress(),
                            mAudioDownloadManager.getTotal())
            );
            mNotifyManager.notify(0, mBuilder.build());
        }
    }

    /**
     * Notifies only every 100 KB
     * @param downloadedSize
     * @param totalSize
     */
    @Override
    public void onProgressChanged(double downloadedSize, double totalSize) {
        mProgressSizeSinceUpdate += downloadedSize - mLastProgressSize;
        mLastProgressSize = downloadedSize;

        // Trick to optimize performance - notify only every 100KB
        if (mProgressSizeSinceUpdate >= 0.1) {
            mBuilder.setProgress(100, (int) (downloadedSize * 100 / totalSize), false)
                    .setContentText(
                            mContext.getString(R.string.notification_progress_template,
                                    downloadedSize, totalSize,
                                    mAudioDownloadManager.getProgress(),
                                    mAudioDownloadManager.getTotal())
                    );
            mNotifyManager.notify(0, mBuilder.build());
            mProgressSizeSinceUpdate = 0;
        }
    }

    @Override
    public void onDownloadedChanged(boolean isDownloaded, String fullFileName) {

    }

    @Override
    public void onDownloadError(TaskResult result) {
        mAudioDownloadManager.cancelAllTasks();
        clearBuilder();
        mBuilder.setContentTitle(mContext.getString(R.string.notification_download_error));
        mNotifyManager.notify(0, mBuilder.build());
    }

    private void clearBuilder(){
        mBuilder.setContentTitle(null)
                .setContentText(null)
                .setProgress(0, 0, false);
        mLastProgressSize = 0;
        mProgressSizeSinceUpdate = 0;
    }

    private void invalidateMenu(){
        ((FragmentActivity)mContext).supportInvalidateOptionsMenu();
    }

    private void notifyAction(Events event){
        Tracker v3EasyTracker = EasyTracker.getInstance(mContext);

        switch (event){
            case DOWNLOAD_BEGIN:
                v3EasyTracker.send(MapBuilder
                                .createEvent(
                                        "DownloadState",
                                        "DownloadBegin",
                                        "onDownloadBegin", null)
                                .build()
                );
                break;

            case DOWNLOAD_COMPLETE:
                v3EasyTracker.send(MapBuilder
                                .createEvent(
                                        "DownloadState",
                                        "DownloadComplete",
                                        "onDownloadComplete", null)
                                .build()
                );
                break;
        }
    }
}
