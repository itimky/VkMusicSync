package com.timky.vkmusicsync;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.timky.vkmusicsync.helpers.AudioListLoader;
import com.timky.vkmusicsync.helpers.Downloader;
import com.timky.vkmusicsync.helpers.VKAudioListAdapter;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;

import java.io.File;

public class MusicListActivity extends SlidingFragmentActivity implements IAudioListLoadListener{

    public static final int pageSize = 20;

    private PullToRefreshListView mPullToRefreshView;
    private VKAudioListAdapter mAudioListAdapter;
    private RelativeLayout mAudioLoadingLayout;
    private SlidingMenu mMenu;
    private String mFilePath = "Music/";      // Slash is required
    private int mForceSyncCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        removeTempFile();
        initialize();
        AudioListLoader.initialize(pageSize);
        AudioListLoader.setAudioSupport(mAudioListAdapter, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAudioListAdapter.cancelAllTasks();
        removeTempFile();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAudioListAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
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

            case android.R.id.home:         // Slide menu
                mMenu.toggle(true);
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

    @Override
    public void onBackPressed(){
        if(mMenu.isMenuShowing()){  // if SlidingMenu is opened
            mMenu.toggle(true);     // close SlidingMenu
            return;
        }

        super.onBackPressed();
    }

    private void initialize(){
        setContentView(R.layout.activity_music_list);
        mAudioListAdapter = new VKAudioListAdapter(this, mFilePath);
        mAudioLoadingLayout = (RelativeLayout)findViewById(R.id.audio_loading_layout);
        initializePullToRefresh();
        initializeSlidingMenu();
    }

    private void initializeSlidingMenu(){
        setBehindContentView(R.layout.fragment_album_list);
        FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
        Fragment fragment = new AlbumMenuFragment();
        t.replace(R.id.fragment_album_list, fragment);
        t.commit();

        mMenu = getSlidingMenu();
        mMenu.setShadowWidthRes(R.dimen.shadow_width);
        mMenu.setShadowDrawable(R.drawable.shadow);
        mMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mMenu.setFadeDegree(0.35f);
        mMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initializePullToRefresh(){
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

        mPullToRefreshView.setAdapter(mAudioListAdapter);
        mPullToRefreshView.getFooterLoadingView().refreshing();
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
    public void onListLoadStarted(boolean fullRefresh) {
        if (!fullRefresh) {
            mPullToRefreshView.getFooterLoadingView().setVisibility(View.VISIBLE);

            if (mForceSyncCount == 0) {
                ListView listView = mPullToRefreshView.getRefreshableView();
                listView.setSelection(listView.getCount() - 1);
            }
        }
        else if (!mPullToRefreshView.isRefreshing())
            mAudioLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAlbumSelected() {
        AudioListLoader.refresh();

        if (mMenu.isMenuShowing())
            mMenu.toggle(true);
    }

    @Override
    public void onListLoadCanceled() {
        finishLoading();
    }

    @Override
    public void onListLoadFinished(TaskResult result) {
        finishLoading();

        if (result.errorMessage != null)
            Toast.makeText(getApplicationContext(), result.errorMessage,
                    Toast.LENGTH_SHORT).show();

        else if (mForceSyncCount != 0) {
            mAudioListAdapter.forceSync(mForceSyncCount);
            mForceSyncCount = 0;
        }
    }

    private void finishLoading(){
        mPullToRefreshView.onRefreshComplete();
        mPullToRefreshView.getFooterLoadingView().setVisibility(View.GONE);
        mAudioLoadingLayout.setVisibility(View.GONE);
    }

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
