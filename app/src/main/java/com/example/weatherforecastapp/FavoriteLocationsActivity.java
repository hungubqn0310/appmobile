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
import android.view.ViewGroup;
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FavoriteLocationsActivity extends BaseActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private GestureDetector gestureDetector;
    private boolean isSwipeInProgress = false;

    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPreferences;
    // SharedPreferences constants for favorite locations
    private static final String PREFS_NAME = "FavoriteLocationsPrefs";
    private static final String KEY_FAVORITE_CITIES = "favoriteCities";

    // Language settings
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
        setContentView(R.layout.favorite_locations);

        // Initialize settings preferences
        settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        currentLanguage = settingsPrefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE);
        currentTempUnit = settingsPrefs.getString(KEY_TEMPERATURE_UNIT, TEMP_CELSIUS);

        // Khởi tạo background ngay sau setContentView
        initializeBackground();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Cập nhật tiêu đề
        TextView tvTitle = findViewById(R.id.tvTitle);
        if (tvTitle != null) {
            tvTitle.setText(getLocalizedString("FAVORITE_LOCATIONS"));
        }
        // Initialize animation
        TextView tvSlide = findViewById(R.id.tvSlide);
        if (tvSlide != null) {
            Animation pulse = AnimationUtils.loadAnimation(this, R.anim.slide_left_to_right);
            tvSlide.startAnimation(pulse);
            tvSlide.setText(getLocalizedString("SLIDE_TO_RETURN"));
        }

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup gesture detector for swipe navigation
        setupGestureDetector();

        // Setup location cards
        setupLocationCards();
    }

    // Language localization
    private static final class LanguageStrings {
        // Vietnamese strings
        private static final Map<String, String> VI = new HashMap<String, String>() {{
            put("MY_LOCATION", "Vị trí của tôi");
            put("LOADING", "Đang tải...");
            put("LOCATION_UNAVAILABLE", "Không xác định được vị trí");
            put("LOCATION_ERROR", "Lỗi lấy vị trí");
            put("WEATHER_DATA_ERROR", "Lỗi dữ liệu thời tiết");
            put("API_ERROR", "Lỗi API thời tiết");
            put("CONNECTION_ERROR", "Lỗi kết nối API");
            put("GO_TO_WEATHER", "Chuyển đến thời tiết tại");
            put("DEFAULT_LOCATION", "Không thể lấy vị trí hiện tại, sử dụng Hà Nội");
            put("SLIDE_TO_RETURN", "Vuốt sang phải để quay lại");
            put("NO_NETWORK", "Không có kết nối mạng");
            put("FAVORITE_LOCATIONS", "Địa điểm yêu thích");
        }};

        // English strings
        private static final Map<String, String> EN = new HashMap<String, String>() {{
            put("MY_LOCATION", "My Location");
            put("LOADING", "Loading...");
            put("LOCATION_UNAVAILABLE", "Location unavailable");
            put("LOCATION_ERROR", "Location error");
            put("WEATHER_DATA_ERROR", "Weather data error");
            put("API_ERROR", "Weather API error");
            put("CONNECTION_ERROR", "API connection error");
            put("GO_TO_WEATHER", "Going to weather in");
            put("DEFAULT_LOCATION", "Unable to get current location, using Hanoi");
            put("SLIDE_TO_RETURN", "Swipe right to return →");
            put("NO_NETWORK", "No network connection");
            put("FAVORITE_LOCATIONS", "Favorite Locations");
        }};
    }

    private String getLocalizedString(String key) {
        Map<String, String> strings = currentLanguage.equals(LANG_VIETNAMESE) ?
                LanguageStrings.VI : LanguageStrings.EN;
        String value = strings.get(key);
        return value != null ? value : key;
    }

    private void setupGestureDetector() {
        // 1. Setup gesture detector cho ScrollView (swipe trong ScrollView)
        HorizontalSwipeScrollView scrollView = findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollView.setOnHorizontalSwipeListener(() -> {
                finish();
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                return true;
            });
        }

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
        View rootLayout = findViewById(R.id.rootLayout);
        if (rootLayout != null) {
            rootLayout.setOnTouchListener((v, event) -> {
                return gestureDetector.onTouchEvent(event);
            });
        }
    }

    private void setupLocationCards() {
        LinearLayout locationContainer = findViewById(R.id.locationContainer);
        if (locationContainer == null) {
            Log.e("FavoriteLocations", "locationContainer not found");
            return;
        }

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

        tvLocationTitle.setText(getLocalizedString("MY_LOCATION"));
        tvCityName.setText(getLocalizedString("LOADING"));

        requestLocationAndFetchWeather(tvCityName, tvTime, tvWeatherStatus, tvTemperature, tvHighTemp, tvLowTemp);

        // Chỉ dùng OnClickListener đơn giản
        myLocationCard.setOnClickListener(v -> {
            Intent intent = new Intent(FavoriteLocationsActivity.this, MainActivity.class);
            String cityName = tvCityName.getText().toString();

            if (cityName.equals(getLocalizedString("LOADING")) ||
                    cityName.equals(getLocalizedString("LOCATION_UNAVAILABLE")) ||
                    cityName.equals(getLocalizedString("LOCATION_ERROR"))) {
                intent.putExtra("SELECTED_CITY", "Hanoi");
                Toast.makeText(this, getLocalizedString("DEFAULT_LOCATION"), Toast.LENGTH_SHORT).show();
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

            Toast.makeText(this, getLocalizedString("GO_TO_WEATHER") + " " + city, Toast.LENGTH_SHORT).show();
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
                        tvCityName.setText(getLocalizedString("LOCATION_UNAVAILABLE"));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("FavoriteLocations", "Error getting location", e);
                    tvCityName.setText(getLocalizedString("LOCATION_ERROR"));
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
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", latlon, 1, currentLanguage);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    try {
                        // Safely update UI with null checks
                        if (weather.location != null && weather.location.name != null) {
                            tvCityName.setText(weather.location.name);
                        }

                        if (weather.location != null && weather.location.localtime != null) {
                            String[] timeParts = weather.location.localtime.split(" ");
                            if (timeParts.length > 1) {
                                tvTime.setText(timeParts[1]);
                            }
                        }

                        if (weather.current != null) {
                            if (weather.current.condition != null && weather.current.condition.text != null) {
                                tvWeatherStatus.setText(weather.current.condition.text);
                            }
                            tvTemperature.setText(weather.current.temp_c + "°");
                        }

                        if (weather.forecast != null && weather.forecast.forecastday != null &&
                                !weather.forecast.forecastday.isEmpty() && weather.forecast.forecastday.get(0).day != null) {
                            tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                            tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                        }

                    } catch (Exception e) {
                        Log.e("FavoriteLocations", "Error parsing weather data", e);
                        tvCityName.setText(getLocalizedString("WEATHER_DATA_ERROR"));
                    }
                } else {
                    Log.e("FavoriteLocations", "Weather API response not successful: " + response.code());
                    tvCityName.setText(getLocalizedString("API_ERROR"));
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("FavoriteLocations", "Weather API call failed", t);
                tvCityName.setText(getLocalizedString("CONNECTION_ERROR"));
            }
        });
    }

    private void fetchWeather(String city, TextView tvTime, TextView tvWeatherStatus, TextView tvTemperature, TextView tvHighTemp, TextView tvLowTemp) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weatherapi.com/v1/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApiService apiService = retrofit.create(WeatherApiService.class);
        Call<WeatherResponse> call = apiService.getForecast("da7aaf6a73cd4196a8121617251005", city, 1, currentLanguage);

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WeatherResponse weather = response.body();
                    try {
                        // Safely update UI with null checks
                        if (weather.location != null && weather.location.localtime != null) {
                            String[] timeParts = weather.location.localtime.split(" ");
                            if (timeParts.length > 1) {
                                tvTime.setText(timeParts[1]);
                            }
                        }
                        if (weather.current != null) {
                            if (weather.current.condition != null && weather.current.condition.text != null) {
                                tvWeatherStatus.setText(weather.current.condition.text);
                            }
                            tvTemperature.setText(weather.current.temp_c + "°");
                        }

                        if (weather.forecast != null && weather.forecast.forecastday != null &&
                                !weather.forecast.forecastday.isEmpty() && weather.forecast.forecastday.get(0).day != null) {
                            tvHighTemp.setText("C:" + weather.forecast.forecastday.get(0).day.maxtemp_c + "°");
                            tvLowTemp.setText("T:" + weather.forecast.forecastday.get(0).day.mintemp_c + "°");
                        }

                    } catch (Exception e) {
                        Log.e("FavoriteLocations", "Error parsing weather data for " + city, e);
                    }
                } else {
                    Log.e("FavoriteLocations", "Weather API response not successful for " + city + ": " + response.code());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Log.e("FavoriteLocations", "Weather API call failed for " + city, t);
            }
        });
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event) || super.onTouchEvent(event);
    }
}