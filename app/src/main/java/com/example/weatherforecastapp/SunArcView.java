package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SunArcView extends View {
    private Paint arcPaint;
    private Paint sunPaint;
    private Paint grayArcPaint;
    private RectF arcRect;
    private float sunPosition = 0.7f;

    public SunArcView(Context context) {
        super(context);
        init();
    }

    public SunArcView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SunArcView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Paint cho đường cung màu vàng
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(0xFFFFA500);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(6f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho đường cung màu xám
        grayArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        grayArcPaint.setColor(0x66FFFFFF); // Màu trắng trong suốt
        grayArcPaint.setStyle(Paint.Style.STROKE);
        grayArcPaint.setStrokeWidth(6f);
        grayArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho điểm mặt trời
        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(0xFFFFA500);
        sunPaint.setStyle(Paint.Style.FILL);

        arcRect = new RectF();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Tạo hình chữ nhật cho đường cung
        float padding = 30f;
        float diameter = Math.min(w - padding * 2, (h - padding) * 2);

        float left = (w - diameter) / 2f;
        float top = h - diameter / 2f - padding / 2f;
        float right = left + diameter;
        float bottom = top + diameter;

        arcRect.set(left, top, right, bottom);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (arcRect.isEmpty()) return;

        // Vẽ đường cung màu xám (toàn bộ)
        canvas.drawArc(arcRect, 180, 180, false, grayArcPaint);

        // Vẽ đường cung màu vàng (phần đã đi qua)
        float sweepAngle = 180 * sunPosition;
        canvas.drawArc(arcRect, 180, sweepAngle, false, arcPaint);

        // Tính toán vị trí mặt trời
        float centerX = arcRect.centerX();
        float centerY = arcRect.centerY();
        float radius = arcRect.width() / 2f;

        // Góc của mặt trời (180 độ = trái, 0 độ = phải)
        double angleInRadians = Math.toRadians(180 + sweepAngle);
        float sunX = centerX + (float)(radius * Math.cos(angleInRadians));
        float sunY = centerY + (float)(radius * Math.sin(angleInRadians));

        // Vẽ điểm mặt trời
        canvas.drawCircle(sunX, sunY, 8f, sunPaint);
    }

    public void setSunPosition(float position) {
        this.sunPosition = Math.max(0f, Math.min(1f, position));
        invalidate();
    }
}