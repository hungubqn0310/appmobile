package com.example.weatherforecastapp.api;

import java.util.List;

public class WeatherResponse {
    public Location location;
    public Current current;
    public Forecast forecast;

    public static class Location {
        public String name;
        public String localtime;
    }

    public static class Current {
        public double temp_c;
        public Condition condition;
        public double wind_kph;
        public int humidity;
    }

    public static class Condition {
        public String text;
        public String icon;
        public int code;
    }

    public static class Forecast {
        public List<ForecastDay> forecastday;
    }

    public static class ForecastDay {
        public Day day;
    }

    public static class Day {
        public double maxtemp_c;
        public double mintemp_c;
    }
    public static class Astro {
        public String sunrise;  // ví dụ: "05:15 AM"
        public String sunset;   // ví dụ: "06:33 PM"
        public String moonrise;
        public String moonset;
        public String moon_phase;
        public String moon_illumination;
        public int is_moon_up;
        public int is_sun_up;
    }
}
