package com.timky.vkmusicsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import com.timky.vkmusicsync.helpers.AudioListLoader;
import com.timky.vkmusicsync.helpers.Downloader;
import com.timky.vkmusicsync.helpers.VKAlbumListAdapter;
import com.timky.vkmusicsync.helpers.VKAudioListAdapter;
import com.timky.vkmusicsync.models.AlbumSelectedListener;
import com.timky.vkmusicsync.models.ListLoadEventListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.models.VKAlbum;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;

import java.io.File;

public class MusicListActivity extends ActionBarActivity implements ListLoadEventListener, AlbumSelectedListener {
    public static final int pageSize = 20;

    private PullToRefreshListView mPullToRefreshView;
    private VKAudioListAdapter mAudioAdapter;
    private VKAlbumListAdapter mAlbumAdapter;
    private RelativeLayout mAudioLoadingLayout;
    private RelativeLayout mAlbumLoadingLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private FrameLayout mFrame;
    private ListView mDrawerList;
    private String mFilePath = "Music/";      // Slash is required
    private int mForceSyncCount = 0;
    private float lastTranslate = 0.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_list);
        initialize();
        removeTempFile();
        mAudioLoadingLayout.setVisibility(View.VISIBLE);
        AudioListLoader.loadAlbums();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioAdapter.cancelAllTasks();
        removeTempFile();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAudioAdapter.notifyDataSetChanged();
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        int id = item.getItemId();

        switch (id){
            case R.id.action_logout:
                VKAccessToken token = VKSdk.getAccessToken();

                if (token != null) {
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor edit = prefs.edit();
                    edit.remove(LoginActivity.tokenKey);
                    edit.commit();
                    VKSdk.setAccessToken(null, true);

                    startLoginActivity();
                }
                return true;

            case R.id.sync_first_100:
                mForceSyncCount = 100;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                AudioListLoader.refresh(mForceSyncCount);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.music_list, menu);
        return true;
    }

    private void initialize(){
        setContentView(R.layout.activity_music_list);

        DrawerLayout drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(getResources().getColor(android.R.color.transparent));
        mAudioAdapter = new VKAudioListAdapter(this, mFilePath);
        mAlbumAdapter = new VKAlbumListAdapter(this, drawerLayout);
        mAlbumAdapter.onAlbumSelectedListener = this;
        AudioListLoader.init(this, mAlbumAdapter, mAudioAdapter, pageSize);

        mPullToRefreshView = (PullToRefreshListView) findViewById(R.id.pull_to_refresh_listview);
        mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                AudioListLoader.refresh();
            }
        });

        mPullToRefreshView.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
            @Override
            public void onLastItemVisible() {
                    AudioListLoader.loadMore();
                }
        });

        mPullToRefreshView.setAdapter(mAudioAdapter);
        mPullToRefreshView.getFooterLoadingView().refreshing();

        mAlbumLoadingLayout = (RelativeLayout)findViewById(R.id.album_loading_layout);
        mAudioLoadingLayout = (RelativeLayout)findViewById(R.id.audio_loading_layout);

        mFrame = (FrameLayout) findViewById(R.id.main_frame);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, R.drawable.ic_drawer, R.string.drawer_open, R.string.drawer_close)
        {
            //@SuppressLint("NewApi")
            public void onDrawerSlide(View drawerView, float slideOffset)
            {
                float moveFactor = (mDrawerList.getWidth() * slideOffset);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                {
                    mFrame.setTranslationX(moveFactor);
                }
                else
                {
                    TranslateAnimation anim = new TranslateAnimation(lastTranslate, moveFactor, 0.0f, 0.0f);
                    anim.setDuration(0);
                    anim.setFillAfter(true);
                    mFrame.startAnimation(anim);

                    lastTranslate = moveFactor;
                }
            }
        };

        mDrawerList.setAdapter(mAlbumAdapter);

//        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int position, long album_id) {
//                VKAlbum album = (VKAlbum)parent.getItemAtPosition(position);
//                if (AudioListLoader.albumId == album.album_id)
//                    return;
//
//                AudioListLoader.albumId = album.album_id;
//                AudioListLoader.refresh();
//            }
//        });

        drawerLayout.setDrawerListener(mDrawerToggle);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);
    }

    private void startLoginActivity() {
        Intent intent = new Intent(MusicListActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void removeTempFile(){
        File tempFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + mFilePath,
                Downloader.tempName);
        if (tempFile.exists())
            tempFile.delete();
    }

    @Override
    public void onAlbumLoadStarted() {
        //mAlbumLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAlbumLoadFinished(TaskResult result) {
        if (result.errorMessage != null)
            Toast.makeText(getApplicationContext(), result.errorMessage,
                    Toast.LENGTH_SHORT).show();
        else {
            mAlbumLoadingLayout.setVisibility(View.GONE);
            AudioListLoader.refresh();
        }

    }

    @Override
    public void onAudioLoadStarted(boolean fullRefresh) {
        if (!fullRefresh) {
            mPullToRefreshView.getFooterLoadingView().setVisibility(View.VISIBLE);

            if (mForceSyncCount == 0) {
                ListView listView = mPullToRefreshView.getRefreshableView();
                listView.setSelection(listView.getCount() - 1);
            }
        }
    }

    @Override
    public void onLoadCanceled() {
        finishLoading();
    }

    @Override
    public void onAudioLoadFinished(TaskResult result) {
        finishLoading();

        if (result.errorMessage != null)
            Toast.makeText(getApplicationContext(), result.errorMessage,
                    Toast.LENGTH_SHORT).show();

        else if (mForceSyncCount != 0) {
            mAudioAdapter.forceSync(mForceSyncCount);
            mForceSyncCount = 0;
        }
    }

    private void finishLoading(){
        mPullToRefreshView.onRefreshComplete();
        mPullToRefreshView.getFooterLoadingView().setVisibility(View.GONE);
        mAlbumLoadingLayout.setVisibility(View.GONE);
        mAudioLoadingLayout.setVisibility(View.GONE);
    }

    @Override
    public void onAlbumSelected(VKAlbum album) {
        if (AudioListLoader.albumId == album.album_id)
            return;

        mAudioLoadingLayout.setVisibility(View.VISIBLE);
        AudioListLoader.albumId = album.album_id;
        AudioListLoader.refresh();
    }

//    @Override
//    public void onAlbumSelected(VKAlbum album) {
//        if (AudioListLoader.albumId == album.album_id)
//            return;
//
//        AudioListLoader.albumId = album.album_id;
//        AudioListLoader.refresh();
//    }


//    /**
//     * A placeholder fragment containing a simple view.
//     */
//    public static class PlaceholderFragment extends Fragment {
//
//        public PlaceholderFragment() {
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                Bundle savedInstanceState) {
//            View rootView = inflater.inflate(R.layout.fragment_music_list, container, false);
//            return rootView;
//        }
//    }

}
