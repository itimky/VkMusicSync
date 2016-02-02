package com.timky.vkmusicsync.loaders;

import android.os.AsyncTask;

import com.timky.vkmusicsync.models.TaskResult;

import java.io.InvalidObjectException;
import java.util.List;

/**
 * Don't use execute method!
 * Created by timky on 24.03.14.
 */
public abstract class ListLoader<T> extends AsyncTask<Integer, Void, ListLoader.Result> {
    private boolean mIsLoading;
    private boolean mIsAlreadyUsed;
    public boolean isLoading() {
        return mIsLoading;
    }

    @Override
    protected void onPreExecute(){
        if (!mIsAlreadyUsed)
            mIsAlreadyUsed = true;
        else
            System.out.print("ListLoader instance can be used only once");
        super.onPreExecute();
        mIsLoading = true;
        preLoad();
    }

    /**
     *
     * @param   params
     *          params[0] - Count
     *          params[1] - Offset
     */
    @Override
    protected Result doInBackground(Integer... params) {
        if (isCancelled())
            return null;

        return load(params[0], params[1]);
    }

    @Override
    protected void onPostExecute(ListLoader.Result result) {
        super.onPostExecute(result);
        mIsLoading = false;
        postLoad(result);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mIsLoading = false;
        cancelled();
    }

    protected abstract void preLoad();
    protected abstract Result load(int count, int offset);
    protected abstract void postLoad(ListLoader.Result result);
    protected abstract void cancelled();

    class Result extends TaskResult {
        public List<T> loaded;
    }
}