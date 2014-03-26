package com.timky.vkmusicsync.helpers;

import android.content.Context;
import android.support.v4.widget.DrawerLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.AlbumSelectedListener;
import com.timky.vkmusicsync.models.VKAlbum;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbumListAdapter extends BaseAdapter {
    public AlbumSelectedListener onAlbumSelectedListener;

    private List<VKAlbum> albumList = new ArrayList<VKAlbum>();
    private final Context context;
    private final DrawerLayout drawerLayout;

    public VKAlbumListAdapter(Context context, DrawerLayout drawerLayout){
        this.context = context;
        this.drawerLayout = drawerLayout;
    }

    public void refresh(List<VKAlbum> albumList){
        this.albumList.clear();

        VKAlbum header = new VKAlbum();
        header.album_id = -1;
        header.title = context.getString(R.string.playlists);
        this.albumList.add(header);

        VKAlbum allAudio = new VKAlbum();
        allAudio.album_id = 0;
        allAudio.title = context.getString(R.string.all_audio);
        this.albumList.add(allAudio);

        this.albumList.addAll(albumList);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return albumList.size();
    }

    @Override
    public Object getItem(int position) {
        return albumList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return albumList.get(position).album_id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final VKAlbum album = albumList.get(position);

        //if (convertView == null)
        if (album.album_id == -1)
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.list_group_header, parent, false);
        else
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.vkalbum_list_item, parent, false);

        if (album.album_id == -1) {
            TextView header = (TextView)convertView.findViewById(R.id.group_header);
            header.setText(album.title);
            return convertView;
        }

        ViewHolder viewHolder;
        if (convertView.getTag() == null){
            TextView title = (TextView)convertView.findViewById(R.id.album_title);
            viewHolder = new ViewHolder(title);
            convertView.setTag(viewHolder);
        }
        else
            viewHolder = (ViewHolder)convertView.getTag();

        viewHolder.title.setText(albumList.get(position).title);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onAlbumSelectedListener != null) {
                    onAlbumSelectedListener.onAlbumSelected(album);
                    drawerLayout.closeDrawers();
                }
            }
        });

        return convertView;
    }


    private class ViewHolder{
        public final TextView title;

        public ViewHolder(TextView title){
            this.title = title;
        }
    }
}
