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

    public class Current {
        public double temp_c;
        public double temp_f;
        public double feelslike_c;
        public double feelslike_f;
        public int humidity;
        public double vis_km;
        public double precip_mm;
        public double wind_kph;
        public int wind_degree;
        public String wind_dir;
        public double pressure_mb;
        public Condition condition;
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
        public String sunrise;
        public String sunset;
        public String moonrise;
        public String moonset;
        public String moon_phase;
        public String moon_illumination;
        public int is_moon_up;
        public int is_sun_up;
    }
}
