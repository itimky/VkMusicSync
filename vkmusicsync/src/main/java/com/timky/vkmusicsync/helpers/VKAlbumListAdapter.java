package com.timky.vkmusicsync.helpers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.IAlbumSelectedListener;
import com.timky.vkmusicsync.models.VKAlbum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by timky on 25.03.14.
 */
public class VKAlbumListAdapter extends BaseAdapter {
    //public AlbumSelectedListener onAlbumSelectedListener;

    private List<VKAlbum> albumList = new ArrayList<VKAlbum>();
    private final Context context;
    private VKAlbum selectedAlbum;
    //private final DrawerLayout drawerLayout;

    public VKAlbumListAdapter(Context context){
        this.context = context;
    }

    public void refresh(List<VKAlbum> albumList){
        this.albumList.clear();

        VKAlbum myMusic = new VKAlbum();                            // Menu item "My music" -> selects all music from all albums
        myMusic.album_id = ListViewItemKind.MyMusic;
        myMusic.title = context.getString(R.string.album_list_item_my_music);
        myMusic.setIsSelected(true);
        this.albumList.add(myMusic);

        VKAlbum playListHeader = new VKAlbum();                     // Menu header "Playlists", non-clickable
        playListHeader.album_id = ListViewItemKind.PlayListHeader;
        this.albumList.add(playListHeader);

        if (albumList.size() == 0){
            VKAlbum empty = new VKAlbum();
            empty.album_id = ListViewItemKind.EmptyList;
            this.albumList.add(empty);
        }
        else {
            Collections.sort(albumList);
            this.albumList.addAll(albumList);
        }

        selectedAlbum = myMusic;
        notifyDataSetChanged();

        AudioListLoader.setmAlbumId(selectedAlbum.album_id);
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

        VKAlbum album = albumList.get(position);

        // I can't use switch in stupid Java... Switch statement data type can't be long.
        // Of course I can cast long to int, but in some cases long value != -1 && != 0, but int value does.

        if (album.album_id == ListViewItemKind.PlayListHeader){
            return LayoutInflater.from(context).
                    inflate(R.layout.vkalbum_list_header, parent, false);
        }

        else if (album.album_id == ListViewItemKind.EmptyList){
            return LayoutInflater.from(context).
                    inflate(R.layout.vkalbum_list_empty, parent, false);

        }
        else {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.vkalbum_list_item, parent, false);
        }

        ViewHolder viewHolder = (ViewHolder)convertView.getTag();
        if (viewHolder == null){
            TextView title = (TextView)convertView.findViewById(R.id.album_title);
            ImageView selected = (ImageView)convertView.findViewById(R.id.album_selected);
            viewHolder = new ViewHolder(title, selected);
            convertView.setTag(viewHolder);
        }

        viewHolder.setAlbum(album);

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ViewHolder holder =  (ViewHolder)v.getTag();

                VKAlbum vkAlbum = holder.getAlbum();

                if (vkAlbum.album_id != selectedAlbum.album_id) {
                    vkAlbum.setIsSelected(true);
                    selectedAlbum.setIsSelected(false);
                    selectedAlbum = vkAlbum;
                }

                AudioListLoader.setmAlbumId(vkAlbum.album_id);
                AudioListLoader.refresh();
            }
        });

        return convertView;
    }

    private class ViewHolder implements IAlbumSelectedListener{
        public final TextView title;
        public final ImageView selected;
        private VKAlbum mAlbum;

        public ViewHolder(TextView title, ImageView selected){
            this.title = title;
            this.selected = selected;
        }

        public VKAlbum getAlbum() {
            return mAlbum;
        }

        public void setAlbum(VKAlbum album){
            if (mAlbum != null)
                mAlbum.listener = null;

            mAlbum = album;
            mAlbum.listener = this;

            int visibility = album.isSelected() ? View.VISIBLE : View.GONE;
            title.setText(album.title);
            selected.setVisibility(visibility);
        }

        @Override
        public void onAlbumSelected(boolean isSelected) {
            int visibility = isSelected ? View.VISIBLE : View.GONE;
            selected.setVisibility(visibility);
        }
    }

    private class ListViewItemKind {
        public static final long EmptyList = -2;
        public static final long PlayListHeader = -1;
        public static final long MyMusic = 0;
    }
}
