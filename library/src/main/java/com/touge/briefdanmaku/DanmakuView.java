package com.touge.briefdanmaku;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;

import java.util.ConcurrentModificationException;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DanmakuView extends View {
    private static final String TAG = "DanmakuView";
    public static final float MAX_RUNNING_COUNT_FACTOR = 1.5f;

    private AtomicInteger mSequenceGenerator = new AtomicInteger();

    private Status mStatus = Status.PENDING;
    private int mMaxLines;

    private SparseArray<Deque<DanmakuItem>> mRunningLines;
    private final BlockingQueue<DanmakuItem> mCacheQueue = new PriorityBlockingQueue<>();
    private CacheDispatcher mCacheDispatcher;

    public enum Status {
        PENDING,
        RUNNING,
        FINISHED
    }

    public DanmakuView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DanmakuView, 0, 0);
        mMaxLines = ta.getInteger(R.styleable.DanmakuView_max_lines, 1);
        ta.recycle();

        setBackgroundColor(Color.TRANSPARENT);
        setDrawingCacheBackgroundColor(Color.TRANSPARENT);

        int maxRunningCount = (int) (mMaxLines * MAX_RUNNING_COUNT_FACTOR);
        mRunningLines = new SparseArray<>(mMaxLines);
        for (int i = 0; i < mMaxLines; i++) {
            mRunningLines.put(i, new LinkedList<DanmakuItem>());
        }
        mCacheDispatcher = new CacheDispatcher(mCacheQueue, mRunningLines, maxRunningCount);
        mCacheDispatcher.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mStatus == Status.RUNNING) {
            try {
                // draw running items
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
                    invalidate();
                }
            } catch (ConcurrentModificationException e) {
                // Ignore exceptions, continue invalidate.
                invalidate();
            }

        } else {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        }
    }

    public void start() {
        mStatus = Status.RUNNING;
        invalidate();
    }

    public void stop() {
        mStatus = Status.PENDING;
        invalidate();
    }

    public void finish() {
        mStatus = Status.FINISHED;
        release();
        if (mCacheDispatcher != null) {
            mCacheDispatcher.quit();
        }
        invalidate();
    }

    public boolean isPaused() {
        return mStatus == Status.PENDING;
    }

    private void release() {
        if (mRunningLines != null && mRunningLines.size() != 0) {
            mRunningLines.clear();
        }
        if (mCacheQueue != null && !mCacheQueue.isEmpty()) {
            mCacheQueue.clear();
        }
    }

    public int getSequenceNumber() {
        return mSequenceGenerator.incrementAndGet();
    }

    public void addDanmaku(DanmakuItem item) {
        synchronized (mCacheQueue) {
            item.setSequence(getSequenceNumber());
            mCacheQueue.add(item);
        }
    }
}
