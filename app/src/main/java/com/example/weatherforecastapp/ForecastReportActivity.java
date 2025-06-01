package com.example.weatherforecastapp;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForecastReportActivity extends BaseActivity {
    private static final String API_KEY = "07ccb22a4d5e482f8db72513252805";
    private static final String TAG = "WeatherApp";
    private static final String BASE_URL = "https://api.weatherapi.com/v1/forecast.json";
    private static final int FORECAST_DAYS = 7;
    private static final int MAX_HOURLY_ITEMS = 24;
    private static final int MAX_DAILY_ITEMS = 7;

    // UI Components
    private TextView dateLabel;
    private Animation loadingAnimation;
    private SunArcView sunArcView;
    private TextView tvBinhMinh, tvHoangHon, tvTrangMoc, tvTrangLan, tvMoonPhase;

    // HTTP Client
    private OkHttpClient httpClient;
    // Thêm vào đầu class
    private static final String SETTINGS_PREFS = "SettingsPrefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TEMPERATURE_UNIT = "temperature_unit";
    private static final String LANG_VIETNAMESE = "vi";
    private static final String LANG_ENGLISH = "en";
    private static final String TEMP_CELSIUS = "celsius";
    private static final String TEMP_FAHRENHEIT = "fahrenheit";

    private SharedPreferences settingsPrefs;
    private String currentLanguage = LANG_VIETNAMESE;
    private String currentTempUnit = TEMP_CELSIUS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast_report);

        initializeComponents();
        updateLabels();
        setupEventListeners();
        loadWeatherData();
    }

    private void initializeComponents() {
        // Initialize background
        initializeBackground();

        // Initialize views
        initViews();

        // Initialize animation
        loadingAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);

        // Initialize HTTP client
        httpClient = new OkHttpClient();
        // Khởi tạo settings
        settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        currentLanguage = settingsPrefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE);
        currentTempUnit = settingsPrefs.getString(KEY_TEMPERATURE_UNIT, TEMP_CELSIUS);
    }

    private void initViews() {
        dateLabel = findViewById(R.id.date_label);
        sunArcView = findViewById(R.id.sunArcView);
        tvBinhMinh = findViewById(R.id.tvBinhMinh);
        tvHoangHon = findViewById(R.id.tvHoangHon);
        tvTrangMoc = findViewById(R.id.tvTrangMoc);
        tvTrangLan = findViewById(R.id.tvTrangLan);
//        tvMoonPhase = findViewById(R.id.tvMoonPhase);
    }
    private static final class LanguageStrings {
        // Tiếng Việt
        private static final Map<String, String> VI = new HashMap<String, String>() {{
            put("BACK","Quay về");
            put("TODAY", "Hôm nay");
            put("NEXT_7_DAYS", "Dự báo 7 ngày tới");
            put("FEELS_LIKE", "Cảm nhận");
            put("HUMIDITY", "Độ ẩm");
            put("VISIBILITY", "Tầm nhìn");
            put("PRECIPITATION", "Lượng mưa");
            put("SUNRISE", "Bình minh");
            put("SUNSET", "Hoàng hôn");
            put("MOONRISE", "Trăng mọc");
            put("MOONSET", "Trăng lặn");
            put("PRESSURE", "Áp suất");
            put("WIND", "Gió");
            put("SETTINGS", "Cài đặt");
            put("LANGUAGE", "Ngôn ngữ");
            put("TEMPERATURE", "Đơn vị nhiệt độ");
        }};

        // English
        private static final Map<String, String> EN = new HashMap<String, String>() {{
            put("BACK","Back");
            put("TODAY", "Today");
            put("NEXT_7_DAYS", "Forecast on 7 days");
            put("FEELS_LIKE", "Feels like");
            put("HUMIDITY", "Humidity");
            put("VISIBILITY", "Visibility");
            put("PRECIPITATION", "Precipitation");
            put("SUNRISE", "Sunrise");
            put("SUNSET", "Sunset");
            put("MOONRISE", "Moonrise");
            put("MOONSET", "Moonset");
            put("PRESSURE", "Pressure");
            put("WIND", "Wind");
            put("SETTINGS", "Settings");
            put("LANGUAGE", "Language");
            put("TEMPERATURE", "Temperature Unit");
        }};
    }

    private String getLocalizedString(String key) {
        Map<String, String> strings = currentLanguage.equals(LANG_VIETNAMESE) ?
                LanguageStrings.VI : LanguageStrings.EN;
        String value = strings.get(key);
        return value != null ? value : key;
    }
    private void updateLabels() {
        // Cập nhật các label cố định
        TextView tvBackbutton = findViewById(R.id.back_button);
        TextView tvToday = findViewById(R.id.today);
        TextView tvNextForecast = findViewById(R.id.next_forecast_title);
        TextView labelFeelsLike = findViewById(R.id.label_feels_like);
        TextView labelHumidity = findViewById(R.id.label_humidity);
        TextView labelVisibility = findViewById(R.id.label_visibility);
        TextView labelPrecipitation = findViewById(R.id.label_precipitation);
        TextView labelSunrise = findViewById(R.id.label_sunrise);
        TextView labelSunset = findViewById(R.id.label_sunset);
        TextView labelMoonrise = findViewById(R.id.label_moonrise);
        TextView labelMoonset = findViewById(R.id.label_moonset);
        TextView labelPressure = findViewById(R.id.label_pressure);
        TextView labelWind = findViewById(R.id.label_wind);
        if(tvBackbutton != null) tvBackbutton.setText(getLocalizedString("BACK"));
        if (tvToday != null) tvToday.setText(getLocalizedString("TODAY"));
        if (tvNextForecast != null) tvNextForecast.setText(getLocalizedString("NEXT_7_DAYS"));
        if (labelFeelsLike != null) labelFeelsLike.setText(getLocalizedString("FEELS_LIKE"));
        if (labelHumidity != null) labelHumidity.setText(getLocalizedString("HUMIDITY"));
        if (labelVisibility != null) labelVisibility.setText(getLocalizedString("VISIBILITY"));
        if (labelPrecipitation != null) labelPrecipitation.setText(getLocalizedString("PRECIPITATION"));
        if (labelSunrise != null) labelSunrise.setText(getLocalizedString("SUNRISE"));
        if (labelSunset != null) labelSunset.setText(getLocalizedString("SUNSET"));
        if (labelMoonrise != null) labelMoonrise.setText(getLocalizedString("MOONRISE"));
        if (labelMoonset != null) labelMoonset.setText(getLocalizedString("MOONSET"));
        if (labelPressure != null) labelPressure.setText(getLocalizedString("PRESSURE"));
        if (labelWind != null) labelWind.setText(getLocalizedString("WIND"));
    }
    private void setupEventListeners() {
        // Settings button
        ImageView settingsButton = findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsPopup());
        }

        // Back button
        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void loadWeatherData() {
        if (!isNetworkAvailable()) {
            handleNetworkError();
            return;
        }

        showAllLoading();

        String cityName = getIntent().getStringExtra("CITY_NAME");
        if (cityName != null && !cityName.trim().isEmpty()) {
            fetchForecast(cityName);
        } else {
            fetchWeatherData();
        }
    }

    private void handleNetworkError() {
        showAllLoading();
        handlePressureError(); // Thêm dòng này
        Toast.makeText(this, "Không có kết nối mạng", Toast.LENGTH_SHORT).show();
    }

    // Network connectivity check
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

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

    // Loading animation methods
    private void showAllLoading() {
        showDailyForecastLoading();
        showHourlyForecastLoading();
        showPressureLoading(); // Thêm dòng này
    }
    private void handlePressureError() {
        PressureGaugeView pressureGaugeView = findViewById(R.id.pressureGaugeView);
        if (pressureGaugeView != null) {
            pressureGaugeView.showPlaceholder();
        }
    }
    private void showDailyForecastLoading() {
        for (int i = 1; i <= MAX_DAILY_ITEMS; i++) {
            LinearLayout container = findDailyContainer(i);
            if (container != null && container.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                iconImageView.setImageResource(R.drawable.ic_loading);
                iconImageView.startAnimation(loadingAnimation);
            }
        }
    }

    private void showHourlyForecastLoading() {
        for (int i = 1; i <= MAX_HOURLY_ITEMS; i++) {
            LinearLayout hourlyItem = findHourlyItem(i);
            if (hourlyItem != null && hourlyItem.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                iconImageView.setImageResource(R.drawable.ic_loading);
                iconImageView.startAnimation(loadingAnimation);
            }
        }
    }

    private void hideAllLoading() {
        hideDailyForecastLoading();
        hideHourlyForecastLoading();
    }

    private void hideDailyForecastLoading() {
        for (int i = 1; i <= MAX_DAILY_ITEMS; i++) {
            LinearLayout container = findDailyContainer(i);
            if (container != null && container.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) container.getChildAt(1);
                iconImageView.clearAnimation();
            }
        }
    }

    private void hideHourlyForecastLoading() {
        for (int i = 1; i <= MAX_HOURLY_ITEMS; i++) {
            LinearLayout hourlyItem = findHourlyItem(i);
            if (hourlyItem != null && hourlyItem.getChildCount() > 1) {
                ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
                iconImageView.clearAnimation();
            }
        }
    }

    // Helper methods for finding views
    private LinearLayout findDailyContainer(int index) {
        int containerId = getResources().getIdentifier("day" + index + "_container", "id", getPackageName());
        return findViewById(containerId);
    }

    private LinearLayout findHourlyItem(int index) {
        int hourlyItemId = getResources().getIdentifier("hourly_forecast_item" + index, "id", getPackageName());
        return findViewById(hourlyItemId);
    }

    // API methods
    private void fetchForecast(String cityName) {
        String url = buildApiUrl(cityName);
        makeApiRequest(url);
    }

    private void fetchWeatherData() {
        String url = buildApiUrl("Hanoi");
        makeApiRequest(url);
    }

    private String buildApiUrl(String cityName) {
        return BASE_URL + "?key=" + API_KEY + "&q=" + cityName + "&days=" + FORECAST_DAYS + "&aqi=no&alerts=no";
    }

    private void makeApiRequest(String url) {
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handleApiError("Lỗi khi gọi API: " + e.getMessage(), "Không thể kết nối đến máy chủ thời tiết");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonData = response.body().string();
                        processApiResponse(jsonData);
                    } catch (Exception e) {
                        handleApiError("Lỗi khi xử lý dữ liệu JSON: " + e.getMessage(), "Lỗi khi xử lý dữ liệu thời tiết");
                    }
                } else {
                    handleApiError("Phản hồi không thành công: " + response.code(), "Không thể lấy dữ liệu thời tiết");
                }
            }
        });
    }

    private void handleApiError(String logMessage, String userMessage) {
        Log.e(TAG, logMessage);
        runOnUiThread(() -> Toast.makeText(ForecastReportActivity.this, userMessage, Toast.LENGTH_SHORT).show());
    }

    private void processApiResponse(String jsonData) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonData).getAsJsonObject();

            // Extract basic info
            JsonObject location = jsonObject.getAsJsonObject("location");
            String currentDate = location.get("localtime").getAsString().split(" ")[0];
            String currentHour = getCurrentHour(jsonObject);

            // Extract current weather data
            JsonObject current = jsonObject.getAsJsonObject("current");

            // Extract forecast data
            JsonArray forecastDays = jsonObject.getAsJsonObject("forecast").getAsJsonArray("forecastday");

            // Process forecasts
            ForecastData forecastData = processForecastData(forecastDays);

            // Extract astro data
            AstroData astroData = extractAstroData(forecastDays);

            // Update UI on main thread
            updateUIOnMainThread(currentDate, currentHour, current, forecastData, astroData);

        } catch (Exception e) {
            handleApiError("Lỗi khi xử lý dữ liệu JSON: " + e.getMessage(), "Lỗi khi xử lý dữ liệu thời tiết");
        }
    }

    private String getCurrentHour(JsonObject jsonObject) {
        try {
            JsonObject location = jsonObject.getAsJsonObject("location");
            String localtime = location.get("localtime").getAsString();
            String time = localtime.split(" ")[1];
            String hour = time.split(":")[0];
            return hour + ":00";
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi lấy giờ hiện tại: " + e.getMessage());
            return null;
        }
    }

    private ForecastData processForecastData(JsonArray forecastDays) {
        List<DailyForecast> dailyForecasts = new ArrayList<>();
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();

        for (int i = 0; i < forecastDays.size(); i++) {
            JsonObject dayForecast = forecastDays.get(i).getAsJsonObject();

            // Process daily forecast
            DailyForecast dailyForecast = processDailyForecast(dayForecast);
            dailyForecasts.add(dailyForecast);

            // Process hourly forecast for first day only
            if (i == 0) {
                hourlyForecasts = processHourlyForecast(dayForecast);
            }
        }

        return new ForecastData(dailyForecasts, hourlyForecasts);
    }

    private DailyForecast processDailyForecast(JsonObject dayForecast) {
        String date = dayForecast.get("date").getAsString();
        JsonObject dayInfo = dayForecast.getAsJsonObject("day");
        double avgTemp = dayInfo.get("avgtemp_c").getAsDouble();
        String iconUrl = "https:" + dayInfo.getAsJsonObject("condition").get("icon").getAsString();

        String formattedDate = formatDate(date);
        return new DailyForecast(formattedDate, iconUrl, formatTemperature(avgTemp));
    }

    private List<HourlyForecast> processHourlyForecast(JsonObject dayForecast) {
        List<HourlyForecast> hourlyForecasts = new ArrayList<>();
        JsonArray hoursArray = dayForecast.getAsJsonArray("hour");

        for (int j = 0; j < hoursArray.size(); j++) {
            JsonObject hourData = hoursArray.get(j).getAsJsonObject();
            String time = hourData.get("time").getAsString();
            double tempC = hourData.get("temp_c").getAsDouble();
            String hourIconUrl = "https:" + hourData.getAsJsonObject("condition").get("icon").getAsString();
            String hourOnly = time.substring(11, 16);

            HourlyForecast hourlyForecast = new HourlyForecast(hourOnly, hourIconUrl, formatTemperature(tempC));
            hourlyForecasts.add(hourlyForecast);
        }

        return hourlyForecasts;
    }

    private AstroData extractAstroData(JsonArray forecastDays) {
        if (forecastDays.size() > 0) {
            JsonObject firstDay = forecastDays.get(0).getAsJsonObject();
            if (firstDay.has("astro")) {
                JsonObject astro = firstDay.getAsJsonObject("astro");
                return new AstroData(
                        astro.get("sunrise").getAsString(),
                        astro.get("sunset").getAsString(),
                        astro.get("moonrise").getAsString(),
                        astro.get("moonset").getAsString(),
                        astro.get("moon_phase").getAsString()
                );
            }
        }
        return null;
    }

    private void updateUIOnMainThread(String currentDate, String currentHour, JsonObject current,
                                      ForecastData forecastData, AstroData astroData) {
        String formattedCurrentDate = formatCurrentDate(currentDate);

        runOnUiThread(() -> {
            hideAllLoading();
            dateLabel.setText(formattedCurrentDate);
            updateWeatherDetails(current);
            updateDailyForecastUI(forecastData.dailyForecasts);
            updateHourlyForecastUI(forecastData.hourlyForecasts, currentHour);

            if (astroData != null) {
                updateAstroData(astroData);
            }
        });
    }

    // UI Update methods
    private void updateWeatherDetails(JsonObject current) {
        if (current == null) return;

        try {
            updateTemperatureDetails(current);
            updateWindAndPressure(current);
        } catch (Exception e) {
            Log.e(TAG, "Error updating weather details: " + e.getMessage());
        }
    }

    private void updateTemperatureDetails(JsonObject current) {
        TextView tvCamNhan = findViewById(R.id.tvCamNhan);
        TextView tvDoAm = findViewById(R.id.tvDoAm);
        TextView tvTamNhin = findViewById(R.id.tvTamNhin);
        TextView tvLuongMua = findViewById(R.id.tvLuongMua);

        // Cảm nhận nhiệt độ với mô tả
        if (tvCamNhan != null && current.has("feelslike_c")) {
            double feelsLike = current.get("feelslike_c").getAsDouble();
            String description = getFeelsLikeDescription(feelsLike);
            String feelsLikeText = String.format(Locale.getDefault(), "%.0f°                       %s", feelsLike, description);
            tvCamNhan.setText(feelsLikeText);
        } else if (tvCamNhan != null) {
            tvCamNhan.setText("Không xác định");
        }

        // Độ ẩm
        if (tvDoAm != null && current.has("humidity")) {
            int humidity = current.get("humidity").getAsInt();
            tvDoAm.setText(String.format(Locale.getDefault(), "%d%%", humidity));
        } else if (tvDoAm != null) {
            tvDoAm.setText("--%");
        }

        // Tầm nhìn với số và mô tả
        if (tvTamNhin != null && current.has("vis_km")) {
            double visibility = current.get("vis_km").getAsDouble();
            String description = getVisibilityDescription(visibility);
            String visibilityText = String.format(Locale.getDefault(), "%.1f km                   %s", visibility, description);
            tvTamNhin.setText(visibilityText);
        } else if (tvTamNhin != null) {
            tvTamNhin.setText("Không xác định");
        }

        // Lượng mưa
        if (tvLuongMua != null && current.has("precip_mm")) {
            double precipMm = current.get("precip_mm").getAsDouble();
            String precipText;
            if (currentLanguage.equals(LANG_ENGLISH)) {
                precipText = precipMm == 0 ? "No rain" : String.format(Locale.getDefault(), "%.1f mm", precipMm);
            } else {
                precipText = precipMm == 0 ? "Không mưa" : String.format(Locale.getDefault(), "%.1f mm", precipMm);
            }
            tvLuongMua.setText(precipText);
        } else if (tvLuongMua != null) {
            tvLuongMua.setText(currentLanguage.equals(LANG_ENGLISH) ? "Undefined" : "Không xác định");
        }
    }

    // Phương thức mô tả cảm nhận nhiệt độ
    private String getFeelsLikeDescription(double feelsLike) {
        if (currentLanguage.equals(LANG_ENGLISH)) {
            if (feelsLike < 10) {
                return "Very cold";
            } else if (feelsLike < 16) {
                return "Cold";
            } else if (feelsLike < 20) {
                return "Cool";
            } else if (feelsLike < 25) {
                return "Comfortable";
            } else if (feelsLike < 30) {
                return "Warm";
            } else if (feelsLike < 35) {
                return "Hot";
            } else if (feelsLike < 40) {
                return "Very hot";
            } else {
                return "Extremely hot";
            }
        } else {
            if (feelsLike < 10) {
                return "Rất lạnh";
            } else if (feelsLike < 16) {
                return "Lạnh";
            } else if (feelsLike < 20) {
                return "Mát mẻ";
            } else if (feelsLike < 25) {
                return "Dễ chịu";
            } else if (feelsLike < 30) {
                return "Ấm áp";
            } else if (feelsLike < 35) {
                return "Nóng";
            } else if (feelsLike < 40) {
                return "Rất nóng";
            } else {
                return "Cực kỳ nóng";
            }
        }
    }

    // Phương thức mô tả tầm nhìn
    private String getVisibilityDescription(double visibility) {
        if (currentLanguage.equals(LANG_ENGLISH)) {
            if (visibility >= 10) {
                return "Perfect visibility";
            } else if (visibility >= 5) {
                return "Good visibility";
            } else if (visibility >= 2) {
                return "Moderate visibility";
            } else if (visibility >= 1) {
                return "Limited visibility";
            } else if (visibility >= 0.5) {
                return "Poor visibility";
            } else {
                return "Dense fog";
            }
        } else {
            if (visibility >= 10) {
                return "Tầm nhìn hoàn toàn rõ";
            } else if (visibility >= 5) {
                return "Tầm nhìn tốt";
            } else if (visibility >= 2) {
                return "Tầm nhìn trung bình";
            } else if (visibility >= 1) {
                return "Tầm nhìn hạn chế";
            } else if (visibility >= 0.5) {
                return "Tầm nhìn kém";
            } else {
                return "Sương mù dày đặc";
            }
        }
    }

    private void updateWindAndPressure(JsonObject current) {
        // Cập nhật CompassView như cũ
        CompassView compassView = findViewById(R.id.compassView);
        if (compassView != null && current.has("wind_kph")) {
            float windSpeed = current.get("wind_kph").getAsFloat();

            // Xử lý hướng gió - API có thể trả về string hoặc degrees
            if (current.has("wind_dir")) {
                String windDir = current.get("wind_dir").getAsString();
                compassView.setWindData(windSpeed, windDir);
            } else if (current.has("wind_degree")) {
                // Nếu API trả về degrees thay vì direction string
                float windDegree = current.get("wind_degree").getAsFloat();
                compassView.setWindDataDegrees(windSpeed, windDegree);
            } else {
                // Fallback nếu không có thông tin hướng gió
                compassView.setWindData(windSpeed, "N");
            }
        }

        // Cập nhật PressureGaugeView với dữ liệu từ API
        PressureGaugeView pressureGaugeView = findViewById(R.id.pressureGaugeView);
        if (pressureGaugeView != null) {
            if (current.has("pressure_mb")) {
                // Lấy áp suất từ API (đơn vị: millibar/hPa)
                float pressureValue = current.get("pressure_mb").getAsFloat();

                // Cập nhật giá trị áp suất
                pressureGaugeView.setPressure(pressureValue);

                // Log để debug
                Log.d(TAG, "Cập nhật áp suất: " + pressureValue + " hPa");
            } else if (current.has("pressure_in")) {
                // Nếu API trả về áp suất bằng inch thì chuyển đổi sang hPa
                float pressureInch = current.get("pressure_in").getAsFloat();
                float pressureHPa = pressureInch * 33.8639f; // 1 inHg = 33.8639 hPa

                pressureGaugeView.setPressure(pressureHPa);

                Log.d(TAG, "Cập nhật áp suất (chuyển đổi từ inHg): " + pressureHPa + " hPa");
            } else {
                // Nếu không có dữ liệu áp suất, hiển thị placeholder
                pressureGaugeView.showPlaceholder();
                Log.w(TAG, "Không tìm thấy dữ liệu áp suất trong API response");
            }
        }
    }
    // Thêm method mới để xử lý trạng thái loading cho pressure gauge
    private void showPressureLoading() {
        PressureGaugeView pressureGaugeView = findViewById(R.id.pressureGaugeView);
        if (pressureGaugeView != null) {
            pressureGaugeView.showPlaceholder();
        }
    }
    private void updateAstroData(AstroData astroData) {
        try {
            String sunrise24h = convertTo24Hour(astroData.sunrise);
            String sunset24h = convertTo24Hour(astroData.sunset);
            String moonrise24h = convertTo24Hour(astroData.moonrise);
            String moonset24h = convertTo24Hour(astroData.moonset);

            tvBinhMinh.setText(sunrise24h);
            tvHoangHon.setText(sunset24h);
            tvTrangMoc.setText(moonrise24h);
            tvTrangLan.setText(moonset24h);

            if (sunArcView != null) {
                sunArcView.setSunTimes(sunrise24h, sunset24h);
            }

            if (tvMoonPhase != null) {
                String moonPhaseVietnamese = translateMoonPhase(astroData.moonPhase);
                tvMoonPhase.setText(moonPhaseVietnamese);
            }
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi cập nhật dữ liệu thiên văn: " + e.getMessage());
        }
    }

    private void updateHourlyForecastUI(List<HourlyForecast> hourlyForecasts, String currentHour) {
        int maxHours = Math.min(hourlyForecasts.size(), MAX_HOURLY_ITEMS);

        for (int i = 0; i < maxHours; i++) {
            LinearLayout hourlyItem = findHourlyItem(i + 1);
            if (hourlyItem != null) {
                updateHourlyItem(hourlyItem, hourlyForecasts.get(i), currentHour);
            }
        }
    }

    private void updateHourlyItem(LinearLayout hourlyItem, HourlyForecast forecast, String currentHour) {
        TextView tempTextView = (TextView) hourlyItem.getChildAt(0);
        ImageView iconImageView = (ImageView) hourlyItem.getChildAt(1);
        TextView timeTextView = (TextView) hourlyItem.getChildAt(2);

        tempTextView.setText(forecast.getTemperature());
        timeTextView.setText(forecast.getTime());

        // Highlight current hour
        if (currentHour != null && forecast.getTime().equals(currentHour)) {
            hourlyItem.setBackgroundResource(R.drawable.info_box_bg);
        } else {
            hourlyItem.setBackground(null);
        }

        // Load weather icon
        Glide.with(this).load(forecast.getIconUrl()).into(iconImageView);
    }

    private void updateDailyForecastUI(List<DailyForecast> forecasts) {
        int maxDays = Math.min(forecasts.size(), MAX_DAILY_ITEMS);

        for (int i = 0; i < maxDays; i++) {
            LinearLayout container = findDailyContainer(i + 1);
            if (container != null) {
                updateDailyItem(container, forecasts.get(i));
            }
        }
    }

    private void updateDailyItem(LinearLayout container, DailyForecast forecast) {
        TextView dateTextView = (TextView) container.getChildAt(0);
        ImageView iconImageView = (ImageView) container.getChildAt(1);
        TextView tempTextView = (TextView) container.getChildAt(2);

        dateTextView.setText(forecast.getDate());
        tempTextView.setText(forecast.getTemperature());

        // Load weather icon
        Glide.with(this).load(forecast.getConditionIconUrl()).into(iconImageView);
    }

    // Settings popup methods
    private void showSettingsPopup() {
        Dialog dialog = createDialog(R.layout.popup_settings);
        View view = dialog.findViewById(android.R.id.content);

        LinearLayout languageSetting = view.findViewById(R.id.language_setting);
        LinearLayout temperatureSetting = view.findViewById(R.id.temperature_setting);
        TextView currentLanguageText = view.findViewById(R.id.current_language);
        TextView currentTempUnitText = view.findViewById(R.id.current_temperature_unit);

        // Tìm và cập nhật các TextView tiêu đề trong popup_settings.xml
        TextView settingsTitle = view.findViewById(R.id.setting); // ID có thể khác
        TextView languageLabel = view.findViewById(R.id.language); // ID có thể khác
        TextView temperatureLabel = view.findViewById(R.id.temperature); // ID có thể khác

        // Hoặc nếu ID khác, thay đổi theo ID thực tế trong XML:
        // TextView settingsTitle = view.findViewById(R.id.tv_settings);
        // TextView languageLabel = view.findViewById(R.id.tv_language);
        // TextView temperatureLabel = view.findViewById(R.id.tv_temperature);

        // Cập nhật text theo ngôn ngữ hiện tại
        if (settingsTitle != null) {
            settingsTitle.setText(getLocalizedString("SETTINGS"));
        }
        if (languageLabel != null) {
            languageLabel.setText(getLocalizedString("LANGUAGE"));
        }
        if (temperatureLabel != null) {
            temperatureLabel.setText(getLocalizedString("TEMPERATURE"));
        }

        // Hiển thị cài đặt hiện tại
        currentLanguageText.setText(currentLanguage.equals(LANG_ENGLISH) ? "English" : "Tiếng Việt");
        currentTempUnitText.setText(currentTempUnit.equals(TEMP_FAHRENHEIT) ? "°F" : "°C");

        languageSetting.setOnClickListener(v -> {
            dialog.dismiss();
            showLanguagePopup();
        });

        temperatureSetting.setOnClickListener(v -> {
            dialog.dismiss();
            showTemperaturePopup();
        });

        dialog.show();
    }

    private void showLanguagePopup() {
        Dialog dialog = createDialog(R.layout.popup_language);
        View view = dialog.findViewById(android.R.id.content);

        LinearLayout englishOption = view.findViewById(R.id.english_option);
        LinearLayout vietnameseOption = view.findViewById(R.id.vietnamese_option);
        ImageView englishCheck = view.findViewById(R.id.english_check);
        ImageView vietnameseCheck = view.findViewById(R.id.vietnamese_check);

        // Hiển thị lựa chọn hiện tại
        englishCheck.setVisibility(currentLanguage.equals(LANG_ENGLISH) ? View.VISIBLE : View.GONE);
        vietnameseCheck.setVisibility(currentLanguage.equals(LANG_VIETNAMESE) ? View.VISIBLE : View.GONE);

        englishOption.setOnClickListener(v -> {
            currentLanguage = LANG_ENGLISH;
            saveLanguageSetting();
            updateLabels();
            dialog.dismiss();
            reloadWeatherData(); // Tải lại dữ liệu với ngôn ngữ mới
            Toast.makeText(this, "Language changed to English", Toast.LENGTH_SHORT).show();
        });

        vietnameseOption.setOnClickListener(v -> {
            currentLanguage = LANG_VIETNAMESE;
            saveLanguageSetting();
            updateLabels();
            dialog.dismiss();
            reloadWeatherData(); // Tải lại dữ liệu với ngôn ngữ mới
            Toast.makeText(this, "Đã chọn Tiếng Việt", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }

    private void showTemperaturePopup() {
        Dialog dialog = createDialog(R.layout.popup_temperature);
        View view = dialog.findViewById(android.R.id.content);

        LinearLayout celsiusOption = view.findViewById(R.id.celsius_option);
        LinearLayout fahrenheitOption = view.findViewById(R.id.fahrenheit_option);
        ImageView celsiusCheck = view.findViewById(R.id.celsius_check);
        ImageView fahrenheitCheck = view.findViewById(R.id.fahrenheit_check);

        // Hiển thị lựa chọn hiện tại
        celsiusCheck.setVisibility(currentTempUnit.equals(TEMP_CELSIUS) ? View.VISIBLE : View.GONE);
        fahrenheitCheck.setVisibility(currentTempUnit.equals(TEMP_FAHRENHEIT) ? View.VISIBLE : View.GONE);

        celsiusOption.setOnClickListener(v -> {
            currentTempUnit = TEMP_CELSIUS;
            saveTemperatureUnitSetting();
            dialog.dismiss();
            updateTemperatureDisplay(); // Cập nhật hiển thị nhiệt độ
            Toast.makeText(this, currentLanguage.equals(LANG_ENGLISH) ? "Selected °C" : "Đã chọn °C", Toast.LENGTH_SHORT).show();
        });

        fahrenheitOption.setOnClickListener(v -> {
            currentTempUnit = TEMP_FAHRENHEIT;
            saveTemperatureUnitSetting();
            dialog.dismiss();
            updateTemperatureDisplay(); // Cập nhật hiển thị nhiệt độ
            Toast.makeText(this, currentLanguage.equals(LANG_ENGLISH) ? "Selected °F" : "Đã chọn °F", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
    }
    private void saveLanguageSetting() {
        SharedPreferences.Editor editor = settingsPrefs.edit();
        editor.putString(KEY_LANGUAGE, currentLanguage);
        editor.apply();
    }

    private void saveTemperatureUnitSetting() {
        SharedPreferences.Editor editor = settingsPrefs.edit();
        editor.putString(KEY_TEMPERATURE_UNIT, currentTempUnit);
        editor.apply();
    }

    private void reloadWeatherData() {
        String cityName = getIntent().getStringExtra("CITY_NAME");
        if (cityName != null && !cityName.trim().isEmpty()) {
            fetchForecast(cityName);
        } else {
            fetchWeatherData();
        }
    }

    private void updateTemperatureDisplay() {
        // Cập nhật lại tất cả các hiển thị nhiệt độ với đơn vị mới
        // Bạn cần lưu trữ giá trị nhiệt độ gốc để chuyển đổi
        reloadWeatherData();
    }
    private double convertTemperature(double celsius) {
        if (currentTempUnit.equals(TEMP_FAHRENHEIT)) {
            return (celsius * 9.0 / 5.0) + 32;
        }
        return celsius;
    }

    private String formatTemperature(double celsius) {
        double temp = convertTemperature(celsius);
        String unit = currentTempUnit.equals(TEMP_FAHRENHEIT) ? "°F" : "°C";
        return String.format(Locale.getDefault(), "%.1f%s", temp, unit);
    }
    private Dialog createDialog(int layoutResId) {
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        View view = LayoutInflater.from(this).inflate(layoutResId, null);
        dialog.setContentView(view);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return dialog;
    }

    // Utility methods
    private String convertTo24Hour(String time12h) {
        try {
            SimpleDateFormat input = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            SimpleDateFormat output = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date date = input.parse(time12h);
            return output.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi chuyển đổi thời gian: " + e.getMessage());
            return time12h;
        }
    }

    private String translateMoonPhase(String englishPhase) {
        switch (englishPhase.toLowerCase()) {
            case "new moon": return "Trăng mới";
            case "waxing crescent": return "Trăng lưỡi liềm tăng";
            case "first quarter": return "Trăng tròn một phần tư";
            case "waxing gibbous": return "Trăng khuyết tăng";
            case "full moon": return "Trăng tròn";
            case "waning gibbous": return "Trăng khuyết giảm";
            case "last quarter": return "Trăng tròn ba phần tư";
            case "waning crescent": return "Trăng lưỡi liềm giảm";
            default: return englishPhase;
        }
    }

    private String formatCurrentDate(String inputDate) {
        return formatDateVietnamese(inputDate, "MMM dd, yyyy");
    }

    private String formatDate(String inputDate) {
        return formatDateVietnamese(inputDate, "MMM dd");
    }

    private String formatDateWithPattern(String inputDate, String inputPattern, String outputPattern) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat(inputPattern, Locale.ENGLISH);

            // Sử dụng locale phù hợp với ngôn ngữ hiện tại
            Locale outputLocale = currentLanguage.equals(LANG_ENGLISH) ? Locale.ENGLISH : new Locale("vi", "VN");
            SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern, outputLocale);

            Date date = inputFormat.parse(inputDate);
            return outputFormat.format(date);
        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày tháng: " + e.getMessage());
            return inputDate;
        }
    }
    private String getVietnameseMonth(int month) {
        String[] vietnameseMonths = {
                "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4", "Tháng 5", "Tháng 6",
                "Tháng 7", "Tháng 8", "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
        };
        return vietnameseMonths[month - 1];
    }
    private String formatDateVietnamese(String inputDate, String outputPattern) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date date = inputFormat.parse(inputDate);

            if (currentLanguage.equals(LANG_VIETNAMESE)) {
                // Format tùy chỉnh cho tiếng Việt
                SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault()); // Không có số 0 phía trước
                SimpleDateFormat monthFormat = new SimpleDateFormat("M", Locale.getDefault()); // Không có số 0 phía trước
                SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

                String day = dayFormat.format(date);
                String month = monthFormat.format(date);
                String year = yearFormat.format(date);

                if (outputPattern.equals("MMM dd, yyyy")) {
                    return day + " tháng " + month + ", " + year;
                } else if (outputPattern.equals("MMM dd")) {
                    return day + " tháng " + month;
                }
            }

            // Fallback cho tiếng Anh
            SimpleDateFormat outputFormat = new SimpleDateFormat(outputPattern, Locale.ENGLISH);
            return outputFormat.format(date);

        } catch (ParseException e) {
            Log.e(TAG, "Lỗi khi định dạng ngày tháng: " + e.getMessage());
            return inputDate;
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel any ongoing HTTP requests
        if (httpClient != null) {
            httpClient.dispatcher().cancelAll();
        }
    }

    // Data classes
    private static class AstroData {
        public final String sunrise;
        public final String sunset;
        public final String moonrise;
        public final String moonset;
        public final String moonPhase;

        public AstroData(String sunrise, String sunset, String moonrise, String moonset, String moonPhase) {
            this.sunrise = sunrise;
            this.sunset = sunset;
            this.moonrise = moonrise;
            this.moonset = moonset;
            this.moonPhase = moonPhase;
        }
    }

    private static class ForecastData {
        public final List<DailyForecast> dailyForecasts;
        public final List<HourlyForecast> hourlyForecasts;

        public ForecastData(List<DailyForecast> dailyForecasts, List<HourlyForecast> hourlyForecasts) {
            this.dailyForecasts = dailyForecasts;
            this.hourlyForecasts = hourlyForecasts;
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