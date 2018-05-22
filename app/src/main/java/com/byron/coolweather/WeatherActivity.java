package com.byron.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Xml;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.byron.coolweather.gson.Forecast;
import com.byron.coolweather.gson.Now;
import com.byron.coolweather.gson.Suggestion;
import com.byron.coolweather.gson.Weather;
import com.byron.coolweather.util.HttpUtil;
import com.byron.coolweather.util.Utility;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degressText;
    private TextView weatherInfoText;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carwashText;
    private TextView sportText;
    private ImageView bingImage;

    public SwipeRefreshLayout refreshWeather;

    public DrawerLayout drawerLayout;
    private Button navButton;

    public String weatherId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );

            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degressText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carwashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingImage = findViewById(R.id.bing_pic_img);
        refreshWeather = findViewById(R.id.refresh_weather);
        drawerLayout = findViewById(R.id.choose_city_drawer_layout);
        navButton = findViewById(R.id.nav_button);
        refreshWeather.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);

        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null) {
            Glide.with(this).load(bingPic).into(bingImage);
        } else {
            loadBingPic();
        }

        if(weatherString != null) {
            Weather weather = Utility.handleWeatherResponse(weatherString);
            weatherId = weather.basic.cid;
            showWeatherInfo(weather);
        } else {
            weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        refreshWeather.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(weatherId);
            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    public void requestWeather(final String location) {
        String weatherUrl = "https://free-api.heweather.com/s6/weather?key=c815489f2f864c31abae704aedcc9fae&location=" + location;
        weatherId = location;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshWeather.setRefreshing(false);
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败...", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshWeather.setRefreshing(false);
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败...", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        loadBingPic();
    }

    private void loadBingPic() {
        String requestBingPic = "https://cn.bing.com/HPImageArchive.aspx?idx=0&n=1";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();

                int startPos = bingPic.indexOf("<url>") + "<url>".length();
                int endPos = bingPic.indexOf("</url>");
                final String bingImageUrl = "https://cn.bing.com" + bingPic.substring(startPos, endPos);

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingImageUrl);
                editor.apply();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingImageUrl).into(bingImage);
                    }
                });
            }
        });
    }

    private void showWeatherInfo(Weather weather) {

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);

        String cityName = weather.basic.location;

        titleCity.setText(cityName);
        titleUpdateTime.setText(weather.update.loc);
        StringBuilder builder = new StringBuilder();
        if(weather.now != null) {
            Now now = weather.now;
            builder.append(now.wind_dir)
                    .append(" ")
                    .append(now.wind_sc)
                    .append("级");
        }
        degressText.setText(weather.now.cond_txt + "  " + weather.now.tmp + " ℃");
        weatherInfoText.setText(builder.toString());

        forecastLayout.removeAllViews();

        for (Forecast forecast : weather.daily_forecast) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);

            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);

            dateText.setText(forecast.date);
            infoText.setText(forecast.cond_txt_d);
            maxText.setText(forecast.tmp_max);
            minText.setText(forecast.tmp_min);

            forecastLayout.addView(view);
        }

        for (Suggestion suggestion : weather.lifestyle) {
            if("comf".equals(suggestion.type)) {
                comfortText.setText(suggestion.brf + " " + suggestion.txt);
            } else if("sport".equals(suggestion.type)) {
                sportText.setText(suggestion.brf + " " + suggestion.txt);
            } else if("cw".equals(suggestion.type)) {
                carwashText.setText(suggestion.brf + " " + suggestion.txt);
            }
        }

        weatherLayout.setVisibility(View.VISIBLE);
    }
}
