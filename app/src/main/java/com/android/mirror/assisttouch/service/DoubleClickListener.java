package com.android.mirror.assisttouch.service;

import android.content.Context;
import android.view.GestureDetector;

import android.view.MotionEvent;
import android.view.View;

public class DoubleClickListener implements View.OnTouchListener {
    private GestureDetector gestureDetector;

    public DoubleClickListener(Context context, final OnDoubleClickListener listener) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                listener.onDoubleClick(); // 双击时调用回调
                return super.onDoubleTap(e);
            }
        });
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        gestureDetector.onTouchEvent(event);
        return true;
    }

    public interface OnDoubleClickListener {
        void onDoubleClick();
    }
}

