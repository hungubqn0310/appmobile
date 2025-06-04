package com.example.weatherforecastapp; // Thay thế bằng package của bạn

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.Random;

public class RainView extends View {

    private static final int DEFAULT_RAIN_COUNT = 150;
    private static final float MIN_RAIN_LENGTH = 20;
    private static final float MAX_RAIN_LENGTH = 60;
    private static final float MIN_RAIN_SPEED = 15;
    private static final float MAX_RAIN_SPEED = 30;

    private ArrayList<Raindrop> raindrops = new ArrayList<>();
    private Paint paint;
    private Random random;
    private int screenWidth;
    private int screenHeight;
    private boolean isRaining = true;

    public RainView(Context context) {
        super(context);
        init();
    }

    public RainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RainView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        random = new Random();

        paint = new Paint();
        // Màu trắng đậm
        paint.setColor(Color.argb(220, 255, 255, 255));
        paint.setStrokeWidth(3); // Tăng độ dày từ 2 lên 3
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setAntiAlias(true);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

        // Khởi tạo hạt mưa
        createRaindrops(DEFAULT_RAIN_COUNT);
    }

    private void createRaindrops(int count) {
        raindrops.clear();
        for (int i = 0; i < count; i++) {
            raindrops.add(createRandomRaindrop());
        }
    }

    private Raindrop createRandomRaindrop() {
        float x = random.nextFloat() * screenWidth;
        float y = random.nextFloat() * screenHeight - screenHeight; // Bắt đầu từ trên màn hình
        float length = MIN_RAIN_LENGTH + random.nextFloat() * (MAX_RAIN_LENGTH - MIN_RAIN_LENGTH);
        float speed = MIN_RAIN_SPEED + random.nextFloat() * (MAX_RAIN_SPEED - MIN_RAIN_SPEED);
        float angle = 80; // Góc rơi từ 70-90 độ
        // Tăng độ đậm alpha: từ 100-155 lên 150-255
        int alpha = 150 + random.nextInt(106);
        return new Raindrop(x, y, length, speed, angle, alpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isRaining) return;

        for (Raindrop raindrop : raindrops) {
            // Vẽ hạt mưa
            float endX = raindrop.x - (float) (Math.cos(Math.toRadians(raindrop.angle)) * raindrop.length);
            float endY = raindrop.y + (float) (Math.sin(Math.toRadians(raindrop.angle)) * raindrop.length);

            paint.setAlpha(raindrop.alpha);
            canvas.drawLine(raindrop.x, raindrop.y, endX, endY, paint);

            // Di chuyển hạt mưa
            raindrop.y += raindrop.speed;
            raindrop.x -= raindrop.speed * 0.15f; // Thêm chút di chuyển ngang để tạo cảm giác mưa nghiêng

            // Nếu hạt mưa rơi ra khỏi màn hình, tạo hạt mưa mới
            if (raindrop.y > screenHeight) {
                int index = raindrops.indexOf(raindrop);
                raindrops.set(index, createRandomRaindrop());
            }
        }

        // Vẽ lại liên tục
        invalidate();
    }

    public void startRain() {
        isRaining = true;
        invalidate();
    }

    public void stopRain() {
        isRaining = false;
    }

    public void setRainIntensity(int count) {
        createRaindrops(count);
    }

    // Phương thức mới để điều chỉnh độ đậm
    public void setRainOpacity(int baseAlpha) {
        paint.setColor(Color.argb(baseAlpha, 255, 255, 255)); // Màu trắng
        // Cập nhật lại tất cả hạt mưa
        for (Raindrop raindrop : raindrops) {
            raindrop.alpha = Math.max(150, Math.min(255, baseAlpha + random.nextInt(50)));
        }
    }

    // Lớp nội bộ để đại diện cho một hạt mưa
    private class Raindrop {
        float x, y;        // Vị trí
        float length;      // Chiều dài
        float speed;       // Tốc độ rơi
        float angle;       // Góc rơi (độ)
        int alpha;         // Độ trong suốt

        Raindrop(float x, float y, float length, float speed, float angle, int alpha) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.speed = speed;
            this.angle = angle;
            this.alpha = alpha;
        }
    }
}