package com.timky.vkmusicsync.models;

/**
 * Created by Timky on 29.03.2014.
 */
public interface IListLoadListener {
    void onListLoadCanceled();
    void onListLoadFinished(TaskResult result);
}
