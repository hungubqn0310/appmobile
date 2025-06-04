package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
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
    private Paint rainPaint;
    private Random random;
    private int screenWidth;
    private int screenHeight;
    private boolean isRaining = true;

    private Paint lightningPaint;
    private Paint flashPaint;
    private ArrayList<Lightning> lightnings;
    private long lastLightningTime;
    private int lightningFrequency = 3000;
    private int flashAlpha = 0;
    private Handler handler;
    private boolean thunderEnabled = true;

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
        handler = new Handler(Looper.getMainLooper());
        lightnings = new ArrayList<>();
        lastLightningTime = System.currentTimeMillis();

        rainPaint = new Paint();
        rainPaint.setColor(Color.argb(180, 200, 200, 255));
        rainPaint.setStrokeWidth(2);
        rainPaint.setStyle(Paint.Style.STROKE);
        rainPaint.setStrokeCap(Paint.Cap.ROUND);
        rainPaint.setAntiAlias(true);

        lightningPaint = new Paint();
        lightningPaint.setAntiAlias(true);
        lightningPaint.setStyle(Paint.Style.STROKE);
        lightningPaint.setStrokeCap(Paint.Cap.ROUND);

        flashPaint = new Paint();
        flashPaint.setColor(Color.WHITE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;

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
        float y = random.nextFloat() * screenHeight - screenHeight;
        float length = MIN_RAIN_LENGTH + random.nextFloat() * (MAX_RAIN_LENGTH - MIN_RAIN_LENGTH);
        float speed = MIN_RAIN_SPEED + random.nextFloat() * (MAX_RAIN_SPEED - MIN_RAIN_SPEED);
        float angle = 80;
        int alpha = 100 + random.nextInt(155);
        return new Raindrop(x, y, length, speed, angle, alpha);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isRaining) return;

        for (Raindrop raindrop : raindrops) {
            float endX = raindrop.x - (float) (Math.cos(Math.toRadians(raindrop.angle)) * raindrop.length);
            float endY = raindrop.y + (float) (Math.sin(Math.toRadians(raindrop.angle)) * raindrop.length);

            rainPaint.setAlpha(raindrop.alpha);
            canvas.drawLine(raindrop.x, raindrop.y, endX, endY, rainPaint);

            raindrop.y += raindrop.speed;
            raindrop.x -= raindrop.speed * 0.15f;

            if (raindrop.y > screenHeight) {
                int index = raindrops.indexOf(raindrop);
                raindrops.set(index, createRandomRaindrop());
            }
        }

        if (thunderEnabled) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastLightningTime > lightningFrequency) {
                createLightning();
                lastLightningTime = currentTime;
                triggerFlash();
                lightningFrequency = 2000 + random.nextInt(4000);
            }

            if (flashAlpha > 0) {
                flashPaint.setAlpha(flashAlpha);
                canvas.drawRect(0, 0, screenWidth, screenHeight, flashPaint);
                flashAlpha = Math.max(0, flashAlpha - 15);
            }

            ArrayList<Lightning> toRemove = new ArrayList<>();
            for (Lightning lightning : lightnings) {
                lightning.update();
                if (lightning.isExpired()) {
                    toRemove.add(lightning);
                } else {
                    drawLightning(canvas, lightning);
                }
            }
            lightnings.removeAll(toRemove);
        }

        invalidate();
    }

    private void createLightning() {
        float startX = screenWidth * 0.2f + random.nextFloat() * screenWidth * 0.6f;
        float startY = -50;

        Lightning lightning = new Lightning(startX, startY);
        lightnings.add(lightning);

        if (random.nextFloat() > 0.6f) {
            handler.postDelayed(() -> {
                float secondX = startX + (random.nextFloat() - 0.5f) * 200;
                lightnings.add(new Lightning(secondX, startY));
            }, 100 + random.nextInt(200));
        }
    }

    private void drawLightning(Canvas canvas, Lightning lightning) {
        lightningPaint.setStrokeWidth(lightning.thickness * 4);
        lightningPaint.setColor(Color.argb(lightning.alpha / 4, 150, 150, 255));
        lightningPaint.setMaskFilter(new android.graphics.BlurMaskFilter(20, android.graphics.BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(lightning.path, lightningPaint);

        lightningPaint.setStrokeWidth(lightning.thickness * 2);
        lightningPaint.setColor(Color.argb(lightning.alpha / 2, 200, 200, 255));
        lightningPaint.setMaskFilter(new android.graphics.BlurMaskFilter(10, android.graphics.BlurMaskFilter.Blur.NORMAL));
        canvas.drawPath(lightning.path, lightningPaint);

        lightningPaint.setStrokeWidth(lightning.thickness);
        lightningPaint.setColor(Color.argb(lightning.alpha, 255, 255, 255));
        lightningPaint.setMaskFilter(null);
        canvas.drawPath(lightning.path, lightningPaint);

        for (Path branch : lightning.branches) {
            lightningPaint.setStrokeWidth(lightning.thickness * 0.6f);
            lightningPaint.setColor(Color.argb(lightning.alpha * 3 / 4, 220, 220, 255));
            canvas.drawPath(branch, lightningPaint);
        }
    }

    private void triggerFlash() {
        flashAlpha = 80 + random.nextInt(80);
        if (random.nextFloat() > 0.5f) {
            handler.postDelayed(() -> {
                flashAlpha = 60 + random.nextInt(60);
            }, 100);
        }
    }

    public void startRain() {
        isRaining = true;
        lastLightningTime = System.currentTimeMillis();
        invalidate();
    }

    public void stopRain() {
        isRaining = false;
        lightnings.clear();
        flashAlpha = 0;
    }

    public void setRainIntensity(int count) {
        createRaindrops(count);

        if (thunderEnabled) {
            if (count < 100) {
                lightningFrequency = 6000;
            } else if (count < 200) {
                lightningFrequency = 3000;
            } else {
                lightningFrequency = 1500;
            }
        }
    }

    public void setThunderEnabled(boolean enabled) {
        this.thunderEnabled = enabled;
        if (!enabled) {
            lightnings.clear();
            flashAlpha = 0;
        }
    }

    private class Raindrop {
        float x, y;
        float length;
        float speed;
        float angle;
        int alpha;

        Raindrop(float x, float y, float length, float speed, float angle, int alpha) {
            this.x = x;
            this.y = y;
            this.length = length;
            this.speed = speed;
            this.angle = angle;
            this.alpha = alpha;
        }
    }

    private class Lightning {
        Path path;
        float startX, startY;
        float endX, endY;
        int segments;
        long creationTime;
        int alpha;
        float thickness;
        boolean isBranching;
        ArrayList<Path> branches;

        Lightning(float startX, float startY) {
            this.startX = startX;
            this.startY = startY;
            this.endY = screenHeight * 0.7f + random.nextFloat() * screenHeight * 0.3f;
            this.segments = 5 + random.nextInt(10);
            this.creationTime = System.currentTimeMillis();
            this.alpha = 255;
            this.thickness = 2 + random.nextFloat() * 4;
            this.isBranching = random.nextFloat() > 0.5f;
            this.branches = new ArrayList<>();
            generatePath();
        }

        void generatePath() {
            path = new Path();
            path.moveTo(startX, startY);

            float currentX = startX;
            float currentY = startY;
            float segmentHeight = (endY - startY) / segments;

            for (int i = 1; i <= segments; i++) {
                float offsetX = (random.nextFloat() - 0.5f) * 100;
                currentX += offsetX;
                currentY += segmentHeight;

                path.lineTo(currentX, currentY);

                if (isBranching && i > segments / 3 && random.nextFloat() > 0.7f) {
                    Path branch = new Path();
                    branch.moveTo(currentX, currentY);

                    float branchEndX = currentX + (random.nextFloat() - 0.5f) * 150;
                    float branchEndY = currentY + random.nextFloat() * 100;
                    int branchSegments = 2 + random.nextInt(3);

                    float branchX = currentX;
                    float branchY = currentY;
                    float branchSegmentHeight = (branchEndY - branchY) / branchSegments;

                    for (int j = 1; j <= branchSegments; j++) {
                        branchX += (random.nextFloat() - 0.5f) * 50;
                        branchY += branchSegmentHeight;
                        branch.lineTo(branchX, branchY);
                    }

                    branches.add(branch);
                }
            }

            endX = currentX;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - creationTime > 500;
        }

        void update() {
            long elapsed = System.currentTimeMillis() - creationTime;
            if (elapsed < 100) {
                alpha = 255;
            } else if (elapsed < 300) {
                alpha = (int)(255 * (1 - (elapsed - 100) / 200f));
            } else {
                alpha = (int)(255 * 0.3f * (1 - (elapsed - 300) / 200f));
            }
        }
    }
}
