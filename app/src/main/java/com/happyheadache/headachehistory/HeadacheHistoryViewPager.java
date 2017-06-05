package com.happyheadache.headachehistory;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by Alexandra Fritzen on 10/11/2016.
 */

public class HeadacheHistoryViewPager extends ViewPager {
    private boolean swipeable = true;

    public HeadacheHistoryViewPager(Context context) {
        super(context);
    }

    public HeadacheHistoryViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    // Call this method in your motion events when you want to disable or enable
    public void setSwipeable(boolean swipeable) {
        this.swipeable = swipeable;
    }

    @Override
    public boolean onTouchEvent(MotionEvent arg0) {
        return this.swipeable && super.onTouchEvent(arg0);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent arg0) {
        return this.swipeable && super.onInterceptTouchEvent(arg0);
    }
}