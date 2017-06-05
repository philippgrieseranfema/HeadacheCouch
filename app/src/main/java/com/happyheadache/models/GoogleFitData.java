package com.happyheadache.models;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class GoogleFitData extends Data {
    private int steps;
    private double distance;
    private List<Activity> activities;
    private double caloriesExpended;

    public GoogleFitData() {
        this.userId = getCurrentUserId();
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

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public List<Activity> getActivities() {
        return activities;
    }

    public void setActivities(List<Activity> activities) {
        this.activities = activities;
    }

    public double getCaloriesExpended() {
        return caloriesExpended;
    }

    public void setCaloriesExpended(double caloriesExpended) {
        this.caloriesExpended = caloriesExpended;
    }
}
