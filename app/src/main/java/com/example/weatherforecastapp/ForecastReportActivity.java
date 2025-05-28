package com.example.weatherforecastapp;

import android.os.Bundle;
import android.widget.TextView;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast_report);

        // Khởi tạo background ngay sau setContentView - DÒNG QUAN TRỌNG NÀY BỊ THIẾU!
        initializeBackground();

        String cityName = getIntent().getStringExtra("CITY_NAME");
        if (cityName != null) {
            fetchForecast(cityName);
        } else {
            fetchWeatherData();
        }

        dateLabel = findViewById(R.id.date_label);

        TextView backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> finish());
    }

    private void fetchForecast(String cityName) {
        OkHttpClient client = new OkHttpClient();
        String url = "https://api.weatherapi.com/v1/forecast.json?key=" + API_KEY + "&q=" + cityName + "&days=7&aqi=no&alerts=no";

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Lỗi khi gọi API dự báo thời tiết: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonObject location = jsonObject.getAsJsonObject("location");
                        String currentDate = location.get("localtime").getAsString().split(" ")[0];

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

                        runOnUiThread(() -> {
                            dateLabel.setText(formattedCurrentDate);
                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON dự báo: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu dự báo thời tiết", Toast.LENGTH_SHORT).show());
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
                runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Không thể kết nối đến máy chủ thời tiết", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    try {
                        JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();
                        JsonObject location = jsonObject.getAsJsonObject("location");
                        String currentDate = location.get("localtime").getAsString().split(" ")[0];

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

                        runOnUiThread(() -> {
                            dateLabel.setText(formattedCurrentDate);
                            updateUI(forecasts);
                            updateHourlyForecast(hourlyForecasts);
                        });

                    } catch (Exception e) {
                        Log.e(TAG, "Lỗi khi xử lý dữ liệu JSON: " + e.getMessage());
                        runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Lỗi khi xử lý dữ liệu thời tiết", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    Log.e(TAG, "Phản hồi không thành công: " + response.code());
                    runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, "Không thể lấy dữ liệu thời tiết", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

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

    private void updateHourlyForecast(List<HourlyForecast> hourlyForecasts) {
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