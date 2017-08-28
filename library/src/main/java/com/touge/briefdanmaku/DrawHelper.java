package com.touge.briefdanmaku;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Process;
import android.util.SparseArray;
import android.view.SurfaceHolder;

import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;

public class DrawHelper extends Thread {

    private SurfaceHolder mSurfaceHolder;
    private SparseArray<Deque<DanmakuItem>> mRunningLines;

    private volatile boolean mQuit = false;

    private volatile Status mStatus = Status.PENDING;

    public enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    public DrawHelper(SurfaceHolder surfaceHolder, SparseArray<Deque<DanmakuItem>> runningLines) {
        mSurfaceHolder = surfaceHolder;
        mRunningLines = runningLines;
    }

    public void quit() {
        mQuit = true;
        interrupt();
    }

    public void setStatus(Status status) {
        mStatus = status;
    }

    @Override
    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
        while (true) {
            Canvas canvas = null;
            boolean continueDraw = false;
            try {
                if (!mSurfaceHolder.getSurface().isValid()) {
                    return;
                }
                canvas = mSurfaceHolder.lockCanvas();
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

                if (mStatus == Status.RUNNING) {
                    int emptyLineCount = 0;
                    for (int i = 0; i < mRunningLines.size(); i++) {
                        Deque<DanmakuItem> line = mRunningLines.get(i);
                        if (line.isEmpty()) {
                            emptyLineCount++;
                        }
                        for (Iterator<DanmakuItem> iterator = line.iterator(); iterator.hasNext(); ) {
                            DanmakuItem item = iterator.next();
                            if (item.inVisible()) {
                                iterator.remove();
                            } else {
                                item.drawInternal(canvas);
                            }
                        }
                    }
                    if (emptyLineCount != mRunningLines.size()) {
                        continueDraw = true;
                    }
                }
            } catch (ConcurrentModificationException e) {
                // Ignore exceptions, continue invalidate.
                continueDraw = true;
            } finally {
                if (mSurfaceHolder.getSurface().isValid()) {
                    mSurfaceHolder.unlockCanvasAndPost(canvas);
                }
                if (!continueDraw) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignore) {
                    }
                }
            }
        }
    }
}
