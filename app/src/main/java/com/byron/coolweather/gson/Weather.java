package com.byron.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    public Basic basic;
    public List<Forecast> daily_forecast;
    public Now now;
    public String status;
    public Update update;
    public List<Suggestion> lifestyle;
}
