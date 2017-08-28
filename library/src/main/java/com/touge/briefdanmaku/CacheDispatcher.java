package com.touge.briefdanmaku;

import android.os.Process;
import android.util.Log;
import android.util.SparseArray;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;

public class CacheDispatcher extends Thread {
    private static final long DEFAULT_SLEEP_INTERVAL = 1000;

    private BlockingQueue<DanmakuItem> mCacheQueue;
    private SparseArray<Deque<DanmakuItem>> mRunningLines;
    private int mMaxRunningCount;

    private volatile boolean mQuit = false;

    public CacheDispatcher(BlockingQueue<DanmakuItem> cacheQueue, SparseArray<Deque<DanmakuItem>> runningLines, int maxRunningCount) {
        mCacheQueue = cacheQueue;
        mRunningLines = runningLines;
        mMaxRunningCount = maxRunningCount;
    }

    public void quit() {
        mQuit = true;
        interrupt();
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            DanmakuItem target;
            try {
                target = mCacheQueue.take();
            } catch (InterruptedException e) {
                if (mQuit) {
                    return;
                }
                continue;
            }

            try {
                int offsetX = target.getOffsetX(), resultLine = -1, totalCount = 0;
                for (int i = 0; i < mRunningLines.size(); i++) {
                    Deque<DanmakuItem> line = mRunningLines.valueAt(i);
                    totalCount += line.size();
                    // Join empty line without thinking.
                    if (line.isEmpty()) {
                        resultLine = i;
                        break;
                    }
                    // Find the line with farthest danmaku.
                    DanmakuItem last = line.peekLast();
                    int lastOffsetX = last.getOffsetX() + last.getWidth();
                    if (offsetX > lastOffsetX) {
                        offsetX = lastOffsetX;
                        resultLine = i;
                    }
                }
                if (resultLine != -1 && totalCount < mMaxRunningCount) {
                    Log.d("@@@", "add target line:" + resultLine + ", totalCount:" + totalCount);
                    target.setLine(resultLine);
                    mRunningLines.get(resultLine).add(target);
                } else {
                    mCacheQueue.add(target);
                    // If there is no suitable line, just sleep for a while.
                    sleep(DEFAULT_SLEEP_INTERVAL);
                }
            } catch (Exception e) {
                if (mQuit) {
                    return;
                }
            }
        }
    }
}
