package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import java.util.ArrayList;
import java.util.Random;

public class SunView extends View {
    private static final int DEFAULT_RAY_COUNT = 12;
    private static final float MIN_RAY_LENGTH = 150;
    private static final float MAX_RAY_LENGTH = 250;
    private static final float MIN_RAY_SPEED = 1;
    private static final float MAX_RAY_SPEED = 3;

    private ArrayList<SunRay> sunRays = new ArrayList<>();
    private Paint paint;
    private Random random;
    private int screenWidth;
    private int screenHeight;
    private boolean isShining = true;
    private Animation rotateAnimation;

    public SunView(Context context) {
        super(context);
        init();
    }

    public SunView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SunView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        random = new Random();
        paint = new Paint();
        paint.setAntiAlias(true);
        
        // Khởi tạo animation
        rotateAnimation = AnimationUtils.loadAnimation(getContext(), R.anim.sun_rotate);
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        createSunRays(DEFAULT_RAY_COUNT);
    }

    private void createSunRays(int count) {
        sunRays.clear();
        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 3f;
        
        for (int i = 0; i < count; i++) {
            float angle = (360f / count) * i;
            float length = MIN_RAY_LENGTH + random.nextFloat() * (MAX_RAY_LENGTH - MIN_RAY_LENGTH);
            float speed = MIN_RAY_SPEED + random.nextFloat() * (MAX_RAY_SPEED - MIN_RAY_SPEED);
            int alpha = 150 + random.nextInt(105);
            sunRays.add(new SunRay(centerX, centerY, length, speed, angle, alpha));
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isShining) return;

        float centerX = screenWidth / 2f;
        float centerY = screenHeight / 3f;

        // Vẽ vòng tròn mặt trời với gradient
        paint.setStyle(Paint.Style.FILL);
        LinearGradient sunGradient = new LinearGradient(
            centerX - 50, centerY - 50,
            centerX + 50, centerY + 50,
            new int[]{Color.argb(255, 255, 255, 200), Color.argb(255, 255, 255, 100)},
            null, Shader.TileMode.CLAMP
        );
        paint.setShader(sunGradient);
        canvas.drawCircle(centerX, centerY, 50, paint);
        paint.setShader(null);

        // Vẽ các tia nắng
        paint.setStyle(Paint.Style.STROKE);
        for (SunRay ray : sunRays) {
            float endX = ray.x + (float) (Math.cos(Math.toRadians(ray.angle)) * ray.length);
            float endY = ray.y + (float) (Math.sin(Math.toRadians(ray.angle)) * ray.length);

            // Tạo gradient cho tia nắng
            LinearGradient rayGradient = new LinearGradient(
                ray.x, ray.y,
                endX, endY,
                new int[]{Color.argb(ray.alpha, 255, 255, 200), Color.argb(ray.alpha/2, 255, 255, 100)},
                null, Shader.TileMode.CLAMP
            );
            paint.setShader(rayGradient);
            canvas.drawLine(ray.x, ray.y, endX, endY, paint);
            paint.setShader(null);

            // Di chuyển tia nắng
            ray.angle += ray.speed;
            if (ray.angle >= 360) {
                ray.angle -= 360;
            }
        }

        invalidate();
    }

    public void startShining() {
        isShining = true;
        startAnimation(rotateAnimation);
        invalidate();
    }

    public void stopShining() {
        isShining = false;
        clearAnimation();
    }

    public void setRayIntensity(int count) {
        createSunRays(count);
    }

    private class SunRay {
        float x, y;
        float length;
        float speed;
        float angle;
        int alpha;

        SunRay(float x, float y, float length, float speed, float angle, int alpha) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.speed = speed;
            this.angle = angle;
            this.alpha = alpha;
        }
    }
}
