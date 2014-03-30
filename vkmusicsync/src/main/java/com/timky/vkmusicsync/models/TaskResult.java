package com.timky.vkmusicsync.models;

import android.content.Context;
import android.widget.Toast;

import com.timky.vkmusicsync.LoginActivity;
import com.timky.vkmusicsync.R;

/**
 * Created by timky on 21.03.14.
 */
public class TaskResult {
    public String errorMessage;
    public String fullFileName;
    public int errorCode;

    public void handleError(Context context){
        if (errorMessage != null)
            switch (errorCode) {
                case ErrorCodes.needReLogin:
                    LoginActivity.reLogin(context);
                    break;
                case ErrorCodes.connectionRefused:
                    Toast.makeText(context, context.getString(R.string.vk_no_internet_access),
                            Toast.LENGTH_SHORT).show();
                    break;
                default:
                    Toast.makeText(context, errorMessage,
                            Toast.LENGTH_SHORT).show();
            }

    }
}

