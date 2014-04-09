package com.timky.vkmusicsync.models;

import android.util.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Created by timky on 03.04.14.
 */
public class AsyncList<T> implements Iterable<T>{
    private final List<T> mAsyncList = new ArrayList<T>();
    // Second value of pair - action flag. False - remove, True - add
    private final List<Pair<T, Boolean>> mQueue = new ArrayList<Pair<T, Boolean>>();
    private boolean mIsIterating = false;

    /**
     * Sets flag "IsIterating" to prevent exception if anybody will try to add or
     * remove when iterating List
     */
    protected void beginIterating(){
        mIsIterating = true;
    }

    /**
     * Unsets flag "IsIterating" and adds/removes items wishing to
     */
    protected void endIterating(){
        mIsIterating = false;

        for (Pair<T, Boolean> request : mQueue)
            if (request.second)
                mAsyncList.add(request.first);
            else
                mAsyncList.remove(request.first);


        mQueue.clear();
    }

    public void add(T item){
        if (mIsIterating)
            mQueue.add(new Pair<T, Boolean>(item, true));
        else
            mAsyncList.add(item);
    }

    public void remove(T item){
        if (mIsIterating)
            mQueue.add(new Pair<T, Boolean>(item, false));
        else
            mAsyncList.remove(item);
    }

    public T findById(int id) {
        if (mAsyncList.size() == 0 || !(mAsyncList.get(0) instanceof IIdObject))
            return null;

        T result = null;

        for (T item : this)
            if (((IIdObject)item).getId() == id) {
                result = item;
                break;
            }

        endIterating();

        return result;
    }

    public T get(int location){
        return mAsyncList.get(location);
    }

    public int size(){
        return mAsyncList.size();
    }

    @Override
    public AsyncListIterator iterator() {
        return new AsyncListIterator();
    }

    private class AsyncListIterator implements Iterator<T> {
        private int mCurrent;

        AsyncListIterator() {
            mCurrent = 0;
            beginIterating();
        }

        @Override
        public boolean hasNext() {

            boolean hasNext = mCurrent < mAsyncList.size();

            if (!hasNext)
                endIterating();

            return hasNext;
        }

        @Override
        public T next() {
            if (!hasNext())
                throw new NoSuchElementException();

            return mAsyncList.get(mCurrent++);
        }

        /**
         * Does nothing
         */
        @Deprecated
        @Override
        public void remove() {}
    }
}
