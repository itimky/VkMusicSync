package com.timky.vkmusicsync;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;

import android.os.Bundle;
import android.os.Environment;

import android.support.v4.app.FragmentTransaction;

import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.google.analytics.tracking.android.EasyTracker;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingFragmentActivity;
import com.timky.vkmusicsync.loaders.VKAudioDownloadManager;
import com.timky.vkmusicsync.adapters.VKAudioListAdapter;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.timky.vkmusicsync.loaders.AudioListLoader;
import com.vk.sdk.VKUIHelper;

import java.io.File;

public class MusicListActivity extends SlidingFragmentActivity implements IAudioListLoadListener, android.support.v7.widget.SearchView.OnQueryTextListener{

    public static final int pageSize = 20;
    private int mOffset = 0;

    private PullToRefreshListView mPullToRefreshView;
    private VKAudioListAdapter mAudioListAdapter;
    private RelativeLayout mAudioLoadingLayout;
    private SlidingMenu mSlidingMenu;
    private Menu mMenu;
    private String mFilePath = "Music/";      // Slash is required
    private String mTempFileName = "sync.tmp";
    private int mForceSyncCount = -1;
    private String mSearchQuery = null;
    private VKAudioDownloadManager mAudioDownloadManager = new VKAudioDownloadManager(mFilePath, mTempFileName);
    private AudioListLoader  mAudioListLoader;
    private AudioListLoader getAudioListLoader() {
        return new AudioListLoader(mAudioListAdapter, this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        removeTempFile();
        initialize();
    }

    @Override
    protected void onResume(){
        super.onResume();
        VKUIHelper.onResume(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);
    }

    @Override
    protected void onStop() {
        EasyTracker.getInstance(this).activityStop(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VKUIHelper.onDestroy(this);
        mAudioDownloadManager.cancelAllTasks();
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
                LoginActivity.reLogin(this);
                return true;

            case android.R.id.home:         // Slide menu
                mSlidingMenu.toggle(true);
                return true;

            case R.id.search:
                onSearchRequested();
                return true;

            case R.id.sync_first_10:
                //mForceSyncCount = 10;
                mOffset = 10;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                getAudioListLoader().execute(10, 0);
                return true;

            case R.id.sync_first_50:
                //mForceSyncCount = 50;
                mOffset = 50;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                getAudioListLoader().execute(50, 0);
                return true;

            case R.id.sync_first_100:
                //mForceSyncCount = 100;
                mOffset = 100;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                getAudioListLoader().execute(100, 0);
                return true;

            case R.id.sync_first_n:
                final EditText editText = new EditText(this);
                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                editText.setText("25");

                DialogInterface.OnClickListener builderNClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    String countStr = editText.getText().toString();

                                if (countStr.compareTo("") == 0)
                                    return;

                                int count = Integer.parseInt(countStr);
                                mOffset = count;
                                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                                getAudioListLoader().execute(count, 0);
                                }
                            }
                        };

                AlertDialog.Builder builderN = new AlertDialog.Builder(this);
                builderN.setView(editText);
                builderN.setMessage(R.string.message_sync_first_n)
                        .setPositiveButton(R.string.dialog_ok, builderNClickListener)
                        .setNegativeButton(R.string.dialog_cancel, builderNClickListener)
                        .show();

                return true;

