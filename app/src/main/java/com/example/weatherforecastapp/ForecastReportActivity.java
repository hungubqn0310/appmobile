package com.example.weatherforecastapp;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForecastReportActivity extends BaseActivity {
    private static final String API_KEY = "07ccb22a4d5e482f8db72513252805";
    private static final String TAG = "WeatherApp";
    private TextView dateLabel;
    private Animation loadingAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast_report);

        // Khởi tạo background ngay sau setContentView
        initializeBackground();

        // Khởi tạo animation loading giống MainActivity
        loadingAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);

        // Setup settings button if exists
        ImageView settingsButton = findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsPopup());
        }

        String cityName = getIntent().getStringExtra("CITY_NAME");

        // Kiểm tra kết nối mạng và hiển thị loading
        if (!isNetworkAvailable()) {
            showAllLoading();
            Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
            return;
        }

        // Hiển thị loading khi bắt đầu fetch data
        showAllLoading();

        if (cityName != null) {
            fetchForecast(cityName);
        } else {
            fetchWeatherData();
        }

        dateLabel = findViewById(R.id.date_label);

        TextView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    // Settings popup methods - chỉ làm giao diện, không xử lý logic thật
    private void showSettingsPopup() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.popup_settings, null);
        dialog.setContentView(view);

        // Make background transparent
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // Find views
        LinearLayout languageSetting = view.findViewById(R.id.language_setting);
        LinearLayout temperatureSetting = view.findViewById(R.id.temperature_setting);
        TextView currentLanguage = view.findViewById(R.id.current_language);
        TextView currentTempUnit = view.findViewById(R.id.current_temperature_unit);

        // Display current values - mặc định
        currentLanguage.setText("Tiếng Việt");
        currentTempUnit.setText("°C");

        // Handle language click
        languageSetting.setOnClickListener(v -> {
            dialog.dismiss();
            showLanguagePopup();
        });

        // Handle temperature click
        temperatureSetting.setOnClickListener(v -> {
            dialog.dismiss();
            showTemperaturePopup();
        });

        dialog.show();
    }

    private void showLanguagePopup() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.popup_language, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout englishOption = view.findViewById(R.id.english_option);
        LinearLayout vietnameseOption = view.findViewById(R.id.vietnamese_option);
        ImageView englishCheck = view.findViewById(R.id.english_check);
        ImageView vietnameseCheck = view.findViewById(R.id.vietnamese_check);

        // Show check mark for current selection - mặc định Vietnamese
        englishCheck.setVisibility(View.GONE);
        vietnameseCheck.setVisibility(View.VISIBLE);

        englishOption.setOnClickListener(v -> {
            // Chỉ đóng popup, không xử lý logic
            dialog.dismiss();
            Toast.makeText(this, "Đã chọn English", Toast.LENGTH_SHORT).show();
        });

        vietnameseOption.setOnClickListener(v -> {
            // Chỉ đóng popup, không xử lý logic
            dialog.dismiss();
            Toast.makeText(this, "Đã chọn Tiếng Việt", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showTemperaturePopup() {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(R.layout.popup_temperature, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        LinearLayout celsiusOption = view.findViewById(R.id.celsius_option);
        LinearLayout fahrenheitOption = view.findViewById(R.id.fahrenheit_option);
        ImageView celsiusCheck = view.findViewById(R.id.celsius_check);
        ImageView fahrenheitCheck = view.findViewById(R.id.fahrenheit_check);

        // Show check mark for current selection - mặc định Celsius
        celsiusCheck.setVisibility(View.VISIBLE);
        fahrenheitCheck.setVisibility(View.GONE);

        celsiusOption.setOnClickListener(v -> {
            // Chỉ đóng popup, không xử lý logic
            dialog.dismiss();
            Toast.makeText(this, "Đã chọn °C", Toast.LENGTH_SHORT).show();
        });

        fahrenheitOption.setOnClickListener(v -> {
            // Chỉ đóng popup, không xử lý logic
            dialog.dismiss();
            Toast.makeText(this, "Đã chọn °F", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    // Kiểm tra kết nối mạng giống MainActivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network == null) return false;
                NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                return networkCapabilities != null &&
                        networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
            } else {
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                return networkInfo != null && networkInfo.isConnected();
            }
        }
        return false;
    }

    // Hiển thị loading cho tất cả ImageView có src="@drawable/ic_cloud_sun"
    private void showAllLoading() {
        // Loading cho daily forecast (7 ngày)
        for (int i = 1; i <= 7; i++) {
            int containerId = getResources().getIdentifier("day" + i + "_container", "id", getPackageName());
            LinearLayout container = findViewById(containerId);
            if (container != null && container.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                // Chỉ áp dụng loading cho ImageView có src="@drawable/ic_cloud_sun"
                iconImageView.setImageResource(R.drawable.ic_loading);
                iconImageView.startAnimation(loadingAnimation);
            }
        }

        // Loading cho hourly forecast (24 giờ)
        for (int i = 1; i <= 24; i++) {
            int hourlyItemId = getResources().getIdentifier("hourly_forecast_item" + i, "id", getPackageName());
            LinearLayout hourlyItem = findViewById(hourlyItemId);
            if (hourlyItem != null && hourlyItem.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                // Chỉ áp dụng loading cho ImageView có src="@drawable/ic_cloud_sun"
                iconImageView.setImageResource(R.drawable.ic_loading);
                iconImageView.startAnimation(loadingAnimation);
            }
        }
    }

    // Ẩn loading cho tất cả ImageView
    private void hideAllLoading() {
        // Ẩn loading cho daily forecast
        for (int i = 1; i <= 7; i++) {
            int containerId = getResources().getIdentifier("day" + i + "_container", "id", getPackageName());
            LinearLayout container = findViewById(containerId);
            if (container != null && container.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                iconImageView.clearAnimation();
            }
        }

        // Ẩn loading cho hourly forecast
        for (int i = 1; i <= 24; i++) {
            int hourlyItemId = getResources().getIdentifier("hourly_forecast_item" + i, "id", getPackageName());
            LinearLayout hourlyItem = findViewById(hourlyItemId);
            if (hourlyItem != null && hourlyItem.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                iconImageView.clearAnimation();
            }
        }
    }

    // Lấy giờ hiện tại từ API response
    private String getCurrentHour(JsonObject jsonObject) {
        try {
            JsonObject location = jsonObject.getAsJsonObject("location");
            String localtime = location.get("localtime").getAsString();
            // localtime format: "2025-05-28 14:33"
            String time = localtime.split(" ")[1]; // "14:33"
            String hour = time.split(":")[0]; // "14"
            return hour + ":00"; // "14:00"
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy giờ hiện tại: " + e.getMessage());
            return null;
        }
    }

    private void fetchForecast(String cityName) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY + "&q=" + cityName + "&days=7&aqi=no&alerts=no";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gọi API dự báo thời tiết: " + e.getMessage());
                runOnUiThread(() -> {
                    // Giữ loading khi có lỗi, không ẩn
                    Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonObject location = jsonObject.getAsJsonObject("location");
                        String currentDate = location.get("localtime").getAsString().split(" ")[0];

                        // Lấy giờ hiện tại
                        String currentHour = getCurrentHour(jsonObject);

                        JsonArray forecastDays = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday");
                        List<DailyForecast> forecasts = new ArrayList<>();
                        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

                        for (int i = 0; i < forecastDays.size(); i++) {
                            JsonObject dayForecast = forecastDays.get(i).getAsJsonObject();
                            String date = dayForecast.get("date").getAsString();
                            JsonObject dayInfo = dayForecast.getAsJsonObject("day");
                            double avgTemp = dayInfo.get("avgtemp_c").getAsDouble();
                            String iconUrl = "https:" + dayInfo.getAsJsonObject("condition").get("icon").getAsString();

                            String formattedDate = formatDate(date);
                            DailyForecast forecast = new DailyForecast(formattedDate, iconUrl, String.format("%.1f°C", avgTemp));
                            forecasts.add(forecast);

                            if (i == 0) {
                                JsonArray hoursArray = dayForecast.getAsJsonArray("hour");
                                for (int j = 0; j < hoursArray.size(); j++) {
                                    JsonObject hourData = hoursArray.get(j).getAsJsonObject();
                                    String time = hourData.get("time").getAsString();
                                    double tempC = hourData.get("temp_c").getAsDouble();
                                    String hourIconUrl = "https:" + hourData.getAsJsonObject("condition").get("icon").getAsString();
                                    String hourOnly = time.substring(11, 16);

                                    HourlyForecast hourlyForecast = new HourlyForecast(hourOnly, hourIconUrl, String.format("%.0f°C", tempC));
                                    hourlyForecasts.add(hourlyForecast);
                                }
                            }
                        }

                        final String formattedCurrentDate = formatCurrentDate(currentDate);
                        final String finalCurrentHour = currentHour;

                        runOnUiThread(() -> {
                            // Ẩn loading trước khi cập nhật UI
                            hideAllLoading();
                            dateLabel.setText(formattedCurrentDate);
                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts, finalCurrentHour);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON dự báo: " + e.getMessage());
                        runOnUiThread(() -> {
                            // Giữ loading khi có lỗi
                            Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> {
                        // Giữ loading khi có lỗi
                        Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    private void fetchWeatherData() {
        String API_URL = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY + "&q=Hanoi&days=7&aqi=no&alerts=no";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(API_URL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gọi API: " + e.getMessage());
                runOnUiThread(() -> {
                    // Giữ loading khi có lỗi
                    Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonObject location = jsonObject.getAsJsonObject("location");
                        String currentDate = location.get("localtime").getAsString().split(" ")[0];

                        // Lấy giờ hiện tại
                        String currentHour = getCurrentHour(jsonObject);

                        JsonArray forecastDays = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday");
                        List<DailyForecast> forecasts = new ArrayList<>();
                        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

                        for (int i = 0; i < forecastDays.size(); i++) {
                            JsonObject dayForecast = forecastDays.get(i).getAsJsonObject();
                            String date = dayForecast.get("date").getAsString();
                            JsonObject dayInfo = dayForecast.getAsJsonObject("day");
                            double avgTemp = dayInfo.get("avgtemp_c").getAsDouble();
                            String iconUrl = "https:" + dayInfo.getAsJsonObject("condition").get("icon").getAsString();

                            String formattedDate = formatDate(date);
                            DailyForecast forecast = new DailyForecast(formattedDate, iconUrl, String.format("%.1f°C", avgTemp));
                            forecasts.add(forecast);

                            if (i == 0) {
                                JsonArray hoursArray = dayForecast.getAsJsonArray("hour");
                                for (int j = 0; j < hoursArray.size(); j++) {
                                    JsonObject hourData = hoursArray.get(j).getAsJsonObject();
                                    String time = hourData.get("time").getAsString();
                                    double tempC = hourData.get("temp_c").getAsDouble();
                                    String hourIconUrl = "https:" + hourData.getAsJsonObject("condition").get("icon").getAsString();
                                    String hourOnly = time.substring(11, 16);

                                    HourlyForecast hourlyForecast = new HourlyForecast(hourOnly, hourIconUrl, String.format("%.0f°C", tempC));
                                    hourlyForecasts.add(hourlyForecast);
                                }
                            }
                        }

                        final String formattedCurrentDate = formatCurrentDate(currentDate);
                        final String finalCurrentHour = currentHour;

                        runOnUiThread(() -> {
                            // Ẩn loading trước khi cập nhật UI
                            hideAllLoading();
                            dateLabel.setText(formattedCurrentDate);
                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts, finalCurrentHour);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON: " + e.getMessage());
                        runOnUiThread(() -> {
                            // Giữ loading khi có lỗi
                            Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> {
                        // Giữ loading khi có lỗi
                        Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu thời tiết", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    // Các method còn lại giữ nguyên
    private String formatCurrentDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày hiện tại: " + e.getMessage());
            return inputDate;
        }
    }

    private void updateHourlyForecast(List<HourlyForecast> hourlyForecasts, String currentHour) {
        int maxHours = Math.min(hourlyForecasts.size(), 24);

        for (int i = 0; i < maxHours; i++) {
            int hourlyItemId = getResources().getIdentifier("hourly_forecast_item" + (i + 1), "id", getPackageName());
            LinearLayout hourlyItem = findViewById(hourlyItemId);

            if (hourlyItem != null) {
                HourlyForecast forecast = hourlyForecasts.get(i);
                TextView tempTextView = (TextView) hourlyItem.getChildAt(0);
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                TextView timeTextView = (TextView) hourlyItem.getChildAt(2);

                tempTextView.setText(forecast.getTemperature());
                timeTextView.setText(forecast.getTime());

                // Kiểm tra nếu đây là giờ hiện tại
                if (currentHour != null && forecast.getTime().equals(currentHour)) {
                    // Áp dụng background cho giờ hiện tại
                    hourlyItem.setBackgroundResource(R.drawable.info_box_bg);
                } else {
                    // Xóa background cho các giờ khác
                    hourlyItem.setBackground(null);
                }

                // Load ảnh thời tiết thực tế
                Glide.with(this).load(forecast.getIconUrl()).into(iconImageView);
            }
        }
    }

    private void updateUI(List<DailyForecast> forecasts) {
        for (int i = 0; i < forecasts.size(); i++) {
            int containerId = getResources().getIdentifier("day" + (i + 1) + "_container", "id", getPackageName());
            LinearLayout container = findViewById(containerId);

            if (container != null) {
                TextView dateTextView = (TextView) container.getChildAt(0);
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                TextView tempTextView = (TextView) container.getChildAt(2);

                DailyForecast forecast = forecasts.get(i);
                dateTextView.setText(forecast.getDate());
                tempTextView.setText(forecast.getTemperature());

                // Load ảnh thời tiết thực tế
                Glide.with(this).load(forecast.getConditionIconUrl()).into(iconImageView);
            }
        }
    }

    private String formatDate(String inputDate) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày tháng: " + e.getMessage());
            return inputDate;
        }
    }

    private static class DailyForecast {
        private final String date;
        private final String conditionIconUrl;
        private final String temperature;

        public DailyForecast(String date, String conditionIconUrl, String temperature) {
            this.date = date;
            this.conditionIconUrl = conditionIconUrl;
            this.temperature = temperature;
        }

        public String getDate() { return date; }
        public String getConditionIconUrl() { return conditionIconUrl; }
        public String getTemperature() { return temperature; }
    }

    private static class HourlyForecast {
        private final String time;
        private final String iconUrl;
        private final String temperature;

        public HourlyForecast(String time, String iconUrl, String temperature) {
            this.time = time;
            this.iconUrl = iconUrl;
            this.temperature = temperature;
        }

        public String getTime() { return time; }
        public String getIconUrl() { return iconUrl; }
        public String getTemperature() { return temperature; }
    }
}