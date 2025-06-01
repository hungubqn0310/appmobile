package com.example.weatherforecastapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    // Language settings
    private static final String SETTINGS_PREFS = "SettingsPrefs";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_TEMPERATURE_UNIT = "temperature_unit";
    private static final String LANG_VIETNAMESE = "vi";
    private static final String LANG_ENGLISH = "en";

    private String currentLanguage = LANG_VIETNAMESE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        // Initialize language setting
        SharedPreferences settingsPrefs = getSharedPreferences(SETTINGS_PREFS, MODE_PRIVATE);
        currentLanguage = settingsPrefs.getString(KEY_LANGUAGE, LANG_VIETNAMESE);

        // Update UI with localized strings
        updateLabels();

        // Nạp bản đồ
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Toast.makeText(this, getLocalizedString("MAP_LOAD_ERROR"), Toast.LENGTH_SHORT).show();
        }

        // Nút quay lại
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        // Xử lý nhập tên thành phố
        EditText etSearch = findViewById(R.id.etSearch);
        etSearch.setHint(getLocalizedString("SEARCH_HINT"));
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                String city = etSearch.getText().toString().trim();
                if (!city.isEmpty()) {
                    saveRecentSearch(city); // ✅ lưu recent
                    Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                    intent.putExtra("CITY_NAME", city);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(this, getLocalizedString("ENTER_CITY_NAME"), Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            return false;
        });

        // ✅ Hiển thị recent search khi mở activity
        showRecentSearches();


    }

    private void updateLabels() {
        // Update any additional labels here
        TextView tvRecentSearches = findViewById(R.id.tvRecentSearches);
        if (tvRecentSearches != null) {
            tvRecentSearches.setText(getLocalizedString("RECENT_SEARCHES"));
        }
    }

    // Language localization
    private static final class LanguageStrings {
        // Vietnamese strings
        private static final Map<String, String> VI = new HashMap<String, String>() {{
            put("CHOOSE_LOCATION", "Chọn địa điểm");
            put("RECENT_SEARCHES", "Tìm kiếm gần đây");
            put("MAP_LOAD_ERROR", "Không thể tải bản đồ");
            put("ENTER_CITY_NAME", "Vui lòng nhập tên thành phố");
            put("LOCATION_SELECTED", "Đã chọn");
            put("COORDINATES_SELECTED", "Đã chọn tọa độ");
            put("ADDRESS_ERROR", "Lỗi khi lấy địa chỉ");
            put("SEARCH_HINT", "Nhập tên thành phố...");
        }};

        // English strings
        private static final Map<String, String> EN = new HashMap<String, String>() {{
            put("CHOOSE_LOCATION", "Choose Location");
            put("RECENT_SEARCHES", "Recent Searches");
            put("MAP_LOAD_ERROR", "Unable to load map");
            put("ENTER_CITY_NAME", "Please enter a city name");
            put("LOCATION_SELECTED", "Selected");
            put("COORDINATES_SELECTED", "Selected coordinates");
            put("ADDRESS_ERROR", "Error fetching address");
            put("SEARCH_HINT", "Enter city name...");
        }};
    }

    private String getLocalizedString(String key) {
        Map<String, String> strings = currentLanguage.equals(LANG_VIETNAMESE) ?
                LanguageStrings.VI : LanguageStrings.EN;
        String value = strings.get(key);
        return value != null ? value : key;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Mặc định về Hà Nội
        LatLng hanoi = new LatLng(21.0285, 105.8542);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(hanoi, 10));

        mMap.setOnMapClickListener(latLng -> {
            mMap.clear();
            mMap.addMarker(new MarkerOptions().position(latLng).title(getLocalizedString("LOCATION_SELECTED")));

            Geocoder geocoder = new Geocoder(LocationPickerActivity.this, Locale.getDefault());
            try {
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                String cityName = null;
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);
                    cityName = address.getLocality();
                    if (cityName == null || cityName.isEmpty()) cityName = address.getSubAdminArea();
                    if (cityName == null || cityName.isEmpty()) cityName = address.getAdminArea();
                }

                String latlon = latLng.latitude + "," + latLng.longitude;

                if (cityName != null && !cityName.isEmpty()) {
                    Toast.makeText(this, getLocalizedString("LOCATION_SELECTED") + ": " + cityName, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getLocalizedString("COORDINATES_SELECTED") + ": " + latlon, Toast.LENGTH_SHORT).show();
                }

                saveRecentSearch(cityName != null ? cityName : latlon); // ✅ lưu recent theo tên nếu có

                Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                intent.putExtra("CITY_NAME", latlon); // 🔥 gọi API bằng lat,lon
                startActivity(intent);
                finish();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, getLocalizedString("ADDRESS_ERROR"), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveRecentSearch(String cityName) {
        SharedPreferences prefs = getSharedPreferences("recent_searches", MODE_PRIVATE);
        Set<String> recent = new LinkedHashSet<>(prefs.getStringSet("cities", new LinkedHashSet<>()));
        recent.remove(cityName); // tránh trùng
        recent.add(cityName);
        if (recent.size() > 5) {
            String first = recent.iterator().next();
            recent.remove(first);
        }
        prefs.edit().putStringSet("cities", recent).apply();
    }

    private List<String> getRecentSearches() {
        SharedPreferences prefs = getSharedPreferences("recent_searches", MODE_PRIVATE);
        return new ArrayList<>(prefs.getStringSet("cities", new LinkedHashSet<>()));
    }

    private void showRecentSearches() {
        LinearLayout recentContainer = findViewById(R.id.recentContainer);
        recentContainer.removeAllViews();

        List<String> recentList = getRecentSearches();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (String city : recentList) {
            View itemView = inflater.inflate(R.layout.item_recent_search, recentContainer, false);
            TextView tvCity = itemView.findViewById(R.id.tvCity);
            tvCity.setText(city);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(LocationPickerActivity.this, MainActivity.class);
                intent.putExtra("CITY_NAME", city);
                startActivity(intent);
                finish();
            });

            recentContainer.addView(itemView);
        }
    }
}