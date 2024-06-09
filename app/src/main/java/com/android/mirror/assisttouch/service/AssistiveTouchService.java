package com.android.mirror.assisttouch.service;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.PopupWindow;

import com.android.mirror.assisttouch.R;
import com.android.mirror.assisttouch.utils.KeyEventsSender;
import com.android.mirror.assisttouch.utils.SystemsUtils;

import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;

public class AssistiveTouchService extends AccessibilityService {

    private boolean isMoving;

    private int mScreenWidth;
    private int mScreenHeight;
    private int mStatusBarHeight;

    private int lastAssistiveTouchViewX;
    private int lastAssistiveTouchViewY;

    private View mAssistiveTouchView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mParams;

    private Instrumentation instrumentation;

    private Timer mTimer;

    private LayoutInflater mInflater;

    @Override
    public void onCreate() {
        super.onCreate();
        init();
        calculateForMyPhone();
        createAssistiveTouchView();
        instrumentation = new Instrumentation();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {

    }

    @Override
    public void onInterrupt() {

    }

    private void init() {
        mTimer = new Timer();
        mParams = new WindowManager.LayoutParams();
        mWindowManager = (WindowManager) this.getSystemService(WINDOW_SERVICE);
        mInflater = LayoutInflater.from(this);
        mAssistiveTouchView = mInflater.inflate(R.layout.assistive_touch_layout, null);
        mAssistiveTouchView.setAlpha(1.0f);
    }

    private void calculateForMyPhone() {
        DisplayMetrics displayMetrics = SystemsUtils.getScreenSize(this);
        mScreenWidth = displayMetrics.widthPixels;
        mScreenHeight = displayMetrics.heightPixels;
        mStatusBarHeight = SystemsUtils.getStatusBarHeight(this);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void createAssistiveTouchView() {
        mParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
        mParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        mParams.x = mScreenWidth;
        mParams.y = 520;
        mParams.gravity = Gravity.TOP | Gravity.LEFT;
        mParams.format = PixelFormat.RGBA_8888;
        mParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        mWindowManager.addView(mAssistiveTouchView, mParams);
        mAssistiveTouchView.setOnTouchListener(new View.OnTouchListener() {
            private GestureDetector gestureDetector = new GestureDetector(AssistiveTouchService.this.getBaseContext(), new GestureDetector.SimpleOnGestureListener() {
                @Override
                public void onLongPress(MotionEvent e) {
                    if (isMoving) return;
                    try {
                        performGlobalAction(AssistiveTouchService.GLOBAL_ACTION_DPAD_CENTER);
                    } catch (Exception ignored) {
                    }
                    super.onLongPress(e);
                }

                @Override
                public boolean onSingleTapConfirmed(MotionEvent e) {
                    KeyEventsSender.sendKeyEvent("KEYCODE_BACK");
                    return true;
                }

                @Override
                public boolean onDoubleTap(MotionEvent e) {
                    KeyEventsSender.sendKeyEvent("KEYCODE_HOME");
                    return true;
                }
            });

            public void sendKeyEvent(final int keyCode) {
                new Thread(() -> instrumentation.sendKeyDownUpSync(keyCode)).start();
            }

            private float initX = 0f;
            private float initY = 0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                gestureDetector.onTouchEvent(event);

                float rawX = event.getRawX();
                float rawY = event.getRawY();

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isMoving = false;
                        initX = rawX;
                        initY = rawY;
                        break;
                    case MotionEvent.ACTION_UP:
                        AssistiveTouchService.this.setAssistiveTouchViewAlign();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Context context = AssistiveTouchService.this;
                        float threshold = SystemsUtils.convertDpToPixel(15, context);
                        if (Math.abs(rawX - initX) > threshold || Math.abs(rawY - initY) > threshold) {
                            isMoving = true;
                            mParams.x = (int) (rawX - mAssistiveTouchView.getMeasuredWidth() / 2);
                            mParams.y = (int) (rawY - mAssistiveTouchView.getMeasuredHeight() / 2 - mStatusBarHeight);
                            mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
                        }
                }
                if (isMoving)
                    return true;
                else
                    return false;
            }
        });
    }


    private ValueAnimator myAssistiveTouchAnimator(final int fromx, final int tox, int fromy, final int toy, final boolean flag) {
        PropertyValuesHolder p1 = PropertyValuesHolder.ofInt("X", fromx, tox);
        PropertyValuesHolder p2 = PropertyValuesHolder.ofInt("Y", fromy, toy);
        ValueAnimator v1 = ValueAnimator.ofPropertyValuesHolder(p1, p2);
        v1.setDuration(100L);
        v1.setInterpolator(new DecelerateInterpolator());
        v1.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer x = (Integer) animation.getAnimatedValue("X");
                Integer y = (Integer) animation.getAnimatedValue("Y");
                mParams.x = x;
                mParams.y = y;
                mWindowManager.updateViewLayout(mAssistiveTouchView, mParams);
            }
        });
        return v1;
    }

    private void setAssistiveTouchViewAlign() {
        int mAssistiveTouchViewWidth = mAssistiveTouchView.getMeasuredWidth();
        int mAssistiveTouchViewHeight = mAssistiveTouchView.getMeasuredHeight();
        int top = mParams.y + mAssistiveTouchViewWidth / 2;
        int left = mParams.x + mAssistiveTouchViewHeight / 2;
        int right = mScreenWidth - mParams.x - mAssistiveTouchViewWidth / 2;
        int bottom = mScreenHeight - mParams.y - mAssistiveTouchViewHeight / 2;
        int lor = Math.min(left, right);
        int tob = Math.min(top, bottom);
        int min = Math.min(lor, tob);
        lastAssistiveTouchViewX = mParams.x;
        lastAssistiveTouchViewY = mParams.y;
        if (min == top) mParams.y = 0;
        if (min == left) mParams.x = 0;
        if (min == right) mParams.x = mScreenWidth - mAssistiveTouchViewWidth;
        if (min == bottom) mParams.y = mScreenHeight - mAssistiveTouchViewHeight;
        myAssistiveTouchAnimator(lastAssistiveTouchViewX, mParams.x, lastAssistiveTouchViewY, mParams.y, false).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        mWindowManager.removeView(mAssistiveTouchView);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("info.plateaukao.action.DISABLE_SERVICE".equals(intent.getAction())) {
                disableSelf();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        IntentFilter filter = new IntentFilter("info.plateaukao.action.DISABLE_SERVICE");
        registerReceiver(receiver, filter);
    }
}
