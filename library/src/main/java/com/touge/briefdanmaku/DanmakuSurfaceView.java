package com.touge.briefdanmaku;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class DanmakuSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = "DanmakuView";
    public static final float MAX_RUNNING_COUNT_FACTOR = 1.5f;

    private AtomicInteger mSequenceGenerator = new AtomicInteger();
    private int mMaxLines;

    private SparseArray<Deque<DanmakuItem>> mRunningLines;
    private final BlockingQueue<DanmakuItem> mCacheQueue = new PriorityBlockingQueue<>();
    private CacheDispatcher mCacheDispatcher;
    private DrawHelper mDrawHelper;

    private SurfaceHolder mSurfaceHolder;

    public DanmakuSurfaceView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.DanmakuView, 0, 0);
        mMaxLines = ta.getInteger(R.styleable.DanmakuView_max_lines, 1);
        ta.recycle();

        mSurfaceHolder = getHolder();
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);

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
    public void surfaceCreated(SurfaceHolder holder) {
        mDrawHelper = new DrawHelper(holder, mRunningLines);
        mDrawHelper.start();
//        Canvas canvas = holder.lockCanvas();
//        if (canvas == null) {
//            return;
//        }
//        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
//        holder.unlockCanvasAndPost(canvas);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mSurfaceHolder.removeCallback(this);
    }

    public void start() {
        mDrawHelper.setStatus(DrawHelper.Status.RUNNING);
    }

    public void stop() {
        mDrawHelper.setStatus(DrawHelper.Status.PENDING);
    }

    public void finish() {
        mDrawHelper.setStatus(DrawHelper.Status.FINISHED);
        release();
        if (mCacheDispatcher != null) {
            mCacheDispatcher.quit();
        }
        if (mDrawHelper != null) {
            mDrawHelper.quit();
        }
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
