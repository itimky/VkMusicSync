package com.timky.vkmusicsync;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.vk.sdk.*;
import com.vk.sdk.api.VKError;

public class LoginActivity extends Activity {

    public static final String tokenKey = "VK_ACCESS_TOKEN";
    public static String[] scope = new String[]{ VKScope.AUDIO };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.getWindow().getDecorView().setBackgroundColor(0x111111);
        VKSdk.initialize(sdkListener, "3954954", VKAccessToken.tokenFromSharedPreferences(this, tokenKey));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VKUIHelper.onResume(this);

        if (VKSdk.instance() == null)
            VKSdk.initialize(sdkListener, "3954954", VKAccessToken.tokenFromSharedPreferences(this, tokenKey));

        if (VKSdk.getAccessToken() == null)
            VKSdk.authorize(scope, true, false);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VKUIHelper.onDestroy(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        VKUIHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    private void startMusicListActivity() {
        Intent intent = new Intent(LoginActivity.this, MusicListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private VKSdkListener sdkListener = new VKSdkListener() {
        @Override
        public void onCaptchaError(VKError captchaError) {
            new VKCaptchaDialog(captchaError).show();
        }

        @Override
        public void onTokenExpired(VKAccessToken expiredToken) {
            VKSdk.authorize(scope);
        }

        @Override
        public void onAccessDenied(VKError authorizationError) {
            finish();
        }

        @Override
        public void onReceiveNewToken(VKAccessToken newToken) {
            newToken.saveTokenToSharedPreferences(LoginActivity.this, tokenKey);
            startMusicListActivity();
        }

        @Override
        public void onAcceptUserToken(VKAccessToken token) {
            startMusicListActivity();
        }
    };
}
