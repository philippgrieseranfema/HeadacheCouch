package com.happyheadache.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class WeatherLibData extends Data {
    private float temperature, humidity, precipitation, wind, pressure;
    private String city, condition;

    public WeatherLibData(float temperature, float humidity, float precipitation, float wind, float pressure, String city, String condition) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.precipitation = precipitation;
        this.wind = wind;
        this.pressure = pressure;
        this.city = city;
        this.condition = condition;
        this.dateTime = System.currentTimeMillis();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateTimeString = dateFormat.format(new Date());
        this.userId = getCurrentUserId();
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getPrecipitation() {
        return precipitation;
    }

    public void setPrecipitation(float precipitation) {
        this.precipitation = precipitation;
    }

    public float getWind() {
        return wind;
    }

    public void setWind(float wind) {
        this.wind = wind;
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
    }

    public String getDateTimeString() {
        return dateTimeString;
    }

    public void setDateTimeString(String dateTimeString) {
        this.dateTimeString = dateTimeString;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
