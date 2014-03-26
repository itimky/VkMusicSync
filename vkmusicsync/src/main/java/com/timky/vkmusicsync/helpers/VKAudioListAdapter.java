package com.timky.vkmusicsync.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.Downloadable;
import com.timky.vkmusicsync.models.VKAudioInfo;
import com.timky.vkmusicsync.models.ViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 3/8/14.
 */
public class VKAudioListAdapter extends BaseAdapter {
    protected List<VKAudioInfo> audioInfoList = new ArrayList<VKAudioInfo>();
    protected final Context context;
    public String filePath;

    public VKAudioListAdapter(Context context, String filePath){
        this.context = context;
        this.filePath = filePath;
    }

    public void refresh(List<VKAudioInfo> audioInfoList){
        AudioDownloader.checkIsDownloaded(audioInfoList, filePath);
        this.audioInfoList = audioInfoList;
        notifyDataSetChanged();
    }

    public void addAll(List<VKAudioInfo> audioInfoList){
        AudioDownloader.checkIsDownloaded(audioInfoList, filePath);
        this.audioInfoList.addAll(audioInfoList);
        notifyDataSetChanged();
    }

    public boolean anyTask(){
        for (Downloadable downloadable : audioInfoList)
            if (downloadable.task != null)
                return true;

        return false;
    }

    public void forceSync(int count){
        for (int i = 0; i < count; i++){
            VKAudioInfo audioInfo = audioInfoList.get(i);
            if (audioInfo.isDownloaded())
                continue;

            if (audioInfo.task != null)
                audioInfo.task.cancel(false);

            AudioDownloader.createTask(audioInfo, filePath);
            audioInfo.task.execute();
        }
    }

    public void cancelAllTasks(){
        for (VKAudioInfo audioInfo : audioInfoList)
            if (audioInfo.task != null)
                audioInfo.task.cancel(false);
    }

    @Override
    public int getCount() {
        return audioInfoList.size();
    }

    @Override
    public Object getItem(int position) {
        return audioInfoList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return audioInfoList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        VKAudioInfo audioInfo = audioInfoList.get(position);

        if (convertView == null)
            convertView = LayoutInflater.from(context)
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
            CheckBox isDownloaded = (CheckBox) convertView.findViewById(R.id.is_downloaded);

            ViewHolder viewHolder = new ViewHolder(
                    context, artist, title, isDownloaded, barProgress, textProgress);

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
                if (audioInfo.task == null)
                    AudioDownloader.createTask(audioInfo, filePath);

                if (audioInfo.isDownloaded() && !audioInfo.isDownloading()){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE)
                                        audioInfo.task.execute();
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.already_downloaded)
                            .setPositiveButton(R.string.yes, dialogClickListener)
                            .setNegativeButton(R.string.no, dialogClickListener).show();

                }
                else if (audioInfo.isDownloading()){
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == DialogInterface.BUTTON_POSITIVE)
                                        audioInfo.task.cancel(false);
                                }
                            };

                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder.setMessage(R.string.cancel_downloading)
                            .setNegativeButton(R.string.no, dialogClickListener)
                            .setPositiveButton(R.string.yes, dialogClickListener).show();
                }
                else
                    audioInfo.task.execute();
            }
        });
    }
}
