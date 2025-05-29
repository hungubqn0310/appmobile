package com.example.weatherforecastapp;

import android.content.Context;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.ScrollView;

public class HorizontalSwipeScrollView extends ScrollView {
    private GestureDetector gestureDetector;
    private OnHorizontalSwipeListener swipeListener;

    public interface OnHorizontalSwipeListener {
        boolean onSwipeRight();
    }

    public HorizontalSwipeScrollView(Context context) {
        super(context);
        init(context);
    }

    public HorizontalSwipeScrollView(Context context, android.util.AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HorizontalSwipeScrollView(Context context, android.util.AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                // Chỉ xử lý swipe ngang từ trái sang phải
                if (Math.abs(diffX) > Math.abs(diffY) &&
                        diffX > 150 &&
                        Math.abs(velocityX) > 200) {

                    if (swipeListener != null) {
                        return swipeListener.onSwipeRight();
                    }
                }
                return false;
            }
        });
    }

    public void setOnHorizontalSwipeListener(OnHorizontalSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // Cho gesture detector xử lý trước
        gestureDetector.onTouchEvent(ev);
        // Sau đó cho ScrollView xử lý bình thường
        return super.dispatchTouchEvent(ev);
    }
}