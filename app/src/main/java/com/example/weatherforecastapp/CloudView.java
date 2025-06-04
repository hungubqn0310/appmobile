package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class CloudView extends View {
    private float screenWidth, screenHeight;
    private float cloudRadius;

    // Vị trí X của từng đám mây
    private float cloud1X, cloud2X, cloud3X, cloud4X, cloud5X;

    // Tốc độ di chuyển của từng đám mây
    private float cloud1Speed = 0.5f;
    private float cloud2Speed = 0.8f;
    private float cloud3Speed = 0.3f;
    private float cloud4Speed = 0.6f;
    private float cloud5Speed = 0.4f;

    // Kiểu mây (NORMAL, DARK, LIGHT)
    private CloudType cloudType = CloudType.NORMAL;

    public enum CloudType {
        NORMAL,  // Mây bình thường
        DARK,    // Mây tối (mưa, giông)
        LIGHT    // Mây sáng (nắng)
    }

    public CloudView(Context context) {
        super(context);
        init();
    }

    public CloudView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Không cần khởi tạo gì đặc biệt
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        cloudRadius = Math.min(w, h) * 0.15f;

        // Khởi tạo vị trí ban đầu của các đám mây
        cloud1X = screenWidth * 0.1f;
        cloud2X = screenWidth * 0.3f;
        cloud3X = screenWidth * 0.5f;
        cloud4X = screenWidth * 0.7f;
        cloud5X = screenWidth * 0.9f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Cập nhật vị trí các đám mây
        updateCloudPositions();

        // Vẽ các đám mây với các kiểu khác nhau
        drawDetailedCloud(canvas, cloud1X, screenHeight * 0.1f, cloudRadius * 0.8f, 0.9f, true);
        drawDetailedCloud(canvas, cloud2X, screenHeight * 0.15f, cloudRadius * 1.0f, 1.0f, false);
        drawDetailedCloud(canvas, cloud3X, screenHeight * 0.2f, cloudRadius * 0.9f, 0.85f, true);
        drawDetailedCloud(canvas, cloud4X, screenHeight * 0.25f, cloudRadius * 0.6f, 0.8f, false);
        drawDetailedCloud(canvas, cloud5X, screenHeight * 0.12f, cloudRadius * 0.7f, 0.95f, true);

        invalidate();
    }

    private void updateCloudPositions() {
        cloud1X += cloud1Speed;
        cloud2X += cloud2Speed;
        cloud3X += cloud3Speed;
        cloud4X += cloud4Speed;
        cloud5X += cloud5Speed;

        if (cloud1X > screenWidth + cloudRadius * 2) {
            cloud1X = -cloudRadius * 2;
        }
        if (cloud2X > screenWidth + cloudRadius * 2) {
            cloud2X = -cloudRadius * 2;
        }
        if (cloud3X > screenWidth + cloudRadius * 2) {
            cloud3X = -cloudRadius * 2;
        }
        if (cloud4X > screenWidth + cloudRadius * 2) {
            cloud4X = -cloudRadius * 2;
        }
        if (cloud5X > screenWidth + cloudRadius * 2) {
            cloud5X = -cloudRadius * 2;
        }
    }

    private void drawDetailedCloud(Canvas canvas, float centerX, float centerY, float radius, float alpha, boolean isDark) {
        float rMain = radius;
        float rSmall = radius * 0.6f;

        // Vẽ bóng đổ phía dưới mây
        Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        shadowPaint.setStyle(Paint.Style.FILL);

        // Bóng đậm hơn cho mây tối
        if (cloudType == CloudType.DARK) {
            shadowPaint.setColor(Color.argb(60, 0, 0, 0)); // Bóng đậm hơn
        } else {
            shadowPaint.setColor(Color.argb(30, 0, 0, 0)); // Bóng mờ đen
        }

        // Vẽ bóng (dịch xuống dưới và sang phải một chút)
        float shadowOffset = radius * 0.1f;
        canvas.drawCircle(centerX - rMain + shadowOffset, centerY + shadowOffset, rSmall, shadowPaint);
        canvas.drawCircle(centerX - rMain * 0.5f + shadowOffset, centerY - rSmall * 0.6f + shadowOffset, rMain * 0.75f, shadowPaint);
        canvas.drawCircle(centerX + shadowOffset, centerY - rSmall * 0.8f + shadowOffset, rMain, shadowPaint);
        canvas.drawCircle(centerX + rMain * 0.7f + shadowOffset, centerY - rSmall * 0.4f + shadowOffset, rMain * 0.8f, shadowPaint);
        canvas.drawCircle(centerX + rMain + shadowOffset, centerY + shadowOffset, rSmall * 0.9f, shadowPaint);

        // Vẽ phần chính của mây với gradient
        Paint cloudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cloudPaint.setStyle(Paint.Style.FILL);

        // Các phần khác nhau của mây với màu sắc khác nhau
        int alphaValue = (int)(alpha * 255);

        // Màu sắc thay đổi theo loại mây
        int topColor, midColor, bottomColor, darkBottomColor;

        switch (cloudType) {
            case DARK:
                // Mây tối cho mưa/giông
                topColor = Color.argb(alphaValue, 140, 140, 150);
                midColor = Color.argb(alphaValue, 120, 120, 130);
                bottomColor = Color.argb(alphaValue, 100, 100, 110);
                darkBottomColor = Color.argb(alphaValue, 80, 80, 90);
                break;
            case LIGHT:
                // Mây sáng cho nắng
                topColor = Color.argb(alphaValue, 255, 255, 255);
                midColor = Color.argb(alphaValue, 250, 250, 255);
                bottomColor = Color.argb(alphaValue, 240, 240, 245);
                darkBottomColor = Color.argb(alphaValue, 220, 220, 230);
                break;
            default:
                // Mây bình thường
                topColor = Color.argb(alphaValue, 255, 255, 255);
                midColor = Color.argb(alphaValue, 245, 245, 250);
                bottomColor = Color.argb(alphaValue, 235, 235, 240);
                darkBottomColor = Color.argb(alphaValue, 200, 200, 210);
                break;
        }

        // Phần sáng nhất (trên cùng)
        cloudPaint.setColor(topColor);
        canvas.drawCircle(centerX, centerY - rSmall * 0.8f, rMain, cloudPaint);

        // Phần trung gian
        cloudPaint.setColor(midColor);
        canvas.drawCircle(centerX - rMain * 0.5f, centerY - rSmall * 0.6f, rMain * 0.75f, cloudPaint);
        canvas.drawCircle(centerX + rMain * 0.7f, centerY - rSmall * 0.4f, rMain * 0.8f, cloudPaint);

        // Phần tối hơn (dưới cùng)
        cloudPaint.setColor(bottomColor);
        canvas.drawCircle(centerX - rMain, centerY, rSmall, cloudPaint);
        canvas.drawCircle(centerX + rMain, centerY, rSmall * 0.9f, cloudPaint);

        // Phần đáy mây (tối nhất)
        cloudPaint.setColor(darkBottomColor);
        canvas.drawCircle(centerX - rMain * 0.5f, centerY + rSmall * 0.5f, rSmall * 0.9f, cloudPaint);
        canvas.drawCircle(centerX + rMain * 0.2f, centerY + rSmall * 0.5f, rSmall, cloudPaint);

        // Thêm chi tiết nhỏ với gradient
        Paint detailPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        detailPaint.setStyle(Paint.Style.FILL);

        // Chi tiết nhỏ
        if (cloudType == CloudType.DARK) {
            detailPaint.setColor(Color.argb((int)(alphaValue * 0.5), 70, 70, 80));
        } else {
            detailPaint.setColor(Color.argb((int)(alphaValue * 0.5), 180, 180, 190));
        }
        canvas.drawCircle(centerX - rMain * 0.3f, centerY + rSmall * 0.2f, rSmall * 0.3f, detailPaint);
        canvas.drawCircle(centerX + rMain * 0.4f, centerY - rSmall * 0.1f, rSmall * 0.25f, detailPaint);

        // Highlight trên đỉnh mây (giảm cho mây tối)
        if (cloudType != CloudType.DARK) {
            Paint highlightPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            highlightPaint.setStyle(Paint.Style.FILL);
            highlightPaint.setShader(new RadialGradient(
                    centerX, centerY - rSmall * 0.8f,
                    rMain * 0.8f,
                    new int[]{
                            Color.argb((int)(alphaValue * 0.9), 255, 255, 255),
                            Color.argb((int)(alphaValue * 0.3), 255, 255, 255)
                    },
                    null,
                    Shader.TileMode.CLAMP
            ));
            canvas.drawCircle(centerX, centerY - rSmall * 0.8f, rMain * 0.6f, highlightPaint);
        }
    }

    public void setCloudSpeed(float speedMultiplier) {
        cloud1Speed = 0.5f * speedMultiplier;
        cloud2Speed = 0.8f * speedMultiplier;
        cloud3Speed = 0.3f * speedMultiplier;
        cloud4Speed = 0.6f * speedMultiplier;
        cloud5Speed = 0.4f * speedMultiplier;
    }

    // Phương thức mới để set loại mây
    public void setCloudType(CloudType type) {
        this.cloudType = type;
        invalidate();
    }
}