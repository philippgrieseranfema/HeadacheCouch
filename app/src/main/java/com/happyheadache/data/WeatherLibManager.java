package com.happyheadache.data;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.happyheadache.models.WeatherLibData;
import com.survivingwithandroid.weather.lib.WeatherClient;
import com.survivingwithandroid.weather.lib.WeatherConfig;
import com.survivingwithandroid.weather.lib.client.okhttp.WeatherDefaultClient;
import com.survivingwithandroid.weather.lib.exception.WeatherLibException;
import com.survivingwithandroid.weather.lib.model.CurrentWeather;
import com.survivingwithandroid.weather.lib.provider.openweathermap.OpenweathermapProviderType;
import com.survivingwithandroid.weather.lib.request.WeatherRequest;

import java.util.concurrent.CountDownLatch;

import static com.happyheadache.Constants.APP_TAG;

/**
 * Created by Alexandra Fritzen on 17/10/2016.
 */

class WeatherLibManager {

    private final CountDownLatch mCountDownLatch;
    private WeatherLibData mWeatherLibData;

    WeatherLibManager(CountDownLatch countDownLatch) {
        Log.d(APP_TAG, "WeatherLibManager created.");
        mCountDownLatch = countDownLatch;
    }

    WeatherLibData getWeatherLibData() {
        return mWeatherLibData;
    }

    void startGetWeatherData(Context context) {
        mWeatherLibData = null;

        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            try {
                WeatherConfig config = new WeatherConfig();
                config.unitSystem = WeatherConfig.UNIT_SYSTEM.M;
                config.lang = "en"; // If you want to use english
                config.maxResult = 5; // Max number of cities retrieved
                config.numDays = 6; // Max num of days in the forecast
                config.ApiKey = "19048e0052a4509d3fde18cd025f3f3b";

                WeatherClient weatherClient = (new WeatherClient.ClientBuilder())
                        .attach(context)
                        .httpClient(WeatherDefaultClient.class)
                        .provider(new OpenweathermapProviderType())
                        .config(config)
                        .build();

                weatherClient.getCurrentCondition(new WeatherRequest(loc.getLongitude(), loc.getLatitude()), weatherEventListener);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        } else {
            mCountDownLatch.countDown();
        }
    }

    private final WeatherClient.WeatherEventListener weatherEventListener = new WeatherClient.WeatherEventListener() {
        @Override
        public void onWeatherRetrieved(CurrentWeather currentWeather) {
            float currentTemp = currentWeather.weather.temperature.getTemp();
            float currentHumidity = currentWeather.weather.currentCondition.getHumidity();
            float currentPrecipitation = currentWeather.weather.rain[0].getChance();
            float currentWind = currentWeather.weather.wind.getSpeed();
            float currentPressure = currentWeather.weather.currentCondition.getPressure();
            String city = currentWeather.weather.location.getCity();
            String condition = currentWeather.weather.currentCondition.getCondition();

            Log.e(APP_TAG, city + ", "
                    + condition + ", "
                    + currentTemp + "°C, "
                    + currentHumidity + "% humidity, "
                    + currentPrecipitation + "% precipitation, "
                    + currentPressure + "hpa pressure, "
                    + currentPrecipitation + "% precipitation, "
                    + currentWind + "m/s or " + currentWind * 3.6 + "km/h wind"
            );

            // Save to weather lib data object
            mWeatherLibData = new WeatherLibData(currentTemp, currentHumidity, currentPrecipitation, currentWind, currentPressure, city, condition);
            mCountDownLatch.countDown();
        }

        @Override
        public void onWeatherError(WeatherLibException e) {
            Log.d(APP_TAG, "OverviewItem Error - parsing data");
            e.printStackTrace();
        }

        @Override
        public void onConnectionError(Throwable throwable) {
            Log.d(APP_TAG, "Connection error");
            throwable.printStackTrace();
        }
    };
}
