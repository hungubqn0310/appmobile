package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;
import java.util.Locale;

public class CompassView extends View {
    private Paint textPaint, arrowPaint, linePaint;
    private float centerX, centerY, radius;
    private float windDirection = 0f; // Góc hướng gió (0-360)
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
        textPaint.setTypeface(Typeface.DEFAULT);

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
        // Mũi tên chỉ hướng GIÓ THỔI ĐẾN (wind direction), không phải hướng gió thổi đi
        // Vì vậy chúng ta cần xoay thêm 180 độ
        canvas.rotate(windDirection + 180, centerX, centerY);

        // Tính toán khoảng cách để tránh đè lên text
        float textAreaRadius = dpToPx(20); // Khoảng trống cho text ở giữa

        // === VẼ PHẦN ĐẦU MŨI TÊN (TRÊN) ===
        // Thân mũi tên phía trên - dày hơn và đẹp hơn
        arrowPaint.setStrokeWidth(dpToPx(3));
        arrowPaint.setStyle(Paint.Style.STROKE);
        canvas.drawLine(centerX, centerY - dpToPx(45),
                centerX, centerY - textAreaRadius, arrowPaint);

        // Đầu mũi tên tam giác đầy - lớn và đẹp hơn
        Path arrowHead = new Path();
        arrowHead.moveTo(centerX, centerY - dpToPx(45));
        arrowHead.lineTo(centerX - dpToPx(8), centerY - dpToPx(30));
        arrowHead.lineTo(centerX + dpToPx(8), centerY - dpToPx(30));
        arrowHead.close();

        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowHead, arrowPaint);

        // === VẼ PHẦN ĐUÔI MŨI TÊN (DƯỚI) ===
        // Thân mũi tên phía dưới
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(dpToPx(3));
        canvas.drawLine(centerX, centerY + textAreaRadius,
                centerX, centerY + dpToPx(35), arrowPaint);





        canvas.restore();
    }

    private void drawWindSpeed(Canvas canvas) {
        textPaint.setTextSize(dpToPx(12));
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setColor(Color.BLACK);

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

    // Phương thức để cập nhật hướng và tốc độ gió từ API
    public void setWindData(float speedKph, String windDirectionStr) {
        // Chuyển đổi hướng gió từ string sang degrees
        this.windDirection = convertWindDirectionToDegrees(windDirectionStr);

        // Format tốc độ gió
        this.windSpeed = String.format(Locale.getDefault(), "%.0f\nkm/h", speedKph);

        invalidate(); // Vẽ lại view
    }

    // Phương thức chuyển đổi hướng gió từ string (N, NE, E, etc.) sang degrees
    private float convertWindDirectionToDegrees(String direction) {
        if (direction == null || direction.isEmpty()) {
            return 0f;
        }

        // API trả về hướng gió thổi ĐẾN (wind direction)
        switch (direction.toUpperCase().trim()) {
            case "N": case "BẮC": return 0f;
            case "NNE": case "BĐB": return 22.5f;
            case "NE": case "ĐB": case "ĐÔNG BẮC": return 45f;
            case "ENE": return 67.5f;
            case "E": case "Đ": case "ĐÔNG": return 90f;
            case "ESE": return 112.5f;
            case "SE": case "ĐN": case "ĐÔNG NAM": return 135f;
            case "SSE": case "ĐNĐ": return 157.5f;
            case "S": case "NAM": return 180f;
            case "SSW": case "NTN": return 202.5f;
            case "SW": case "TN": case "TÂY NAM": return 225f;
            case "WSW": return 247.5f;
            case "W": case "T": case "TÂY": return 270f;
            case "WNW": case "TBT": return 292.5f;
            case "NW": case "TB": case "TÂY BẮC": return 315f;
            case "NNW": return 337.5f;
            default:
                // Nếu không nhận diện được, thử parse như số
                try {
                    return Float.parseFloat(direction);
                } catch (NumberFormatException e) {
                    return 0f;
                }
        }
    }

    // Phương thức backup nếu bạn muốn set trực tiếp bằng degrees
    public void setWindDataDegrees(float speedKph, float directionDegrees) {
        this.windDirection = directionDegrees;
        this.windSpeed = String.format(Locale.getDefault(), "%.0f\nkm/h", speedKph);
        invalidate();
    }
}