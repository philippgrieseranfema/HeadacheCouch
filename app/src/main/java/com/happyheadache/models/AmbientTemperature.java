package com.happyheadache.models;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class AmbientTemperature {
    private float temperature;
    private float humidity;
    private float altitude;

    public AmbientTemperature(float temperature, float humidity, float altitude) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.altitude = altitude;
    }

    @Override
    public String toString() {
        return "Temperature " + temperature + "°C"
                + ", humidity " + humidity + "l/h"
                + ", altitude " + altitude + "m";
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

    public float getAltitude() {
        return altitude;
    }

    public void setAltitude(float altitude) {
        this.altitude = altitude;
    }
}
