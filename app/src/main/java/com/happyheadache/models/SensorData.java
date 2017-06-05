package com.happyheadache.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class SensorData extends Data {
    private float pressure;
    private float ambientTemperature;
    private float light;
    private float relativeHumidity;

    public SensorData() {
        this.dateTime = System.currentTimeMillis();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateTimeString = dateFormat.format(new Date());
        this.userId = getCurrentUserId();
    }

    public float getPressure() {
        return pressure;
    }

    public void setPressure(float pressure) {
        this.pressure = pressure;
    }

    public float getAmbientTemperature() {
        return ambientTemperature;
    }

    public void setAmbientTemperature(float ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public float getLight() {
        return light;
    }

    public void setLight(float light) {
        this.light = light;
    }

    public float getRelativeHumidity() {
        return relativeHumidity;
    }

    public void setRelativeHumidity(float relativeHumidity) {
        this.relativeHumidity = relativeHumidity;
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
