package com.timky.vkmusicsync.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.timky.vkmusicsync.AlbumMenuFragment;
import com.timky.vkmusicsync.R;
import com.timky.vkmusicsync.models.IAlbumSelectedListener;
import com.timky.vkmusicsync.models.VKAlbum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Smart adapter for album list
 * Created by timky on 25.03.14.
 */
public class VKAlbumListAdapter extends BaseAdapter {
    private List<VKAlbum> albumList = new ArrayList<VKAlbum>();
    private final Context mContext;
    private final AlbumMenuFragment mFragment;
    private VKAlbum selectedAlbum;

    public VKAlbumListAdapter(Context context, AlbumMenuFragment fragment){
        this.mContext = context;
        this.mFragment = fragment;
    }

    public void refresh(List<VKAlbum> albumList){
        this.albumList.clear();

        VKAlbum myMusic = new VKAlbum();                            // Menu item "My music" -> selects all music from all albums
        myMusic.id = ListViewItemKind.MyMusic;
        myMusic.title = mContext.getString(R.string.album_list_item_my_music);
        myMusic.setIsSelected(true);
        this.albumList.add(myMusic);

        VKAlbum playListHeader = new VKAlbum();                     // Menu header "Playlists", non-clickable
        playListHeader.id = ListViewItemKind.PlayListHeader;
        this.albumList.add(playListHeader);

        if (albumList.size() == 0){
            VKAlbum empty = new VKAlbum();
            empty.id = ListViewItemKind.EmptyList;
            this.albumList.add(empty);
        }
        else {
            Collections.sort(albumList);
            this.albumList.addAll(albumList);
        }

        selectedAlbum = myMusic;
        notifyDataSetChanged();
        mFragment.onAlbumSelected(selectedAlbum.id);
        //com.timky.vkmusicsync.helpers.ListLoader.setAlbumId(selectedAlbum.id);
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
        return albumList.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        VKAlbum album = albumList.get(position);

        // UPDATE: Now id is int, and I can use switch

        // I can't use switch in stupid Java... Switch statement data type can't be long.
        // Of course I can cast long to int, but in some cases long value != -1 && != 0, but int value does.

        switch (album.id) {
            case ListViewItemKind.PlayListHeader:
                return LayoutInflater.from(mContext).
                        inflate(R.layout.vkalbum_list_header, parent, false);

            case ListViewItemKind.EmptyList:
                return LayoutInflater.from(mContext).
                        inflate(R.layout.vkalbum_list_empty, parent, false);

            default:
                if (convertView == null)
                    convertView = LayoutInflater.from(mContext).
                            inflate(R.layout.vkalbum_list_item, parent, false);
        }

//        if (album.id == ListViewItemKind.PlayListHeader){
//            return LayoutInflater.from(mContext).
//                    inflate(R.layout.vkalbum_list_header, parent, false);
//        }
//
//        else if (album.id == ListViewItemKind.EmptyList){
//            return LayoutInflater.from(mContext).
//                    inflate(R.layout.vkalbum_list_empty, parent, false);
//
//        }
//        else {
//            convertView = LayoutInflater.from(mContext).
//                    inflate(R.layout.vkalbum_list_item, parent, false);
//        }

        if (convertView == null)
            return null;

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

                if (vkAlbum.id != selectedAlbum.id) {
                    vkAlbum.setIsSelected(true);
                    selectedAlbum.setIsSelected(false);
                    selectedAlbum = vkAlbum;
                }

                mFragment.onAlbumSelected(selectedAlbum.id);
                //com.timky.vkmusicsync.helpers.ListLoader.setAlbumId(vkAlbum.id);
                //com.timky.vkmusicsync.helpers.ListLoader.refresh();
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
        public static final int EmptyList = -2;
        public static final int PlayListHeader = -1;
        public static final int MyMusic = 0;
    }
}