            case R.id.sync_cancel:
                DialogInterface.OnClickListener builderCancelClickListener =
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                if (which == DialogInterface.BUTTON_POSITIVE) {
                                    mAudioDownloadManager.cancelAllTasks();
                                }
                            }
                        };

                AlertDialog.Builder builderCancel = new AlertDialog.Builder(this);
                builderCancel.setMessage(R.string.message_cancel_all_downloads)
                        .setPositiveButton(R.string.dialog_yes, builderCancelClickListener)
                        .setNegativeButton(R.string.dialog_no, builderCancelClickListener)
                        .show();

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.action_menu, menu);
        mMenu = menu;

        MenuItem searchItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.setOnQueryTextListener(this);
        }

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        MenuItem sync10 = menu.findItem(R.id.sync_first_10);
        MenuItem sync50 = menu.findItem(R.id.sync_first_50);
        MenuItem sync100 = menu.findItem(R.id.sync_first_100);
        MenuItem syncN = menu.findItem(R.id.sync_first_n);
        MenuItem cancel = menu.findItem(R.id.sync_cancel);

        boolean downloading = mAudioDownloadManager.isDownloading();

        // Ugly, but needed
        if (sync10 != null)
            sync10.setVisible(!downloading);

        if (sync50 != null)
            sync50.setVisible(!downloading);

        if (sync100 != null)
            sync100.setVisible(!downloading);

        if (syncN != null)
            syncN.setVisible(!downloading);

        if (cancel != null)
            cancel.setVisible(downloading);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onBackPressed(){
        if(mSlidingMenu.isMenuShowing()){  // if SlidingMenu is opened
            mSlidingMenu.toggle(true);     // close SlidingMenu
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            // Block menu button if sliding menu is opened or search mode
            return mSlidingMenu.isMenuShowing() || mSearchQuery == null;
        }                                   // True = event handled

        return super.onKeyDown(keyCode, event);
    }

    private void initialize(){
        setContentView(R.layout.activity_music_list);
        mAudioListAdapter = new VKAudioListAdapter(this, mAudioDownloadManager);
        mAudioLoadingLayout = (RelativeLayout)findViewById(R.id.audio_loading_layout);
        mAudioLoadingLayout.setVisibility(View.VISIBLE);
        initializePullToRefresh();
        initializeSlidingMenu();
    }

    private void initializeSlidingMenu(){
        setBehindContentView(R.layout.fragment_album_list);
        FragmentTransaction t = this.getSupportFragmentManager().beginTransaction();
        AlbumMenuFragment fragment = new AlbumMenuFragment();
        fragment.setAlbumListener(this);
        t.replace(R.id.fragment_album_list, fragment);
        t.commit();

        mSlidingMenu = getSlidingMenu();
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setShadowDrawable(R.drawable.shadow);
        mSlidingMenu.setBehindOffsetRes(R.dimen.slidingmenu_offset);
        mSlidingMenu.setFadeDegree(0.35f);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void initializePullToRefresh(){
        mPullToRefreshView = (PullToRefreshListView) findViewById(R.id.pull_to_refresh_listview);
        mPullToRefreshView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                mAudioListAdapter.clear();
                getAudioListLoader().execute(pageSize, 0);
                mOffset = pageSize;
                    //ListLoader.refresh();
            }
        });

        mPullToRefreshView.setOnLastItemVisibleListener(new PullToRefreshBase.OnLastItemVisibleListener() {
            @Override
            public void onLastItemVisible() {
                getAudioListLoader().execute(pageSize, mOffset);
                mOffset += pageSize;
            }
        });

        mPullToRefreshView.setAdapter(mAudioListAdapter);
        mPullToRefreshView.getFooterLoadingView().refreshing();
    }

    private void removeTempFile(){
        File tempFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + mFilePath,
                mTempFileName);
        if (tempFile.exists())
            tempFile.delete();
    }

    @Override
    public void onListLoadStarted() {
        if (mOffset != 0) {
            mPullToRefreshView.getFooterLoadingView().setVisibility(View.VISIBLE);

        }
        else if (!mPullToRefreshView.isRefreshing())
            mAudioLoadingLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void onAlbumSelected(long albumId) {
        mAudioListAdapter.clear();
        AudioListLoader audioListLoader = getAudioListLoader();
        audioListLoader.setAlbumId(albumId);
        audioListLoader.execute(pageSize, 0);
        mOffset = pageSize;

        if (mSlidingMenu.isMenuShowing())
            mSlidingMenu.toggle(true);
    }

    @Override
    public void onListLoadCanceled() {
        finishLoading();
    }

    @Override
    public void onListLoadFinished(TaskResult result) {
        finishLoading();
        result.handleError(this);

        if (mForceSyncCount != -1) {
            mAudioListAdapter.forceSync(mForceSyncCount);
            mForceSyncCount = -1;
        }
    }

    private void finishLoading(){
        mPullToRefreshView.onRefreshComplete();
        mPullToRefreshView.getFooterLoadingView().setVisibility(View.GONE);
        mAudioLoadingLayout.setVisibility(View.GONE);
    }

    private void setFilePath(String filePath){
        mFilePath = filePath;
        mAudioDownloadManager.setFilePath(filePath);
    }

    private void setTempFileName(String tempFileName) {
        mTempFileName = tempFileName;
        mAudioDownloadManager.setTempFileName(tempFileName);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        MenuItem searchItem = mMenu.findItem(R.id.search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        if (searchView != null) {
            searchView.clearFocus();
        }

        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //mSearchQuery = newText;
        AudioListLoader loader = getAudioListLoader();
        if (newText == "")
            loader.setSearchQuery(null);
        else
            loader.setSearchQuery(newText);

        return true;
    }
}
