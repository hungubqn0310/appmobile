package com.example.weatherforecastapp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.weatherforecastapp.FavoriteLocationsActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.bumptech.glide.Glide;
import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.content.pm.PackageManager;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import android.text.Html;
import android.text.Spanned;

public class MainActivity extends BaseActivity { // Thay đổi từ AppCompatActivity thành BaseActivity
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
    private GestureDetector gestureDetector;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "FavoriteLocationsPrefs";
    private static final String KEY_FAVORITE_CITIES = "favoriteCities";
    TextView tvCity, tvDate, tvTemperature, tvWeatherStatus, tvWind, tvHumidity;
    ImageView ivNotification, ivWeatherIcon;
    ImageView ivLove;
    View notificationBadge;
    FrameLayout notificationContainer;
    FrameLayout rainContainer;
    RainView rainView;
    private Animation loadingAnimation;
    TextView tvWindLabel, tvHumidityLabel; // Thêm dòng này
    TextView tvSlide;
    Button btnForecast, btnMyLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Thêm sau setContentView
        initializeSettings();
        // Khởi tạo animation loading
        loadingAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_loading);
        // Khởi tạo background ngay sau setContentView
        initializeBackground();
        // Khởi tạo labels ban đầu
        updateLabels();
        TextView tvSlide = findViewById(R.id.tvSlide);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_right_to_left);
        tvSlide.startAnimation(pulse);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        Button btnMyLocation = findViewById(R.id.btnMyLocation);
        btnMyLocation.setOnClickListener(v -> {
            if (checkLocationPermission()) {
                getLastLocationAndFetchWeather();
            } else {
                requestLocationPermission();
            }
        });

        // Khởi tạo ivLove
        ivLove = findViewById(R.id.ivLove);
        final Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.scale_animation);

        // Gán ID từ layout
        tvCity = findViewById(R.id.tvCity);
        tvDate = findViewById(R.id.tvDate);
        tvTemperature = findViewById(R.id.tvTemperature);
        tvWeatherStatus = findViewById(R.id.tvWeatherStatus);
        tvWind = findViewById(R.id.tvWind);
        tvHumidity = findViewById(R.id.tvHumidity);
        ivNotification = findViewById(R.id.ivNotification);
        ivWeatherIcon = findViewById(R.id.ivWeatherIcon);
        btnForecast = findViewById(R.id.btnForecast);
        notificationContainer = findViewById(R.id.notificationContainer);
        notificationBadge = findViewById(R.id.notificationBadge);
        rainContainer = findViewById(R.id.rainContainer);
        tvWindLabel = findViewById(R.id.tvWindLabel); // Thêm dòng này
        tvHumidityLabel = findViewById(R.id.tvHumidityLabel); // Thêm dòng này


        btnForecast = findViewById(R.id.btnForecast);

        // Khởi tạo hiệu ứng mưa
        setupRainEffect();
        updateLabels();
        // Luôn hiển thị red dot khi mở app
        notificationBadge.setVisibility(View.VISIBLE);

        // Kiểm tra Intent từ FavoriteLocationsActivity
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.getBooleanExtra("USE_CURRENT_LOCATION", false)) {
                // Sử dụng vị trí hiện tại
                if (checkLocationPermission()) {
                    getLastLocationAndFetchWeather();
                } else {
                    requestLocationPermission();
                }
            } else if (intent.hasExtra("SELECTED_CITY")) {
                // Lấy tên thành phố từ Intent
                String selectedCity = intent.getStringExtra("SELECTED_CITY");
                tvCity.setText(selectedCity);
                fetchWeather(selectedCity);
            } else {
                // Mặc định: sử dụng thành phố từ Intent trước đó hoặc fallback
                String city = intent.getStringExtra("CITY_NAME");
                if (city == null || city.isEmpty()) {
                    city = "Hanoi"; // fallback mặc định
                }
                tvCity.setText(city);
                fetchWeather(city);
            }
        } else {
            // Mặc định: sử dụng thành phố Hanoi
            String city = "Hanoi";
            tvCity.setText(city);
            fetchWeather(city);
        }

        // Kiểm tra trạng thái yêu thích ban đầu
        Set<String> favoriteCities = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
        ivLove.setSelected(favoriteCities.contains(tvCity.getText().toString()));

        // Xử lý nhấp vào icon love
        // Trong ivLove.setOnClickListener
        ivLove.setOnClickListener(v -> {
            v.startAnimation(scaleAnimation);
            Set<String> updatedFavorites = new HashSet<>(sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>()));
            String currentCity = tvCity.getText().toString();

            String message;
            if (ivLove.isSelected()) {
                updatedFavorites.remove(currentCity);
                message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Removed " + currentCity + " from favorites" :
                        "Đã gỡ " + currentCity + " khỏi địa điểm yêu thích";
            } else {
                updatedFavorites.add(currentCity);
                message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Added " + currentCity + " to favorites" :
                        "Đã thêm " + currentCity + " vào địa điểm yêu thích";
            }

            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            ivLove.setSelected(!ivLove.isSelected());
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_FAVORITE_CITIES, updatedFavorites);
            editor.apply();
        });

        // Click để mở bản đồ chọn vị trí
        tvCity.setOnClickListener(v -> openLocationPicker());

        // Click thông báo
        // Click thông báo - thêm kiểm tra
        // Trong notificationContainer.setOnClickListener
        notificationContainer.setOnClickListener(v -> {
            String tempText = tvTemperature.getText().toString();
            String weatherCondition = tvWeatherStatus.getText().toString();

            if (tempText.equals("--°") || weatherCondition.equals("Đang tải...") ||
                    weatherCondition.equals("Loading...") || weatherCondition.isEmpty()) {

                String message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Loading weather data, please try again later" :
                        "Đang tải dữ liệu thời tiết, vui lòng thử lại sau";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            } else {
                showNotificationPopup();
            }
            notificationBadge.setVisibility(View.GONE);
        });

        // Mở Forecast
        btnForecast.setOnClickListener(v -> openForecastReport());

        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX < 0) {
                            Intent intent = new Intent(MainActivity.this, FavoriteLocationsActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                            return true;
                        }
                    }
                }
                return false;
            }
        });
    }
    private void initializeSettings() {
        settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        currentLanguage = settingsPrefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE);
        currentTempUnit = settingsPrefs.getString(KEY_TEMPERATURE_UNIT, TEMP_CELSIUS);
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
        return String.format(Locale.getDefault(), "%.0f%s", temp, unit);
    }
    // Thêm method kiểm tra kết nối mạng
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
    // Thêm method hiển thị loading
    private void showLoading() {
        ivWeatherIcon.setImageResource(R.drawable.ic_loading);
        ivWeatherIcon.startAnimation(loadingAnimation);
    }

    // Thêm method ẩn loading
    private void hideLoading() {
        ivWeatherIcon.clearAnimation();
    }
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }

    private void setupRainEffect() {
        rainView = new RainView(this);
        rainContainer.addView(rainView);
        rainContainer.setVisibility(View.GONE);
    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        }, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getLastLocationAndFetchWeather();
            } else {
                String message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Location permission denied, using Hanoi" :
                        "Quyền truy cập vị trí bị từ chối, sử dụng Hà Nội";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                tvCity.setText("Hanoi");
                fetchWeather("Hanoi");
            }
        }
    }

    private void getLastLocationAndFetchWeather() {
        if (!checkLocationPermission()) {
            Log.d("MainActivity", "Location permissions not granted");
            return;
        }

        Log.d("MainActivity", "Attempting to get last location");
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                Log.d("MainActivity", "Location received: Lat=" + location.getLatitude() + ", Lon=" + location.getLongitude());
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                fetchWeatherByCoordinates(latitude, longitude);
            } else {
                Log.d("MainActivity", "Location is null");
                String message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Cannot get current location, using Hanoi" :
                        "Không thể lấy vị trí hiện tại, sử dụng Hà Nội";
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                tvCity.setText("Hanoi");
                fetchWeather("Hanoi");
            }
        }).addOnFailureListener(e -> {
            Log.e("MainActivity", "Failed to get location: " + e.getMessage());
            String message = currentLanguage.equals(LANG_ENGLISH) ?
                    "Failed to get location, using Hanoi: " + e.getMessage() :
                    "Lấy vị trí thất bại, sử dụng Hà Nội: " + e.getMessage();
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            tvCity.setText("Hanoi");
            fetchWeather("Hanoi");
        });
    }

    private void fetchWeatherByCoordinates(double latitude, double longitude) {
        if (!isNetworkAvailable()) {
            showLoading();
            String message = currentLanguage.equals(LANG_ENGLISH) ?
                    "No internet connection. Please check again!" :
                    "Không có kết nối mạng. Vui lòng kiểm tra lại!";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        String query = latitude + "," + longitude;

        // Sử dụng ngôn ngữ hiện tại
        String lang = currentLanguage.equals(LANG_ENGLISH) ? "en" : "vi";
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", query, 1, lang);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hideLoading();
                    WeatherResponse weather = response.body();
                    updateWeatherUI(weather);
                } else {
                    String message = currentLanguage.equals(LANG_ENGLISH) ?
                            "Failed to get weather data" : "Lấy dữ liệu thời tiết thất bại";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                String message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Failed to get weather data: " + t.getMessage() :
                        "Lấy dữ liệu thời tiết thất bại: " + t.getMessage();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchWeather(String city) {
        if (!isNetworkAvailable()) {
            showLoading();
            String message = currentLanguage.equals(LANG_ENGLISH) ?
                    "No internet connection. Please check again!" :
                    "Không có kết nối mạng. Vui lòng kiểm tra lại!";
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            return;
        }

        showLoading();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        String lang = currentLanguage.equals(LANG_ENGLISH) ? "en" : "vi";
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", city, 1, lang);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    hideLoading();
                    WeatherResponse weather = response.body();
                    updateWeatherUI(weather);
                } else {
                    String message = currentLanguage.equals(LANG_ENGLISH) ?
                            "Failed to get weather data" : "Lấy dữ liệu thời tiết thất bại";
                    Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                String message = currentLanguage.equals(LANG_ENGLISH) ?
                        "Failed to get weather data: " + t.getMessage() :
                        "Lấy dữ liệu thời tiết thất bại: " + t.getMessage();
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateWeatherUI(WeatherResponse weather) {
        tvCity.setText(weather.location.name);

        // Format ngày theo ngôn ngữ
        String dateText = currentLanguage.equals(LANG_ENGLISH) ?
                "Today, " + weather.location.localtime :
                "Hôm nay, " + weather.location.localtime;
        tvDate.setText(dateText);

        // Format nhiệt độ theo đơn vị
        tvTemperature.setText(formatTemperature(weather.current.temp_c));
        tvWeatherStatus.setText(weather.current.condition.text);

        // Cập nhật label và giá trị cho Wind
        String windLabel = currentLanguage.equals(LANG_ENGLISH) ? "Wind Speed |" : "Tốc độ gió |";
        tvWindLabel.setText(windLabel);
        tvWind.setText(weather.current.wind_kph + " km/h");

        // Cập nhật label và giá trị cho Humidity
        String humidityLabel = currentLanguage.equals(LANG_ENGLISH) ? "Humidity |" : "Độ ẩm |";
        tvHumidityLabel.setText(humidityLabel);
        tvHumidity.setText(weather.current.humidity + "%");

        String iconUrl = "https:" + weather.current.condition.icon.replace("64x64", "128x128");
        Glide.with(MainActivity.this)
                .load(iconUrl)
                .into(ivWeatherIcon);

        // Cập nhật trạng thái yêu thích
        Set<String> favoriteCities = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
        ivLove.setSelected(favoriteCities.contains(weather.location.name));

        updateBackground(weather.location.localtime);
        updateRainEffect(weather.current.condition.text);
    }
    private void updateLabels() {
        // Cập nhật các label theo ngôn ngữ hiện tại
        String windLabel = currentLanguage.equals(LANG_ENGLISH) ? "Wind Speed:" : "Tốc độ gió:";
        String humidityLabel = currentLanguage.equals(LANG_ENGLISH) ? "Humidity:" : "Độ ẩm:";

        if (tvWindLabel != null) {
            tvWindLabel.setText(windLabel);
        }
        if (tvHumidityLabel != null) {
            tvHumidityLabel.setText(humidityLabel);
        }

        // Slide text - Đảm bảo tìm được element
        TextView tvSlide = findViewById(R.id.tvSlide);
        if (tvSlide != null) {
            String slideText = currentLanguage.equals(LANG_ENGLISH) ?
                    "← Swipe left for favorites" : "← Trượt để sang địa điểm yêu thích";
            tvSlide.setText(slideText);
        }

        // Forecast button - Đảm bảo tìm được element
        Button btnForecast = findViewById(R.id.btnForecast);
        if (btnForecast != null) {
            String forecastText = currentLanguage.equals(LANG_ENGLISH) ?
                    "Forecast Information" : "Thông tin dự báo";
            btnForecast.setText(forecastText);
        }

        // My Location button - Đảm bảo tìm được element
        Button btnMyLocation = findViewById(R.id.btnMyLocation);
        if (btnMyLocation != null) {
            String locationText = currentLanguage.equals(LANG_ENGLISH) ?
                    "My Location" : "Vị trí của tôi";
            btnMyLocation.setText(locationText);
        }
    }
    public void refreshLanguage() {
        initializeSettings();
        updateLabels();

        // Reload weather data với ngôn ngữ mới
        String currentCity = tvCity.getText().toString();
        if (currentCity != null && !currentCity.isEmpty() &&
                (!currentCity.equals("--") && !currentCity.equals("Loading...") && !currentCity.equals("Đang tải..."))) {
            fetchWeather(currentCity);
        }
    }
    private void updateRainEffect(String weatherCondition) {
        boolean isRaining = weatherCondition.toLowerCase().contains("mưa") ||
                weatherCondition.toLowerCase().contains("rain");

        if (isRaining) {
            rainContainer.setVisibility(View.VISIBLE);
            rainView.startRain();
            int intensity = 150;
            if (weatherCondition.toLowerCase().contains("nhẹ") ||
                    weatherCondition.toLowerCase().contains("light")) {
                intensity = 80;
            } else if (weatherCondition.toLowerCase().contains("to") ||
                    weatherCondition.toLowerCase().contains("heavy")) {
                intensity = 250;
            }
            rainView.setRainIntensity(intensity);
        } else {
            rainContainer.setVisibility(View.GONE);
            rainView.stopRain();
        }
    }

    private void showNotificationPopup() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.notification_popup, null);
        bottomSheetDialog.setContentView(sheetView);

        // Lấy các thành phần trong layout
        ImageView ivClose = sheetView.findViewById(R.id.ivClosePopup);
        TextView tvNotificationTitle = sheetView.findViewById(R.id.tvNotificationTitle); // Lấy từ popup
        TextView tvClothingSuggestion = sheetView.findViewById(R.id.tvClothingSuggestion);
        TextView tvHealthAdvice = sheetView.findViewById(R.id.tvHealthAdvice);
        TextView tvWeatherImpact = sheetView.findViewById(R.id.tvWeatherImpact);

        // Cập nhật title theo ngôn ngữ
        String notificationTitle = currentLanguage.equals(LANG_ENGLISH) ?
                "Weather Notifications" : "Thông báo thời tiết";
        tvNotificationTitle.setText(notificationTitle);

        try {
            String tempText = tvTemperature.getText().toString();
            String weatherCondition = tvWeatherStatus.getText().toString();
            String humidityText = tvHumidity.getText().toString();
            String windText = tvWind.getText().toString();

            if (tempText.equals("--°") || tempText.isEmpty() ||
                    weatherCondition.equals("Đang tải...") || weatherCondition.equals("Loading...") || weatherCondition.isEmpty() ||
                    humidityText.equals("--%") || humidityText.isEmpty() ||
                    windText.equals("-- km/h") || windText.isEmpty()) {

                showNoDataNotification(tvClothingSuggestion, tvHealthAdvice, tvWeatherImpact);
            } else {
                // Parse temperature considering current unit
                double temperature = parseTemperatureWithUnit(tempText);
                int humidity = parseHumidity(humidityText);
                double windSpeed = parseWindSpeed(windText);

                String clothingSuggestion = getClothingSuggestion(temperature, weatherCondition, humidity);
                tvClothingSuggestion.setText(fromHtml(clothingSuggestion));

                String healthAdvice = getHealthAdvice(temperature, humidity, weatherCondition);
                tvHealthAdvice.setText(fromHtml(healthAdvice));

                String weatherImpact = getWeatherImpact(temperature, weatherCondition, windSpeed, humidity);
                tvWeatherImpact.setText(fromHtml(weatherImpact));
            }
        } catch (Exception e) {
            Log.e("MainActivity", "Error parsing weather data: " + e.getMessage());
            showErrorNotification(tvClothingSuggestion, tvHealthAdvice, tvWeatherImpact);
        }

        ivClose.setOnClickListener(v -> bottomSheetDialog.dismiss());
        bottomSheetDialog.show();
        notificationBadge.setVisibility(View.GONE);
    }
    private double parseTemperatureWithUnit(String tempText) {
        try {
            String numericPart = tempText.replace("°F", "").replace("°C", "").replace("°", "");
            double temp = Double.parseDouble(numericPart);

            // Nếu đang hiển thị Fahrenheit, chuyển về Celsius để tính toán
            if (currentTempUnit.equals(TEMP_FAHRENHEIT)) {
                temp = (temp - 32) * 5.0 / 9.0;
            }
            return temp;
        } catch (NumberFormatException e) {
            return 25.0; // Giá trị mặc định
        }
    }
    // Phương thức parse nhiệt độ an toàn
    private double parseTemperature(String tempText) {
        try {
            return Double.parseDouble(tempText.replace("°", ""));
        } catch (NumberFormatException e) {
            return 25.0; // Giá trị mặc định
        }
    }

    // Phương thức parse độ ẩm an toàn
    private int parseHumidity(String humidityText) {
        try {
            return Integer.parseInt(humidityText.replace("%", ""));
        } catch (NumberFormatException e) {
            return 50; // Giá trị mặc định
        }
    }

    // Phương thức parse tốc độ gió an toàn
    private double parseWindSpeed(String windText) {
        try {
            return Double.parseDouble(windText.split(" ")[0]);
        } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
            return 10.0; // Giá trị mặc định
        }
    }
    // Hiển thị thông báo khi không có dữ liệu
    private void showNoDataNotification(TextView tvClothingSuggestion, TextView tvHealthAdvice, TextView tvWeatherImpact) {
        String noDataMessage;
        if (currentLanguage.equals(LANG_ENGLISH)) {
            noDataMessage = "📡 <b>No weather data available</b><br><br>" +
                    "Please check your internet connection and try again to get:<br>" +
                    "• Clothing suggestions<br>" +
                    "• Health advice<br>" +
                    "• Weather impact forecast";
        } else {
            noDataMessage = "📡 <b>Không có dữ liệu thời tiết</b><br><br>" +
                    "Vui lòng kiểm tra kết nối mạng và thử lại để nhận được:<br>" +
                    "• Gợi ý trang phục phù hợp<br>" +
                    "• Lời khuyên sức khỏe<br>" +
                    "• Dự báo tác động thời tiết";
        }

        tvClothingSuggestion.setText(fromHtml(noDataMessage));
        tvHealthAdvice.setText(fromHtml(""));
        tvWeatherImpact.setText(fromHtml(""));
    }

    // Hiển thị thông báo lỗi
    private void showErrorNotification(TextView tvClothingSuggestion, TextView tvHealthAdvice, TextView tvWeatherImpact) {
        String errorMessage;
        if (currentLanguage.equals(LANG_ENGLISH)) {
            errorMessage = "⚠️ <b>Data processing error</b><br><br>" +
                    "An error occurred while processing weather information.<br>" +
                    "Please try again later.";
        } else {
            errorMessage = "⚠️ <b>Lỗi xử lý dữ liệu</b><br><br>" +
                    "Đã xảy ra lỗi khi xử lý thông tin thời tiết.<br>" +
                    "Vui lòng thử lại sau.";
        }

        tvClothingSuggestion.setText(fromHtml(errorMessage));
        tvHealthAdvice.setText(fromHtml(""));
        tvWeatherImpact.setText(fromHtml(""));
    }

    // Phương thức hỗ trợ để xử lý HTML trên cả phiên bản Android cũ và mới
    @SuppressWarnings("deprecation")
    private Spanned fromHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            return Html.fromHtml(html);
        }
    }

    private String getClothingSuggestion(double temperature, String weatherCondition, int humidity) {
        StringBuilder suggestion = new StringBuilder();

        if (currentLanguage.equals(LANG_ENGLISH)) {
            suggestion.append("📝 <b>Clothing suggestions:</b><br>");

            // Temperature-based suggestions
            if (temperature >= 30) {
                suggestion.append("• Light cotton t-shirt, breathable<br>");
                suggestion.append("• Shorts/light skirt<br>");
                suggestion.append("• Wide-brimmed hat for sun protection");

                if (humidity > 70) {
                    suggestion.append("<br>• Choose breathable cotton fabric due to high humidity");
                }
            } else if (temperature >= 20) {
                suggestion.append("• Shirt or light t-shirt<br>");
                suggestion.append("• Long pants/long skirt");
            } else if (temperature >= 10) {
                suggestion.append("• Light jacket or cardigan<br>");
                suggestion.append("• Long pants<br>");
                suggestion.append("• Light scarf");
            } else {
                suggestion.append("• Thick jacket, layered clothing<br>");
                suggestion.append("• Wool hat, gloves, scarf<br>");
                suggestion.append("• Boots");
            }

            // Weather condition additions
            if (weatherCondition.toLowerCase().contains("rain")) {
                suggestion.append("<br>• Bring umbrella/raincoat<br>");
                suggestion.append("• Waterproof shoes");
            } else if (weatherCondition.toLowerCase().contains("sunny")) {
                suggestion.append("<br>• UV protection sunglasses<br>");
                suggestion.append("• SPF 50+ sunscreen");
            }
        } else {
            suggestion.append("📝 <b>Gợi ý trang phục:</b><br>");

            // Gợi ý trang phục dựa trên nhiệt độ
            if (temperature >= 30) {
                suggestion.append("• Áo thun cotton nhẹ, thoáng khí<br>");
                suggestion.append("• Quần short/váy nhẹ<br>");
                suggestion.append("• Mũ rộng vành để che nắng");

                if (humidity > 70) {
                    suggestion.append("<br>• Chọn vải cotton thoáng khí vì độ ẩm cao");
                }
            } else if (temperature >= 20) {
                suggestion.append("• Áo sơ mi hoặc áo thun nhẹ<br>");
                suggestion.append("• Quần dài/váy dài mỏng");
            } else if (temperature >= 10) {
                suggestion.append("• Áo khoác nhẹ hoặc cardigan<br>");
                suggestion.append("• Quần dài<br>");
                suggestion.append("• Khăn quàng cổ mỏng");
            } else {
                suggestion.append("• Áo khoác dày, đa lớp<br>");
                suggestion.append("• Mũ len, găng tay, khăn quàng cổ<br>");
                suggestion.append("• Giày bốt");
            }

            // Bổ sung dựa trên điều kiện thời tiết
            if (weatherCondition.toLowerCase().contains("mưa") ||
                    weatherCondition.toLowerCase().contains("rain")) {
                suggestion.append("<br>• Mang theo ô/áo mưa<br>");
                suggestion.append("• Giày không thấm nước");
            } else if (weatherCondition.toLowerCase().contains("nắng") ||
                    weatherCondition.toLowerCase().contains("sunny")) {
                suggestion.append("<br>• Kính râm chống tia UV<br>");
                suggestion.append("• Kem chống nắng SPF 50+");
            }
        }

        return suggestion.toString();
    }

    private String getHealthAdvice(double temperature, int humidity, String weatherCondition) {
        StringBuilder advice = new StringBuilder();

        if (currentLanguage.equals(LANG_ENGLISH)) {
            advice.append("❤️ <b>Health advice:</b><br>");

            // Heat Index calculation
            double heatIndex = temperature;
            if (temperature > 27 && humidity > 40) {
                heatIndex = temperature + 0.05 * humidity;
            }

            // Heat-based warnings
            if (heatIndex > 40) {
                advice.append("• <b>WARNING:</b> Extreme heat, avoid outdoor activities!<br>");
                advice.append("• High risk of heat stroke<br>");
                advice.append("• Drink plenty of water (3-4 liters/day)");
            } else if (heatIndex > 35) {
                advice.append("• Limit outdoor activities from 11am-3pm<br>");
                advice.append("• Drink at least 2-3 liters of water/day<br>");
                advice.append("• Rest frequently in shade");
            } else if (heatIndex > 30) {
                advice.append("• Drink enough water (2 liters/day)<br>");
                advice.append("• Apply sunscreen when going outside");
            } else if (temperature < 10) {
                advice.append("• Keep body warm, especially head and feet<br>");
                advice.append("• Avoid sudden temperature changes");
            }

            // Weather-based advice
            if (weatherCondition.toLowerCase().contains("rain")) {
                advice.append("<br>• Be careful of slippery roads<br>");
                advice.append("• Avoid prolonged exposure to wet conditions");
            }

            // Allergy advice
            if (humidity > 70) {
                advice.append("<br>• People with pollen allergies should be cautious due to high humidity");
            }
        } else {
            advice.append("❤️ <b>Lời khuyên sức khỏe:</b><br>");

            // Chỉ số nhiệt (Heat Index) đơn giản
            double heatIndex = temperature;
            if (temperature > 27 && humidity > 40) {
                heatIndex = temperature + 0.05 * humidity;
            }

            // Cảnh báo dựa trên chỉ số nhiệt
            if (heatIndex > 40) {
                advice.append("• <b>CẢNH BÁO:</b> Nhiệt độ cực cao, tránh hoạt động ngoài trời!<br>");
                advice.append("• Nguy cơ say nắng, sốc nhiệt cao<br>");
                advice.append("• Uống nhiều nước (3-4 lít/ngày)");
            } else if (heatIndex > 35) {
                advice.append("• Hạn chế hoạt động ngoài trời từ 11h-15h<br>");
                advice.append("• Uống ít nhất 2-3 lít nước/ngày<br>");
                advice.append("• Nghỉ ngơi thường xuyên trong bóng râm");
            } else if (heatIndex > 30) {
                advice.append("• Uống đủ nước (2 lít/ngày)<br>");
                advice.append("• Bôi kem chống nắng khi ra ngoài");
            } else if (temperature < 10) {
                advice.append("• Giữ ấm cơ thể, đặc biệt là đầu và bàn chân<br>");
                advice.append("• Tránh thay đổi nhiệt độ đột ngột");
            }

            // Lời khuyên dựa trên điều kiện thời tiết
            if (weatherCondition.toLowerCase().contains("mưa") ||
                    weatherCondition.toLowerCase().contains("rain")) {
                advice.append("<br>• Cẩn thận đường trơn trượt<br>");
                advice.append("• Tránh để cơ thể bị ướt kéo dài");
            }

            // Thêm lời khuyên về dị ứng nếu trời nhiều gió và độ ẩm cao
            if (humidity > 70) {
                advice.append("<br>• Người bị dị ứng phấn hoa cần đề phòng do độ ẩm cao");
            }
        }

        return advice.toString();
    }

    private String getWeatherImpact(double temperature, String weatherCondition, double windSpeed, int humidity) {
        StringBuilder impact = new StringBuilder();

        if (currentLanguage.equals(LANG_ENGLISH)) {
            impact.append("🔍 <b>Weather impact forecast:</b><br>");

            // Traffic impact assessment
            impact.append("• <b>Traffic:</b> ");
            if (weatherCondition.toLowerCase().contains("rain")) {
                impact.append("Be careful of slippery roads, reduced visibility");

                if (weatherCondition.toLowerCase().contains("heavy")) {
                    impact.append(", possible local flooding");
                }
            } else if (windSpeed > 20) {
                impact.append("Strong winds, drive carefully");
            } else {
                impact.append("Normal conditions, smooth travel");
            }
            impact.append("<br>");

            // Outdoor activities assessment
            impact.append("• <b>Outdoor activities:</b><br>");

            // Activity scoring
            int exerciseScore = getActivityScore(temperature, humidity, weatherCondition, "exercise");
            int picnicScore = getActivityScore(temperature, humidity, weatherCondition, "picnic");
            int swimmingScore = getActivityScore(temperature, humidity, weatherCondition, "swimming");

            impact.append("  - Exercise: " + getScoreEmojiEnglish(exerciseScore) + "<br>");
            impact.append("  - Picnic: " + getScoreEmojiEnglish(picnicScore) + "<br>");
            impact.append("  - Swimming: " + getScoreEmojiEnglish(swimmingScore));

            // Best time for activities
            if (temperature > 30) {
                impact.append("<br>• <b>Best time for activities:</b> Early morning or after 5pm");
            } else if (temperature < 10) {
                impact.append("<br>• <b>Best time for activities:</b> 10am-3pm when temperature is highest");
            }
        } else {
            impact.append("🔍 <b>Dự báo tác động:</b><br>");

            // Đánh giá tác động đến giao thông
            impact.append("• <b>Giao thông:</b> ");
            if (weatherCondition.toLowerCase().contains("mưa") ||
                    weatherCondition.toLowerCase().contains("rain")) {
                impact.append("Cẩn thận đường trơn, tầm nhìn giảm");

                if (weatherCondition.toLowerCase().contains("to") ||
                        weatherCondition.toLowerCase().contains("heavy")) {
                    impact.append(", có thể ngập úng cục bộ");
                }
            } else if (windSpeed > 20) {
                impact.append("Gió mạnh, lái xe cẩn thận");
            } else {
                impact.append("Bình thường, đi lại thuận lợi");
            }
            impact.append("<br>");

            // Đánh giá tác động đến hoạt động ngoài trời
            impact.append("• <b>Hoạt động ngoài trời:</b><br>");

            // Chấm điểm các hoạt động
            int exerciseScore = getActivityScore(temperature, humidity, weatherCondition, "exercise");
            int picnicScore = getActivityScore(temperature, humidity, weatherCondition, "picnic");
            int swimmingScore = getActivityScore(temperature, humidity, weatherCondition, "swimming");

            impact.append("  - Tập thể dục: " + getScoreEmoji(exerciseScore) + "<br>");
            impact.append("  - Dã ngoại: " + getScoreEmoji(picnicScore) + "<br>");
            impact.append("  - Bơi lội: " + getScoreEmoji(swimmingScore));

            // Thời gian tốt nhất cho hoạt động
            if (temperature > 30) {
                impact.append("<br>• <b>Thời điểm tốt nhất để hoạt động:</b> Sáng sớm hoặc sau 17h");
            } else if (temperature < 10) {
                impact.append("<br>• <b>Thời điểm tốt nhất để hoạt động:</b> 10h-15h khi nhiệt độ cao nhất");
            }
        }

        return impact.toString();
    }

    private int getActivityScore(double temperature, int humidity, String weatherCondition, String activityType) {
        int score = 5; // Điểm trung bình

        // Điều chỉnh theo nhiệt độ
        if (activityType.equals("exercise")) {
            if (temperature > 35) score -= 4;
            else if (temperature > 30) score -= 3;
            else if (temperature > 25) score -= 1;
            else if (temperature > 15 && temperature <= 25) score += 2;
            else if (temperature < 5) score -= 2;
        } else if (activityType.equals("picnic")) {
            if (temperature > 35) score -= 3;
            else if (temperature > 30) score -= 2;
            else if (temperature > 20 && temperature <= 28) score += 3;
            else if (temperature < 15) score -= 2;
        } else if (activityType.equals("swimming")) {
            if (temperature > 30) score += 3;
            else if (temperature > 25) score += 2;
            else if (temperature < 25) score -= 3;
        }

        // Điều chỉnh theo độ ẩm
        if (humidity > 80) score -= 2;
        else if (humidity > 70) score -= 1;

        // Điều chỉnh theo thời tiết
        if (weatherCondition.toLowerCase().contains("mưa") ||
                weatherCondition.toLowerCase().contains("rain")) {
            score -= 3;

            // Bơi lội không bị ảnh hưởng nhiều bởi mưa
            if (activityType.equals("swimming")) score += 1;
        }

        // Giới hạn điểm từ 1-10
        return Math.max(1, Math.min(10, score));
    }

    private String getScoreEmoji(int score) {
        if (score >= 8) return "Rất tốt ("+score+"/10) 👍";
        else if (score >= 6) return "Tốt ("+score+"/10) 👌";
        else if (score >= 4) return "Trung bình ("+score+"/10) 😐";
        else return "Không phù hợp ("+score+"/10) 👎";
    }
    private String getScoreEmojiEnglish(int score) {
        if (score >= 8) return "Excellent ("+score+"/10) 👍";
        else if (score >= 6) return "Good ("+score+"/10) 👌";
        else if (score >= 4) return "Average ("+score+"/10) 😐";
        else return "Not suitable ("+score+"/10) 👎";
    }

    private void openLocationPicker() {
        Intent intent = new Intent(MainActivity.this, LocationPickerActivity.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }

    private void openForecastReport() {
        String currentCity = tvCity.getText().toString();
        Intent intent = new Intent(MainActivity.this, ForecastReportActivity.class);
        intent.putExtra("CITY_NAME", currentCity);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Luôn reload settings khi quay lại activity
        String oldLanguage = currentLanguage;
        String oldTempUnit = currentTempUnit;

        // Reload settings từ SharedPreferences
        initializeSettings();

        // Kiểm tra xem có thay đổi không
        boolean languageChanged = !oldLanguage.equals(currentLanguage);
        boolean tempUnitChanged = !oldTempUnit.equals(currentTempUnit);

        if (languageChanged || tempUnitChanged) {
            // Cập nhật labels ngay lập tức
            updateLabels();

            // Reload dữ liệu thời tiết với settings mới nếu cần
            String currentCity = tvCity.getText().toString();
            if (currentCity != null && !currentCity.isEmpty() &&
                    (!currentCity.equals("--") && !currentCity.equals("Loading...") && !currentCity.equals("Đang tải..."))) {
                fetchWeather(currentCity);
            }
        } else {
            // Vẫn cập nhật labels để đảm bảo UI nhất quán
            updateLabels();
        }

        if (rainContainer.getVisibility() == View.VISIBLE) {
            rainView.startRain();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        rainView.stopRain();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rainView != null) {
            rainView.stopRain();
        }
    }
}