package com.happyheadache.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class SHealthData extends Data {
    private int steps;
    private int caffeine;
    private int water;
    private List<Food> foods;
    private List<Exercise> exercises;
    private Sleep sleep;
    private AmbientTemperature ambientTemperature;

    public SHealthData() {
        this.dateTime = System.currentTimeMillis();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        this.dateTimeString = dateFormat.format(new Date());
        this.userId = getCurrentUserId();
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public void setCaffeine(int caffeine) {
        this.caffeine = caffeine;
    }

    public void setWater(int water) {
        this.water = water;
    }

    public void setFoods(List<Food> foods) {
        this.foods = foods;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }

    public void setSleep(Sleep sleep) {
        this.sleep = sleep;
    }

    public void setAmbientTemperature(AmbientTemperature ambientTemperature) {
        this.ambientTemperature = ambientTemperature;
    }

    public int getSteps() {
        return steps;
    }

    public int getCaffeine() {
        return caffeine;
    }

    public int getWater() {
        return water;
    }

    public List<Food> getFoods() {
        return foods;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public Sleep getSleep() {
        return sleep;
    }

    public AmbientTemperature getAmbientTemperature() {
        return ambientTemperature;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(dateTime);
        this.dateTimeString = dateFormat.format(cal.getTime());
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
