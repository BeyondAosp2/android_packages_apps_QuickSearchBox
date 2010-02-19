/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.quicksearchbox.util;

import android.util.Log;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;

/**
 * Executor that uses a single thread and an unbounded work queue.
 */
public class SingleThreadNamedTaskExecutor implements NamedTaskExecutor {

    private static final String TAG = "QSB.SingleThreadNamedTaskExecutor";

    private final LinkedBlockingQueue<NamedTask> mQueue;
    private final Thread mWorker;
    private volatile boolean mClosed = false;

    public SingleThreadNamedTaskExecutor(ThreadFactory threadFactory) {
        mQueue = new LinkedBlockingQueue<NamedTask>();
        mWorker = threadFactory.newThread(new Worker());
        mWorker.start();
    }

    public void cancelPendingTasks() {
        if (mClosed) {
            throw new IllegalStateException("cancelPendingTasks() after close()");
        }
        mQueue.clear();
    }

    public void close() {
        mClosed = true;
        mWorker.interrupt();
        mQueue.clear();
    }

    public void execute(NamedTask task) {
        if (mClosed) {
            throw new IllegalStateException("execute() after close()");
        }
        mQueue.add(task);
    }

    private class Worker implements Runnable {
        public void run() {
            while (!mClosed) {
                NamedTask task;
                try {
                    task = mQueue.take();
                } catch (InterruptedException ex) {
                    break;
                }
                try {
                    task.run();
                } catch (RuntimeException ex) {
                    Log.e(TAG, "Task " + task.getName() + " failed", ex);
                }
            }
        }
    }

    public static Factory<NamedTaskExecutor> factory(final ThreadFactory threadFactory) {
        return new Factory<NamedTaskExecutor>() {
            public NamedTaskExecutor create() {
                return new SingleThreadNamedTaskExecutor(threadFactory);
            }
        };
    }

}