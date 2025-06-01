package com.example.weatherforecastapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import java.util.Calendar;

public class SunArcView extends View {
    private Paint arcPaint;
    private Paint sunPaint;
    private Paint grayArcPaint;
    private Paint textPaint;
    // Thêm biến để lưu thời gian hiện tại
    private int currentHour = 0;
    private int currentMinute = 0;
    private RectF arcRect;

    // Thời gian bình minh và hoàng hôn (theo phút từ 00:00) - sẽ được cập nhật từ API
    private int sunriseTime = 6 * 60; // Mặc định 06:00 nếu chưa có dữ liệu từ API
    private int sunsetTime = 18 * 60; // Mặc định 18:00 nếu chưa có dữ liệu từ API

    private float sunPosition = 0f;
    private String currentTimeText = "";

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
    // Thêm phương thức để set thời gian hiện tại từ API
    public void setCurrentTime(String timeString) {
        try {
            String[] timeParts = timeString.split(":");
            this.currentHour = Integer.parseInt(timeParts[0]);
            this.currentMinute = Integer.parseInt(timeParts[1]);

            // Cập nhật vị trí mặt trời với thời gian mới
            updateSunPositionWithTime(currentHour, currentMinute);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // Thêm phương thức mới để cập nhật vị trí với thời gian cụ thể
    private void updateSunPositionWithTime(int hour, int minute) {
        int currentTimeInMinutes = hour * 60 + minute;

        // Cập nhật text thời gian hiện tại
        currentTimeText = String.format("%02d:%02d", hour, minute);

        // Tính toán vị trí mặt trời
        if (currentTimeInMinutes < sunriseTime) {
            sunPosition = 0f;
        } else if (currentTimeInMinutes > sunsetTime) {
            sunPosition = 1f;
        } else {
            int dayDuration = sunsetTime - sunriseTime;
            int timeFromSunrise = currentTimeInMinutes - sunriseTime;
            sunPosition = (float) timeFromSunrise / dayDuration;
        }

        sunPosition = Math.max(0f, Math.min(1f, sunPosition));
        invalidate();
    }
    private void init() {
        // Paint cho đường cung màu vàng
        arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        arcPaint.setColor(0xFFFFA500);
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(8f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho đường cung màu xám
        grayArcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        grayArcPaint.setColor(0x66FFFFFF); // Màu trắng trong suốt
        grayArcPaint.setStyle(Paint.Style.STROKE);
        grayArcPaint.setStrokeWidth(8f);
        grayArcPaint.setStrokeCap(Paint.Cap.ROUND);

        // Paint cho điểm mặt trời
        sunPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sunPaint.setColor(0xFFFFA500);
        sunPaint.setStyle(Paint.Style.FILL);
        sunPaint.setShadowLayer(12f, 0f, 0f, 0xFFFFD700); // Hiệu ứng phát sáng

        // Paint cho text thời gian hiện tại
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);

        // Thiết lập kích thước chữ theo đơn vị sp, ví dụ 18sp
        float textSizeSp = 16f;
        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                getResources().getDisplayMetrics()
        );
        textPaint.setTextSize(textSizePx);
        textPaint.setTextAlign(Paint.Align.CENTER);

        arcRect = new RectF();

        // Không cập nhật ngay vì chưa có dữ liệu từ API
        // updateSunPosition() sẽ được gọi sau khi có dữ liệu từ API
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Tạo hình chữ nhật cho đường cung
        float padding = 40f;
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

        // Vẽ điểm mặt trời với hiệu ứng phát sáng
        canvas.drawCircle(sunX, sunY, 12f, sunPaint);

        // Vẽ thời gian hiện tại phía trên mặt trời
        if (!currentTimeText.isEmpty()) {
            canvas.drawText(currentTimeText, sunX, sunY - 25f, textPaint);
        }

        // Vẽ nhãn bình minh và hoàng hôn
        float baseY = arcRect.bottom + 20f;

        // Nhãn bình minh (bên trái)
        Paint sunriseTextPaint = new Paint(textPaint);
        sunriseTextPaint.setTextAlign(Paint.Align.LEFT);
        sunriseTextPaint.setAlpha(180);
        canvas.drawText("Bình minh", arcRect.left, baseY, sunriseTextPaint);
        canvas.drawText(formatTime(sunriseTime), arcRect.left, baseY + 20f, sunriseTextPaint);

        // Nhãn hoàng hôn (bên phải)
        Paint sunsetTextPaint = new Paint(textPaint);
        sunsetTextPaint.setTextAlign(Paint.Align.RIGHT);
        sunsetTextPaint.setAlpha(180);
        canvas.drawText("Hoàng hôn", arcRect.right, baseY, sunsetTextPaint);
        canvas.drawText(formatTime(sunsetTime), arcRect.right, baseY + 20f, sunsetTextPaint);
    }

    // Cập nhật vị trí mặt trời dựa trên thời gian hiện tại
    public void updateSunPosition() {
        // Nếu chưa có thời gian từ API, sử dụng thời gian hệ thống
        if (currentHour == 0 && currentMinute == 0) {
            Calendar calendar = Calendar.getInstance();
            currentHour = calendar.get(Calendar.HOUR_OF_DAY);
            currentMinute = calendar.get(Calendar.MINUTE);
        }

        updateSunPositionWithTime(currentHour, currentMinute);
    }

    // Đặt thời gian bình minh và hoàng hôn
    public void setSunTimes(int sunriseHour, int sunriseMinute, int sunsetHour, int sunsetMinute) {
        this.sunriseTime = sunriseHour * 60 + sunriseMinute;
        this.sunsetTime = sunsetHour * 60 + sunsetMinute;
        updateSunPosition();
    }

    // Đặt thời gian bình minh và hoàng hôn (chuỗi format HH:mm)
    public void setSunTimes(String sunriseStr, String sunsetStr) {
        try {
            String[] sunriseParts = sunriseStr.split(":");
            String[] sunsetParts = sunsetStr.split(":");

            int sunriseHour = Integer.parseInt(sunriseParts[0]);
            int sunriseMinute = Integer.parseInt(sunriseParts[1]);
            int sunsetHour = Integer.parseInt(sunsetParts[0]);
            int sunsetMinute = Integer.parseInt(sunsetParts[1]);

            setSunTimes(sunriseHour, sunriseMinute, sunsetHour, sunsetMinute);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Chuyển đổi phút thành chuỗi thời gian HH:mm
    private String formatTime(int minutes) {
        int hours = minutes / 60;
        int mins = minutes % 60;
        return String.format("%02d:%02d", hours, mins);
    }

    // Getter cho vị trí mặt trời hiện tại (0.0 - 1.0)
    public float getSunPosition() {
        return sunPosition;
    }

    // Kiểm tra xem hiện tại có phải ban ngày không
    public boolean isDaytime() {
        Calendar calendar = Calendar.getInstance();
        int currentTimeInMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE);
        return currentTimeInMinutes >= sunriseTime && currentTimeInMinutes <= sunsetTime;
    }
}