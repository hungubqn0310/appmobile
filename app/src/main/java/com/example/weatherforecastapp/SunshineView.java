package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class SunshineView extends View {
    private float screenWidth, screenHeight;
    private float centerX, centerY;
    private float sunRadius;

    public SunshineView(Context context) {
        super(context);
        init();
    }

    public SunshineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Không cần khởi tạo gì thêm
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        screenWidth = w;
        screenHeight = h;
        centerX = w / 2f;
        centerY = h / 2f;
        sunRadius = Math.min(w, h) * 0.15f;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Vẽ sun flare (ánh sáng mặt trời) vàng đậm ở giữa trên
        Paint flarePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        flarePaint.setStyle(Paint.Style.FILL);

        // Vòng tròn lớn, vàng nhạt
        flarePaint.setColor(Color.argb(60, 255, 230, 120)); // vàng nhạt
        canvas.drawCircle(centerX, -sunRadius * 1.5f, sunRadius * 3.5f, flarePaint);

        // Vòng tròn nhỏ hơn, cam nhạt hơn
        flarePaint.setColor(Color.argb(40, 255, 200, 60)); // cam nhạt hơn, alpha thấp hơn
        canvas.drawCircle(centerX, -sunRadius * 1.5f, sunRadius * 1.8f, flarePaint);

        // Vòng tròn nhỏ, vàng đậm
        flarePaint.setColor(Color.argb(30, 255, 255, 120)); // vàng sáng
        canvas.drawCircle(centerX, -sunRadius * 1.5f, sunRadius * 0.7f, flarePaint);

        // Vẽ các tia nắng to dần từ sun flare ra ngoài, màu nhạt hơn và lắc lư nhẹ
        long time = System.currentTimeMillis();
        float animPhase = (time % 10000) / 10000f * 2f * (float)Math.PI; // chu kỳ 4s

        int rayCount = 14;
        float flareCenterX = centerX;
        float flareCenterY = -sunRadius * 1.5f;
        float rayStart = sunRadius * 1.0f;
        float rayEnd = (float) Math.hypot(Math.max(flareCenterX, screenWidth - flareCenterX), Math.max(flareCenterY, screenHeight - flareCenterY)) + sunRadius * 1.5f;
        float rayWidthStart = sunRadius * 0.10f;
        float rayWidthEnd = sunRadius * 0.70f; // To dần

        Paint rayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        rayPaint.setStyle(Paint.Style.FILL);
        rayPaint.setColor(Color.argb(40, 255, 240, 180)); // vàng nhạt, mờ hơn

        for (int i = 0; i < rayCount; i++) {
            float baseAngle = (float) (i * (360.0f / rayCount));
            // Lắc lư: offset góc cho từng tia
            float swing = (float) Math.sin(animPhase + i * 0.5f) * 3f; // lắc lư tối đa 7 độ
            float angle = baseAngle + swing;
            double rad = Math.toRadians(angle);

            // Tâm tia
            float startX = flareCenterX + (float) Math.cos(rad) * rayStart;
            float startY = flareCenterY + (float) Math.sin(rad) * rayStart;
            float endX = flareCenterX + (float) Math.cos(rad) * rayEnd;
            float endY = flareCenterY + (float) Math.sin(rad) * rayEnd;

            // Góc vuông góc với tia để tạo độ rộng
            double perpRad = rad + Math.PI / 2;

            // Gốc nhỏ
            float sx1 = startX + (float) Math.cos(perpRad) * (rayWidthStart / 2);
            float sy1 = startY + (float) Math.sin(perpRad) * (rayWidthStart / 2);
            float sx2 = startX - (float) Math.cos(perpRad) * (rayWidthStart / 2);
            float sy2 = startY - (float) Math.sin(perpRad) * (rayWidthStart / 2);

            // Ngọn to
            float ex1 = endX + (float) Math.cos(perpRad) * (rayWidthEnd / 2);
            float ey1 = endY + (float) Math.sin(perpRad) * (rayWidthEnd / 2);
            float ex2 = endX - (float) Math.cos(perpRad) * (rayWidthEnd / 2);
            float ey2 = endY - (float) Math.sin(perpRad) * (rayWidthEnd / 2);

            android.graphics.Path path = new android.graphics.Path();
            path.moveTo(sx1, sy1);
            path.lineTo(ex1, ey1);
            path.lineTo(ex2, ey2);
            path.lineTo(sx2, sy2);
            path.close();

            canvas.drawPath(path, rayPaint);
        }

        // Gọi lại invalidate để animation lắc lư liên tục
        invalidate();
    }
}