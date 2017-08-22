package com.touge.briefdanmaku;

import android.os.Process;
import android.util.SparseArray;

import java.util.Deque;
import java.util.concurrent.BlockingQueue;

public class CacheDispatcher extends Thread {

    private BlockingQueue<DanmakuItem> mCacheQueue;
    private SparseArray<Deque<DanmakuItem>> mRunningLines;

    private volatile boolean mQuit = false;

    public CacheDispatcher(BlockingQueue<DanmakuItem> cacheQueue, SparseArray<Deque<DanmakuItem>> runningLines) {
        mCacheQueue = cacheQueue;
        mRunningLines = runningLines;
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
                int offsetX = target.getOffsetX(), resultLine = -1;
                for (int i = 0; i < mRunningLines.size(); i++) {
                    Deque<DanmakuItem> line = mRunningLines.valueAt(i);
                    if (line.isEmpty()) {
                        resultLine = i;
                        break;
                    }
                    DanmakuItem last = line.peekLast();
                    int lastOffsetX = last.getOffsetX() + last.getWidth();
                    if (offsetX > lastOffsetX) {
                        offsetX = lastOffsetX;
                        resultLine = i;
                    }
                }
                if (resultLine != -1) {
                    target.setLine(resultLine);
                    mRunningLines.get(resultLine).add(target);
                } else {
                    mCacheQueue.add(target);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
