package com.touge.briefdanmaku;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextPaint;

public class DanmakuItem implements Comparable<DanmakuItem> {

    private static final int DEFAULT_TEXT_COLOR = Color.WHITE;
    private static final int DEFAULT_TEXT_SIZE = 15;
    private static final int DEFAULT_SPEED = 5;

    private Context mContext;
    private Integer mSequence;
    private SpannableString mText;
    private int mOffsetX;
    private int mLine;
    private int mTextColor;
    private int mTextSize;
    private long mTime;

    private int mWidth;
    private int mHeight;
    private StaticLayout mTextStaticLayout;

    public DanmakuItem(Context context, SpannableString text, long time, int offsetX) {
        this(context, text, time, offsetX, DEFAULT_TEXT_COLOR, DEFAULT_TEXT_SIZE);
    }

    public DanmakuItem(Context context, SpannableString text, long time, int offsetX, int textColor, int textSize) {
        mContext = context;
        mText = text;
        mTime = time;
        mOffsetX = offsetX;
        mTextColor = textColor;
        setTextSize(textSize);
        initText();
    }

    private void initText() {
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setColor(mTextColor);
        tp.setTextSize(mTextSize);
        mTextStaticLayout = new StaticLayout(
                mText,
                tp,
                (int) Layout.getDesiredWidth(mText, 0, mText.length(), tp),
                Layout.Alignment.ALIGN_NORMAL,
                1,
                0,
                false
        );
        mWidth = mTextStaticLayout.getWidth();
        mHeight = (int) tp.getFontSpacing();
    }

    public void drawInternal(Canvas canvas) {
        canvas.save();
        canvas.translate(mOffsetX, mLine * mHeight);
        mTextStaticLayout.draw(canvas);
        canvas.restore();

        mOffsetX -= DEFAULT_SPEED;
    }

    public final void setSequence(int sequence) {
        mSequence = sequence;
    }

    public final int getSequence() {
        if (mSequence == null) {
            throw new IllegalStateException("getSequence called before setSequence");
        }
        return mSequence;
    }

    public void setTextSize(int size) {
        if (size > DEFAULT_TEXT_SIZE) {
            mTextSize = dip2px(mContext, size);
        } else {
            mTextSize = dip2px(mContext, DEFAULT_TEXT_SIZE);
        }
    }

    public void setTextColor(@ColorInt int color) {
        mTextColor = color;
    }

    public boolean inVisible() {
        return mOffsetX < 0 && Math.abs(mOffsetX) > mWidth;
    }

    public int getOffsetX() {
        return mOffsetX;
    }

    public int getLine() {
        return mLine;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public void setLine(int line) {
        mLine = line;
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    @Override
    public int compareTo(@NonNull DanmakuItem o) {
        return mTime > o.mTime ? 1 : -1;
    }
}
