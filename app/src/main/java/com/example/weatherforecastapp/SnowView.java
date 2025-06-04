package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.Random;

public class SnowView extends View {
    private ArrayList<Snowflake> snowflakes;
    private Paint snowPaint;
    private Paint backgroundPaint;
    private Random random;
    private boolean isSnowing = true;
    private int snowIntensity = 150; // Số lượng bông tuyết
    private long lastUpdateTime;
    private float screenWidth, screenHeight;

    // Class để quản lý từng bông tuyết
    private class Snowflake {
        float x, y;
        float speed;
        float size;
        float swingAmplitude;
        float swingOffset;
        float opacity;

        Snowflake() {
            reset();
            // Khởi tạo vị trí ngẫu nhiên trên màn hình
            y = random.nextFloat() * screenHeight;
        }

        void reset() {
            x = random.nextFloat() * screenWidth;
            y = -size;
            speed = 1 + random.nextFloat() * 2; // Tốc độ rơi 1-3
            size = 2 + random.nextFloat() * 6; // Kích thước 2-8
            swingAmplitude = 20 + random.nextFloat() * 30; // Biên độ lắc 20-50
            swingOffset = random.nextFloat() * (float) Math.PI * 2; // Pha ban đầu
            opacity = 0.6f + random.nextFloat() * 0.4f; // Độ trong suốt 0.6-1.0
        }

        void update(float deltaTime) {
            // Cập nhật vị trí Y (rơi xuống)
            y += speed * deltaTime * 60;

            // Lắc lư theo phương ngang (hiệu ứng gió nhẹ)
            float swingX = (float) Math.sin((y / 100) + swingOffset) * swingAmplitude;
            x = x + swingX * deltaTime;

            // Giữ bông tuyết trong màn hình theo phương ngang
            if (x < -size) x = screenWidth + size;
            if (x > screenWidth + size) x = -size;

            // Reset khi rơi ra khỏi màn hình
            if (y > screenHeight + size) {
                reset();
            }
        }

        void draw(Canvas canvas, Paint paint) {
            int alpha = (int) (opacity * 255);
            paint.setAlpha(alpha);

            // Vẽ bông tuyết với hiệu ứng mờ
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, size, paint);

            // Vẽ thêm hiệu ứng sáng nhẹ
            paint.setAlpha(alpha / 2);
            canvas.drawCircle(x, y, size * 1.5f, paint);
        }
    }

    public SnowView(Context context) {
        super(context);
        init();
    }

    public SnowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        snowflakes = new ArrayList<>();
        random = new Random();
        lastUpdateTime = System.currentTimeMillis();

        // Paint cho bông tuyết
        snowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        snowPaint.setColor(Color.WHITE);
        snowPaint.setStyle(Paint.Style.FILL);

        // Paint cho background
        backgroundPaint = new Paint();
        backgroundPaint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Tạo gradient background mùa đông
        backgroundPaint.setShader(new RadialGradient(
                w / 2f, 0,
                h * 1.2f,
                new int[] {
                        Color.rgb(220, 230, 240), // Xanh nhạt ở trên
                        Color.rgb(180, 200, 220), // Xanh đậm hơn ở giữa
                        Color.rgb(160, 180, 200)  // Xanh xám ở dưới
                },
                new float[] {0f, 0.6f, 1f},
                Shader.TileMode.CLAMP
        ));

        // Khởi tạo bông tuyết
        createSnowflakes();
    }

    private void createSnowflakes() {
        snowflakes.clear();
        for (int i = 0; i < snowIntensity; i++) {
            snowflakes.add(new Snowflake());
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Vẽ background mùa đông
        canvas.drawRect(0, 0, screenWidth, screenHeight, backgroundPaint);

        if (!isSnowing || snowflakes.isEmpty()) {
            return;
        }

        // Tính toán thời gian delta
        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000f;
        lastUpdateTime = currentTime;

        // Vẽ hiệu ứng sương mù nhẹ
        Paint fogPaint = new Paint();
        fogPaint.setColor(Color.argb(30, 255, 255, 255));
        canvas.drawRect(0, 0, screenWidth, screenHeight, fogPaint);

        // Cập nhật và vẽ từng bông tuyết
        for (Snowflake snowflake : snowflakes) {
            snowflake.update(deltaTime);
            snowflake.draw(canvas, snowPaint);
        }

        // Vẽ lớp tuyết tích tụ ở dưới
        drawSnowAccumulation(canvas);

        // Tiếp tục animation
        if (isSnowing) {
            invalidate();
        }
    }

    private void drawSnowAccumulation(Canvas canvas) {
        Paint snowGroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        snowGroundPaint.setColor(Color.argb(180, 255, 255, 255));
        snowGroundPaint.setStyle(Paint.Style.FILL);

        // Vẽ lớp tuyết dưới đáy với hiệu ứng gợn sóng
        float baseHeight = screenHeight - 50;
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(0, screenHeight);

        for (int x = 0; x <= screenWidth; x += 20) {
            float waveHeight = (float) Math.sin(x * 0.02) * 10;
            path.lineTo(x, baseHeight + waveHeight);
        }

        path.lineTo(screenWidth, screenHeight);
        path.close();

        canvas.drawPath(path, snowGroundPaint);
    }

    public void startSnow() {
        isSnowing = true;
        lastUpdateTime = System.currentTimeMillis();
        invalidate();
    }

    public void stopSnow() {
        isSnowing = false;
    }

    public void setSnowIntensity(int intensity) {
        this.snowIntensity = Math.max(50, Math.min(300, intensity));
        createSnowflakes();
    }
}