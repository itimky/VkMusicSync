package com.timky.vkmusicsync;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;

import android.support.v7.app.ActionBar;
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
import com.timky.vkmusicsync.helpers.AudioListLoader;
import com.timky.vkmusicsync.helpers.VKAudioDownloadManager;
import com.timky.vkmusicsync.helpers.VKAudioListAdapter;
import com.timky.vkmusicsync.models.IAudioListLoadListener;
import com.timky.vkmusicsync.models.TaskResult;
import com.vk.sdk.VKAccessToken;
import com.vk.sdk.VKSdk;
import com.vk.sdk.VKUIHelper;

import java.io.File;

public class MusicListActivity extends SlidingFragmentActivity implements IAudioListLoadListener{

    public static final int pageSize = 20;

    private PullToRefreshListView mPullToRefreshView;
    private VKAudioListAdapter mAudioListAdapter;
    private RelativeLayout mAudioLoadingLayout;
    private SlidingMenu mMenu;
    private String mFilePath = "Music/";      // Slash is required
    private String mTempFileName = "sync.tmp";
    private int mForceSyncCount = -1;
    private VKAudioDownloadManager mAudioDownloadManager = new VKAudioDownloadManager(mFilePath, mTempFileName);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        removeTempFile();
        initialize();

        // In Android 2.3.3 logo setup in AndroidManifest.xml not working
        ActionBar actionBar = getSupportActionBar();
        actionBar.setLogo(R.drawable.ic_vkmusicsync_logo);

        AudioListLoader.initialize(pageSize);
        AudioListLoader.setAudioSupport(mAudioListAdapter, this);
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
                mMenu.toggle(true);
                return true;

            case R.id.sync_first_10:
                mForceSyncCount = 10;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                AudioListLoader.refresh(mForceSyncCount);
                return true;

            case R.id.sync_first_50:
                mForceSyncCount = 50;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                AudioListLoader.refresh(mForceSyncCount);
                return true;

            case R.id.sync_first_100:
                mForceSyncCount = 100;
                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                AudioListLoader.refresh(mForceSyncCount);
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
                                    String count = editText.getText().toString();

                                if (count.compareTo("") == 0)
                                    return;

                                mForceSyncCount = Integer.parseInt(editText.getText().toString());
                                mAudioLoadingLayout.setVisibility(View.VISIBLE);
                                AudioListLoader.refresh(mForceSyncCount);
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
        if(mMenu.isMenuShowing()){  // if SlidingMenu is opened
            mMenu.toggle(true);     // close SlidingMenu
            return;
        }

        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            return mMenu.isMenuShowing();  // Block menu button if sliding menu is opened
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

    private void removeTempFile(){
        File tempFile = new File(Environment.getExternalStorageDirectory().getPath() + "/" + mFilePath,
                mTempFileName);
        if (tempFile.exists())
            tempFile.delete();
    }

    @Override
    public void onListLoadStarted(boolean fullRefresh) {
        if (!fullRefresh) {
            mPullToRefreshView.getFooterLoadingView().setVisibility(View.VISIBLE);

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
