package android.coolweather.com.coolweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.coolweather.com.coolweather.gson.Forecast;
import android.coolweather.com.coolweather.gson.Weather;
import android.coolweather.com.coolweather.service.AutoupdateService;
import android.coolweather.com.coolweather.util.HttpUtil;
import android.coolweather.com.coolweather.util.Utility;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Layout;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by Administrator on 2018/1/14.
 */

public class WeatherActivity extends Activity{
    private ScrollView weatherLayout;
    private TextView titleCity,titleUpdateTime,degreeText,weatherInfoText,aqiText,pm25Text,comforText,carWashText,sportText;
    private LinearLayout forecastLayout;
    private ImageView bingPicImg;
    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;
    public DrawerLayout drawerLayout;
    private Button navButton;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);
        //初始化个控件
        bingPicImg = findViewById(R.id.bing_pic_img);
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comforText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        drawerLayout =findViewById(R.id.drawer_layout);
        navButton = findViewById(R.id.nav_button);     //打开滑动菜单, 选择城市

        swipeRefreshLayout = findViewById(R.id.swipe_refresh); //下拉刷新功能
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);//下拉刷新功能
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        if (weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        }else{
            //无缓存, 去服务器获取
            //String weatherId = getIntent().getStringExtra("weather_id");
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });
        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        String bingpic = prefs.getString("bing_pic",null);
        if (bingpic != null){
            Glide.with(this).load(bingpic).into(bingPicImg);
        }else{
            loadBingPic();
        }
    }
    //根据天气 id 请求天气信息
    public void requestWeather(final String weatherId){
        String weatherUrl ="http://guolin.tech/api/weather?cityid="+weatherId+"&key=0c2b99f90347466e843bedb55c3bbb11";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this,"获取天气信息失败",Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
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
                            if (weather != null){
                                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                                editor.putString("weather",responseText);
                                editor.apply();
                                mWeatherId = weather.basic.weatherId;
                                showWeatherInfo(weather);
                            }else{
                                Toast.makeText(WeatherActivity.this, "获取信息失败", Toast.LENGTH_SHORT).show();
                            }
                            swipeRefreshLayout.setRefreshing(false);
                        }
                    });
            }
        });
        loadBingPic();
    }

    //加载必应 每日一图
    private void loadBingPic(){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                 final String bingPic = response.body().string();
                 SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                 editor.putString("bing_pic",bingPic);
                 editor.apply();
                 runOnUiThread(new Runnable() {
                     @Override
                     public void run() {
                         Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                     }
                 });
            }
        });
    }
    //处理展示 Weather 实体类中的数据
    public void showWeatherInfo(Weather weather){
            String cityName = weather.basic.cityName;
            String updateTime =weather.basic.update.updateTime.split(" ")[1];
            String degree = weather.now.temperature+"℃";
            String weatherInfo = weather.now.more.info;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            for (Forecast forecast : weather.forecastList){
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item,forecastLayout,false);
                TextView dateText =view.findViewById(R.id.date_text);
                TextView infoText =view.findViewById(R.id.info_text);
                TextView maxText = view.findViewById(R.id.max_text);
                TextView minText = view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.more.info);
                maxText.setText(forecast.temperature.max);
                minText.setText(forecast.temperature.min);
                forecastLayout.addView(view);
            }
            if (weather.aqi != null){
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
            }
            String comfort = "舒适度:"+weather.suggestion.comfort.info;
            String carWash ="洗车指数:"+weather.suggestion.carWash.info;
            String sport ="运动建议:"+weather.suggestion.sport.info;
            comforText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            weatherLayout.setVisibility(View.VISIBLE);
            //下面两行代码,执行自动更新天气
            Intent intent = new Intent(this, AutoupdateService.class);
             startActivity(intent);
    }
}
