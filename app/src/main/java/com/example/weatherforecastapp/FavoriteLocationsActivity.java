package com.example.weatherforecastapp;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.example.weatherforecastapp.api.WeatherApiService;
import com.example.weatherforecastapp.api.WeatherResponse;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.HashSet;
import java.util.Set;

public class FavoriteLocationsActivity extends BaseActivity { // Thay đổi từ AppCompatActivity thành BaseActivity
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GestureDetector gestureDetector;
    private boolean isSwipeInProgress = false;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    // SharedPreferences constants for favorite locations
    private static final String PREFS_NAME = "FavoriteLocationsPrefs";
    private static final String KEY_FAVORITE_CITIES = "favoriteCities";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.favorite_locations);

        // Khởi tạo background ngay sau setContentView
        initializeBackground();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Initialize animation
        TextView tvSlide = findViewById(R.id.tvSlide);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_left_to_right);
        tvSlide.startAnimation(pulse);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup settings button if exists
        ImageView settingsButton = findViewById(R.id.settings_button);
        if (settingsButton != null) {
            settingsButton.setOnClickListener(v -> showSettingsPopup());
        }

        // Setup gesture detector for swipe navigation
        setupGestureDetector();

        // Setup location cards
        setupLocationCards();
    }

    private void setupGestureDetector() {
        // 1. Setup gesture detector cho ScrollView (swipe trong ScrollView)
        HorizontalSwipeScrollView scrollView = findViewById(R.id.scrollView);
        scrollView.setOnHorizontalSwipeListener(() -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        });

        // 2. Setup gesture detector cho toàn bộ Activity (swipe ngoài ScrollView)
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                if (e1 == null) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            finish();
                            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // 3. Set touch listener cho root layout (để handle swipe ngoài ScrollView)
        findViewById(R.id.rootLayout).setOnTouchListener((v, event) -> {
            return gestureDetector.onTouchEvent(event);
        });
    }

    private void setupLocationCards() {
        LinearLayout locationContainer = findViewById(R.id.locationContainer);

        // Get favorite cities from SharedPreferences
        Set<String> favoriteCitiesSet = sharedPreferences.getStringSet(KEY_FAVORITE_CITIES, new HashSet<>());
        String[] favoriteCities = favoriteCitiesSet.toArray(new String[0]);

        // Add "My Location" card
        addMyLocationCard(locationContainer);

        // Add favorite location cards
        for (String city : favoriteCities) {
            addFavoriteLocationCard(locationContainer, city);
        }
    }

    private void addMyLocationCard(LinearLayout locationContainer) {
        View myLocationCard = LayoutInflater.from(this).inflate(R.layout.location_card, locationContainer, false);
        TextView tvLocationTitle = myLocationCard.findViewById(R.id.tvLocationTitle);
        TextView tvCityName = myLocationCard.findViewById(R.id.tvCityName);
        TextView tvTime = myLocationCard.findViewById(R.id.tvTime);
        TextView tvWeatherStatus = myLocationCard.findViewById(R.id.tvWeatherStatus);
        TextView tvTemperature = myLocationCard.findViewById(R.id.tvTemperature);
        TextView tvHighTemp = myLocationCard.findViewById(R.id.tvHighTemp);
        TextView tvLowTemp = myLocationCard.findViewById(R.id.tvLowTemp);

        tvLocationTitle.setText("Vị trí của tôi");
        tvCityName.setText("Đang tải...");

        requestLocationAndFetchWeather(tvCityName, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);

        // Chỉ dùng OnClickListener đơn giản
        myLocationCard.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteLocationsActivity.this, MainActivity.class);
            String cityName = tvCityName.getText().toString();

            if (cityName.equals("Đang tải...") || cityName.equals("Không xác định được vị trí") || cityName.equals("Lỗi lấy vị trí")) {
                intent.putExtra("SELECTED_CITY", "Hanoi");
                Toast.makeText(this, "Không thể lấy vị trí hiện tại, sử dụng Hà Nội", Toast.LENGTH_SHORT).show();
            } else {
                intent.putExtra("SELECTED_CITY", cityName);
            }
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        locationContainer.addView(myLocationCard);
    }

    private void addFavoriteLocationCard(LinearLayout locationContainer, String city) {
        View cardView = LayoutInflater.from(this).inflate(R.layout.location_card, locationContainer, false);

        TextView tvLocationTitleCard = cardView.findViewById(R.id.tvLocationTitle);
        TextView tvCityNameCard = cardView.findViewById(R.id.tvCityName);
        TextView tvTimeCard = cardView.findViewById(R.id.tvTime);
        TextView tvWeatherStatusCard = cardView.findViewById(R.id.tvWeatherStatus);
        TextView tvTemperatureCard = cardView.findViewById(R.id.tvTemperature);
        TextView tvHighTempCard = cardView.findViewById(R.id.tvHighTemp);
        TextView tvLowTempCard = cardView.findViewById(R.id.tvLowTemp);

        tvLocationTitleCard.setText(city);
        tvCityNameCard.setText(city);
        fetchWeather(city, tvTimeCard, tvWeatherStatusCard, tvTemperatureCard, tvHighTempCard, tvLowTempCard);

        // Chỉ dùng OnClickListener đơn giản
        cardView.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteLocationsActivity.this, MainActivity.class);
            intent.putExtra("SELECTED_CITY", city);

            Toast.makeText(this, "Chuyển đến thời tiết tại " + city, Toast.LENGTH_SHORT).show();
            Log.d("FavoriteLocations", "Using location: " + city);

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            finish();
        });

        locationContainer.addView(cardView);
    }

    private void requestLocationAndFetchWeather(TextView tvCityName, TextView tvTime, TextView tvWeatherStatus,
                                                TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        fetchWeatherByCoordinates(lat, lon, tvCityName, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);
                    } else {
                        tvCityName.setText("Không xác định được vị trí");
                    }
                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    tvCityName.setText("Lỗi lấy vị trí");
                });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (granted) {
                recreate();
            }
        }
    }

    private void fetchWeatherByCoordinates(double lat, double lon, TextView tvCityName, TextView tvTime,
                                           TextView tvWeatherStatus, TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        String latlon = lat + "," + lon;
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", latlon, 1, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    tvCityName.setText(weather.location.name);
                    String time = weather.location.localtime.split(" ")[1];
                    tvTime.setText(time);
                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvTemperature.setText(weather.current.temp_c + "°");
                    tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                    tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
    }

    private void fetchWeather(String city, TextView tvTime, TextView tvWeatherStatus, TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", city, 1, "vi");

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    String time = weather.location.localtime.split(" ")[1];
                    tvTime.setText(time);
                    tvWeatherStatus.setText(weather.current.condition.text);
                    tvTemperature.setText(weather.current.temp_c + "°");
                    tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                    tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                t.printStackTrace();
            }
        });
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
        LinearLayout vietnameseOption = view        .findViewById(R.id.vietnamese_option);
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}