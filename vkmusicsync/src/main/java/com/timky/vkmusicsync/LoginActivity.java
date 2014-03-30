package com.timky.vkmusicsync;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
        VKSdk.initialize(sdkListener, "3954954", VKAccessToken.tokenFromSharedPreferences(this, tokenKey));
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
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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

        @Override
        public void onAuthDecline() {
            finish();
        }
    };

    public static void reLogin(Context context) {
        VKAccessToken token = VKSdk.getAccessToken();

        if (token != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor edit = prefs.edit();
            edit.remove(LoginActivity.tokenKey);
            edit.commit();
            VKSdk.setAccessToken(null, true);
        }

        startLoginActivity(context);
        if (context instanceof Activity)
            ((Activity)context).finish();
    }

    private static void startLoginActivity(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        context.startActivity(intent);
    }
}
