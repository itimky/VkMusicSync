package com.timky.vkmusicsync.models;

/**
 * Created by Timky on 29.03.2014.
 */
public interface IListLoadListener {
    public void onListLoadCanceled();
    public void onListLoadFinished(TaskResult result);
}
