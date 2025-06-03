package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class PressureGaugeView extends View implements ThemeAware {
    private Paint arcPaint, textPaint, labelPaint, indicatorPaint;
    private RectF arcRect;
    private float centerX, centerY, radius;
    private float pressure = 1013f;
    private float minPressure = 980f;
    private float maxPressure = 1040f;
    private boolean showPlaceholder = true;
    private boolean isDarkTheme = false;

    public PressureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        initPaints();
        arcRect = new RectF();
    }

    private void initPaints() {
        int primaryColor = isDarkTheme ? Color.WHITE : Color.BLACK;
        int secondaryColor = isDarkTheme ? Color.parseColor("#999999") : Color.parseColor("#666666");

        // Paint cho vòng cung áp suất
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(primaryColor);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dpToPx(3));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho text áp suất
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(primaryColor);
        textPaint.setTextSize(dpToPx(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT);

        // Paint cho label (Thấp/Cao)
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(primaryColor);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Paint cho kim chỉ thị
        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setColor(primaryColor);
        indicatorPaint.setStrokeWidth(dpToPx(2));
        indicatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);
    }

    @Override
    public void applyTheme(boolean isDarkTheme) {
        this.isDarkTheme = isDarkTheme;
        initPaints();
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2f;
        centerY = h / 2f;
        radius = Math.min(w, h) / 2f - dpToPx(8);

        float arcRadius = radius - dpToPx(8);
        arcRect.set(centerX - arcRadius, centerY - arcRadius,
                centerX + arcRadius, centerY + arcRadius);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawPressureArc(canvas);
        drawTickMarks(canvas);

        if (!showPlaceholder) {
            drawIndicator(canvas);
        }

        drawPressureText(canvas);
        drawUpArrow(canvas);
        drawLabels(canvas);
    }

    private void drawPressureArc(Canvas canvas) {
        float startAngle = 135f;
        float sweepAngle = 270f;

        // Vẽ vòng cung nền
        Paint backgroundArcPaint = new Paint(arcPaint);
        backgroundArcPaint.setColor(isDarkTheme ? Color.parseColor("#999999") : Color.parseColor("#666666"));
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, backgroundArcPaint);

        // Vẽ vòng cung hiện tại
        if (!showPlaceholder) {
            float pressureRatio = (pressure - minPressure) / (maxPressure - minPressure);
            pressureRatio = Math.max(0f, Math.min(1f, pressureRatio));
            float currentSweep = sweepAngle * pressureRatio;

            Paint currentArcPaint = new Paint(arcPaint);
            currentArcPaint.setColor(isDarkTheme ? Color.WHITE : Color.BLACK);
            currentArcPaint.setStrokeWidth(dpToPx(2));
            canvas.drawArc(arcRect, startAngle, currentSweep, false, currentArcPaint);
        }
    }

    private void drawTickMarks(Canvas canvas) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        int tickCount = 20;

        Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tickPaint.setColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        tickPaint.setStrokeWidth(dpToPx(1));

        for (int i = 0; i <= tickCount; i++) {
            float angle = startAngle + (sweepAngle * i / tickCount);
            float radian = (float) Math.toRadians(angle);

            float tickLength = (i % 5 == 0) ? dpToPx(6) : dpToPx(3);
            float innerRadius = radius - dpToPx(15);
            float outerRadius = innerRadius + tickLength;

            float startX = centerX + innerRadius * (float) Math.cos(radian);
            float startY = centerY + innerRadius * (float) Math.sin(radian);
            float endX = centerX + outerRadius * (float) Math.cos(radian);
            float endY = centerY + outerRadius * (float) Math.sin(radian);

            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }
    }

    private void drawIndicator(Canvas canvas) {
        float pressureRatio = (pressure - minPressure) / (maxPressure - minPressure);
        pressureRatio = Math.max(0f, Math.min(1f, pressureRatio));
        float angle = 135f + (270f * pressureRatio);
        float radian = (float) Math.toRadians(angle);

        float indicatorRadius = radius - dpToPx(12);
        float endX = centerX + indicatorRadius * (float) Math.cos(radian);
        float endY = centerY + indicatorRadius * (float) Math.sin(radian);

        canvas.drawLine(centerX, centerY, endX, endY, indicatorPaint);
        canvas.drawCircle(centerX, centerY, dpToPx(3), indicatorPaint);
    }

    private void drawPressureText(Canvas canvas) {
        String pressureText = showPlaceholder ? "--" : String.format("%.0f", pressure);
        canvas.drawText(pressureText, centerX, centerY - dpToPx(-35), textPaint);

        textPaint.setTextSize(dpToPx(10));
        canvas.drawText("hPa", centerX, centerY + dpToPx(48), textPaint);
        textPaint.setTextSize(dpToPx(14));
    }

    private void drawLabels(Canvas canvas) {
        float bottomY = centerY + radius - dpToPx(5);
        canvas.drawText("Thấp", centerX - dpToPx(30), bottomY, labelPaint);
        canvas.drawText("Cao", centerX + dpToPx(30), bottomY, labelPaint);
    }

    private void drawUpArrow(Canvas canvas) {
        float arrowY = centerY - dpToPx(-15);

        Path arrow = new Path();
        arrow.moveTo(centerX, arrowY - dpToPx(5));
        arrow.lineTo(centerX - dpToPx(3.5f), arrowY);
        arrow.lineTo(centerX - dpToPx(1.5f), arrowY);
        arrow.lineTo(centerX - dpToPx(1.5f), arrowY + dpToPx(5));
        arrow.lineTo(centerX + dpToPx(1.5f), arrowY + dpToPx(5));
        arrow.lineTo(centerX + dpToPx(1.5f), arrowY);
        arrow.lineTo(centerX + dpToPx(3.5f), arrowY);
        arrow.close();

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(isDarkTheme ? Color.WHITE : Color.BLACK);
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrow, arrowPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
        this.showPlaceholder = false;
        invalidate();
    }

    public void setPressureRange(float min, float max) {
        this.minPressure = min;
        this.maxPressure = max;
        invalidate();
    }

    public void showPlaceholder() {
        this.showPlaceholder = true;
        invalidate();
    }

    public void hidePlaceholder() {
        this.showPlaceholder = false;
        invalidate();
    }

    public boolean isShowingPlaceholder() {
        return showPlaceholder;
    }

    public float getPressure() {
        return pressure;
    }
}