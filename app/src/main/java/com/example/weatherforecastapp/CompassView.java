package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class CompassView extends View {
    private Paint textPaint, arrowPaint, linePaint;
    private float centerX, centerY, radius;
    private float windDirection = 45f; // Góc hướng gió (0-360)
    private String windSpeed = "--\nkm/h";

    public CompassView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // Paint cho text
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dpToPx(12));
        textPaint.setTextAlign(Paint.Align.CENTER);

        // Paint cho mũi tên
        arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.BLACK);
        arrowPaint.setStrokeWidth(dpToPx(2));
        arrowPaint.setStyle(Paint.Style.STROKE);

        // Paint cho các vạch chia
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.BLACK);
        linePaint.setStrokeWidth(dpToPx(1));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f - dpToPx(4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Vẽ các hướng chính (N, S, E, W)
        drawDirections(canvas);

        // Vẽ các vạch chia độ
        drawTickMarks(canvas);

        // Vẽ mũi tên hướng gió (vẽ trước text để text đè lên)
        drawWindArrow(canvas);

        // Vẽ text tốc độ gió
        drawWindSpeed(canvas);
    }

    private void drawDirections(Canvas canvas) {
        String[] directions = {"N", "E", "S", "W"};
        float[] angles = {0, 90, 180, 270};

        textPaint.setTextSize(dpToPx(14));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        for (int i = 0; i < directions.length; i++) {
            float angle = (float) Math.toRadians(angles[i] - 90);
            float x = centerX + (radius - dpToPx(15)) * (float) Math.cos(angle);
            float y = centerY + (radius - dpToPx(15)) * (float) Math.sin(angle) + dpToPx(5);
            canvas.drawText(directions[i], x, y, textPaint);
        }
    }

    private void drawTickMarks(Canvas canvas) {
        for (int i = 0; i < 360; i += 30) {
            if (i % 90 != 0) { // Không vẽ vạch tại vị trí N,S,E,W
                float angle = (float) Math.toRadians(i - 90);
                float startX = centerX + (radius - dpToPx(10)) * (float) Math.cos(angle);
                float startY = centerY + (radius - dpToPx(10)) * (float) Math.sin(angle);
                float endX = centerX + (radius - dpToPx(5)) * (float) Math.cos(angle);
                float endY = centerY + (radius - dpToPx(5)) * (float) Math.sin(angle);
                canvas.drawLine(startX, startY, endX, endY, linePaint);
            }
        }
    }

    private void drawWindArrow(Canvas canvas) {
        canvas.save();
        canvas.rotate(windDirection, centerX, centerY);

        // Vẽ phần trên của mũi tên (từ đầu mũi tên đến gần giữa)
        canvas.drawLine(centerX, centerY - dpToPx(50),
                centerX, centerY - dpToPx(24), arrowPaint);

        // Vẽ phần dưới của mũi tên (từ gần giữa đến cuối)
        canvas.drawLine(centerX, centerY + dpToPx(24),
                centerX, centerY + dpToPx(50), arrowPaint);

        // Vẽ đầu mũi tên
        Path arrowHead = new Path();
        arrowHead.moveTo(centerX, centerY - dpToPx(50));
        arrowHead.lineTo(centerX - dpToPx(5), centerY - dpToPx(45));
        arrowHead.lineTo(centerX + dpToPx(5), centerY - dpToPx(45));
        arrowHead.close();

        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowHead, arrowPaint);
        arrowPaint.setStyle(Paint.Style.STROKE);

        // Vẽ đuôi mũi tên (tùy chọn)
        Path arrowTail = new Path();
        arrowTail.moveTo(centerX - dpToPx(3), centerY + dpToPx(50));
        arrowTail.lineTo(centerX, centerY + dpToPx(45));
        arrowTail.lineTo(centerX + dpToPx(3), centerY + dpToPx(50));

        arrowPaint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(arrowTail, arrowPaint);

        canvas.restore();
    }

    private void drawWindSpeed(Canvas canvas) {
        textPaint.setTextSize(dpToPx(12));
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        String[] lines = windSpeed.split("\n");
        float lineHeight = textPaint.getTextSize() + dpToPx(2);
        float totalHeight = lines.length * lineHeight;
        float startY = centerY - totalHeight / 2 + lineHeight / 2;

        for (int i = 0; i < lines.length; i++) {
            canvas.drawText(lines[i], centerX, startY + i * lineHeight, textPaint);
        }
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    // Phương thức để cập nhật hướng và tốc độ gió
    public void setWindData(float direction, String speed) {
        this.windDirection = direction;
        this.windSpeed = speed;
        invalidate(); // Vẽ lại view
    }
}