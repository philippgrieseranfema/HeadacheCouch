package com.happyheadache.models;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class Activity {
    private int activityType;
    private int duration;

    public Activity() {
    }

    public int getActivityType() {
        return activityType;
    }

    public void setActivityType(int activityType) {
        this.activityType = activityType;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
