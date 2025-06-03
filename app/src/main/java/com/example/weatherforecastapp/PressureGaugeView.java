package com.example.weatherforecastapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class PressureGaugeView extends View {
    private Paint arcPaint, textPaint, labelPaint, indicatorPaint;
    private RectF arcRect;
    private float centerX, centerY, radius;
    private float pressure = 1013f; // Giá trị áp suất mặc định (áp suất tiêu chuẩn)
    private float minPressure = 980f; // Áp suất tối thiểu
    private float maxPressure = 1040f; // Áp suất tối đa
    private boolean showPlaceholder = true; // true = hiển thị "--", false = hiển thị giá trị thực

    // Language settings
    private static final String SETTINGS_PREFS = "SettingsPrefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String LANG_VIETNAMESE = "vi";
    private static final String LANG_ENGLISH = "en";
    private String currentLanguage = LANG_VIETNAMESE;

    public PressureGaugeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Lấy cài đặt ngôn ngữ từ SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences(SETTINGS_PREFS, Context.MODE_PRIVATE);
        currentLanguage = prefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE);
        init();
    }

    private void init() {
        // Paint cho vòng cung áp suất
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(Color.BLACK);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(dpToPx(3));
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho text áp suất
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(dpToPx(14));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Paint cho label (Thấp/Cao)
        labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        labelPaint.setColor(Color.BLACK);
        labelPaint.setTextSize(dpToPx(10));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        // Paint cho kim chỉ thị
        indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        indicatorPaint.setColor(Color.BLACK);
        indicatorPaint.setStrokeWidth(dpToPx(2));
        indicatorPaint.setStyle(Paint.Style.FILL_AND_STROKE);

        arcRect = new RectF();
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

        // Vẽ vòng cung áp suất
        drawPressureArc(canvas);

        // Vẽ các vạch chia
        drawTickMarks(canvas);

        // Vẽ kim chỉ thị (chỉ vẽ khi không phải placeholder)
        if (!showPlaceholder) {
            drawIndicator(canvas);
        }

        // Vẽ text áp suất
        drawPressureText(canvas);

        // Vẽ mũi tên hướng lên
        drawUpArrow(canvas);

        // Vẽ label Thấp/Cao (vẽ cuối cùng để ở dưới cùng)
        drawLabels(canvas);
    }

    private void drawPressureArc(Canvas canvas) {
        // Vẽ vòng cung nền (màu đen nhạt)
        float startAngle = 135f; // Bắt đầu từ góc 135 độ
        float sweepAngle = 270f; // Quét 270 độ

        Paint backgroundArcPaint = new Paint(arcPaint);
        backgroundArcPaint.setColor(Color.parseColor("#666666"));
        canvas.drawArc(arcRect, startAngle, sweepAngle, false, backgroundArcPaint);

        // Vẽ vòng cung hiện tại (màu đen) - chỉ vẽ khi có dữ liệu thực
        if (!showPlaceholder) {
            float pressureRatio = (pressure - minPressure) / (maxPressure - minPressure);
            // Đảm bảo ratio nằm trong khoảng 0-1
            pressureRatio = Math.max(0f, Math.min(1f, pressureRatio));
            float currentSweep = sweepAngle * pressureRatio;

            Paint currentArcPaint = new Paint(arcPaint);
            currentArcPaint.setColor(Color.BLACK);
            currentArcPaint.setStrokeWidth(dpToPx(2));
            canvas.drawArc(arcRect, startAngle, currentSweep, false, currentArcPaint);
        }
    }

    private void drawTickMarks(Canvas canvas) {
        float startAngle = 135f;
        float sweepAngle = 270f;
        int tickCount = 20;

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

            Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            tickPaint.setColor(Color.BLACK);
            tickPaint.setStrokeWidth(dpToPx(1));
            canvas.drawLine(startX, startY, endX, endY, tickPaint);
        }
    }

    private void drawIndicator(Canvas canvas) {
        float pressureRatio = (pressure - minPressure) / (maxPressure - minPressure);
        // Đảm bảo ratio nằm trong khoảng 0-1
        pressureRatio = Math.max(0f, Math.min(1f, pressureRatio));
        float angle = 135f + (270f * pressureRatio);
        float radian = (float) Math.toRadians(angle);

        float indicatorRadius = radius - dpToPx(12);
        float endX = centerX + indicatorRadius * (float) Math.cos(radian);
        float endY = centerY + indicatorRadius * (float) Math.sin(radian);

        // Vẽ kim chỉ thị
        canvas.drawLine(centerX, centerY, endX, endY, indicatorPaint);

        // Vẽ điểm tròn ở giữa
        canvas.drawCircle(centerX, centerY, dpToPx(3), indicatorPaint);
    }

    private void drawPressureText(Canvas canvas) {
        // Vẽ giá trị áp suất hoặc placeholder
        String pressureText = showPlaceholder ? "--" : String.format("%.0f", pressure);
        canvas.drawText(pressureText, centerX, centerY - dpToPx(-35), textPaint);

        // Vẽ đơn vị hPa
        textPaint.setTextSize(dpToPx(10));
        canvas.drawText("hPa", centerX, centerY + dpToPx(48), textPaint);
        textPaint.setTextSize(dpToPx(14)); // Reset lại size
    }

    private void drawLabels(Canvas canvas) {
        // Label "Thấp"/"Low" và "Cao"/"High" ở dưới cùng
        float bottomY = centerY + radius - dpToPx(5);

        // Chọn nhãn phù hợp với ngôn ngữ
        String lowLabel = currentLanguage.equals(LANG_ENGLISH) ? "Low" : "Thấp";
        String highLabel = currentLanguage.equals(LANG_ENGLISH) ? "High" : "Cao";

        // Label "Thấp"/"Low" bên trái
        canvas.drawText(lowLabel, centerX - dpToPx(30), bottomY, labelPaint);

        // Label "Cao"/"High" bên phải
        canvas.drawText(highLabel, centerX + dpToPx(30), bottomY, labelPaint);
    }

    private void drawUpArrow(Canvas canvas) {
        float arrowY = centerY - dpToPx(-15);

        Path arrow = new Path();
        // Đầu mũi tên
        arrow.moveTo(centerX, arrowY - dpToPx(5));
        arrow.lineTo(centerX - dpToPx(3.5f), arrowY);
        arrow.lineTo(centerX - dpToPx(1.5f), arrowY);

        // Thân mũi tên
        arrow.lineTo(centerX - dpToPx(1.5f), arrowY + dpToPx(5));
        arrow.lineTo(centerX + dpToPx(1.5f), arrowY + dpToPx(5));
        arrow.lineTo(centerX + dpToPx(1.5f), arrowY);
        arrow.lineTo(centerX + dpToPx(3.5f), arrowY);
        arrow.close();

        Paint arrowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arrowPaint.setColor(Color.BLACK);
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrow, arrowPaint);
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    // Phương thức để cập nhật giá trị áp suất từ API
    public void setPressure(float pressure) {
        this.pressure = pressure;
        this.showPlaceholder = false; // Khi có dữ liệu từ API, không hiển thị placeholder nữa
        invalidate(); // Vẽ lại view
    }

    // Phương thức để set range áp suất
    public void setPressureRange(float min, float max) {
        this.minPressure = min;
        this.maxPressure = max;
        invalidate();
    }

    // Phương thức để hiển thị placeholder khi đang loading
    public void showPlaceholder() {
        this.showPlaceholder = true;
        invalidate();
    }

    // Phương thức để ẩn placeholder và hiển thị dữ liệu
    public void hidePlaceholder() {
        this.showPlaceholder = false;
        invalidate();
    }

    // Phương thức để kiểm tra trạng thái placeholder
    public boolean isShowingPlaceholder() {
        return showPlaceholder;
    }

    // Phương thức để lấy giá trị áp suất hiện tại
    public float getPressure() {
        return pressure;
    }

    // Phương thức để cập nhật ngôn ngữ
    public void setLanguage(String language) {
        this.currentLanguage = language;
        invalidate(); // Vẽ lại view để cập nhật nhãn
    }
}