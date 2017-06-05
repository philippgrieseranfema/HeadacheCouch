package com.happyheadache.models;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class Data {
    public long dateTime;
    public String dateTimeString;
    public String userId;

    public Data() {}

    public Data(long dateTime, String dateTimeString, String userId) {
        this.dateTime = dateTime;
        this.dateTimeString = dateTimeString;
        this.userId = userId;
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
