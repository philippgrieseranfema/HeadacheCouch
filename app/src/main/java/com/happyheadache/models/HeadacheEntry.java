package com.happyheadache.models;

import com.google.firebase.database.Exclude;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static com.happyheadache.Constants.EMPTY_STRING;
import static com.happyheadache.Constants.FIREBASE_MODEL_DATETIME;
import static com.happyheadache.Constants.FIREBASE_MODEL_DATETIMESTRING;
import static com.happyheadache.Constants.FIREBASE_MODEL_DIDTAKEPAINRELIEVERS;
import static com.happyheadache.Constants.FIREBASE_MODEL_DURATION;
import static com.happyheadache.Constants.FIREBASE_MODEL_ID;
import static com.happyheadache.Constants.FIREBASE_MODEL_INTENSITY;
import static com.happyheadache.Constants.FIREBASE_MODEL_PAINRELIEVERS;
import static com.happyheadache.Constants.FIREBASE_MODEL_SYMPTOMS;
import static com.happyheadache.Constants.FIREBASE_MODEL_USERID;
import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 13/10/2016.
 */

public class HeadacheEntry {

    private String id;
    private long dateTime;
    private String dateTimeString;
    private int duration;
    private int intensity;
    private HashMap<String, Boolean> symptoms;
    private boolean didTakePainRelievers;
    private String painRelievers;
    private String userId;

    public HeadacheEntry() {
        this(0, 12, 5, new HashMap<String, Boolean>(), false, EMPTY_STRING);
    }

    public HeadacheEntry(long dateTime, int duration, int intensity, HashMap<String, Boolean> symptoms, boolean didTakePainRelievers, String painRelievers) {
        this.dateTime = dateTime;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();
        date.setTime(dateTime);
        this.dateTimeString = dateFormat.format(date);
        this.duration = duration;
        this.intensity = intensity;
        this.symptoms = symptoms;
        this.didTakePainRelievers = didTakePainRelievers;
        this.painRelievers = painRelievers;
        this.userId = getCurrentUserId();
    }

    public HashMap<String, Boolean> getSymptoms() {
        return symptoms;
    }

    public void setSymptoms(HashMap<String, Boolean> symptoms) {
        this.symptoms = symptoms;
    }

    public boolean isDidTakePainRelievers() {
        return didTakePainRelievers;
    }

    public void setDidTakePainRelievers(boolean didTakePainRelievers) {
        this.didTakePainRelievers = didTakePainRelievers;
    }

    public String getPainRelievers() {
        return painRelievers;
    }

    public void setPainRelievers(String painRelievers) {
        this.painRelievers = painRelievers;
    }

    public long getDateTime() {
        return dateTime;
    }

    public void setDateTime(long dateTime) {
        this.dateTime = dateTime;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date = new Date();
        date.setTime(dateTime);
        this.dateTimeString = dateFormat.format(date);
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getIntensity() {
        return intensity;
    }

    public void setIntensity(int intensity) {
        this.intensity = intensity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put(FIREBASE_MODEL_ID, id);
        result.put(FIREBASE_MODEL_DATETIME, dateTime);
        result.put(FIREBASE_MODEL_DATETIMESTRING, dateTimeString);
        result.put(FIREBASE_MODEL_DURATION, duration);
        result.put(FIREBASE_MODEL_INTENSITY, intensity);
        result.put(FIREBASE_MODEL_SYMPTOMS, symptoms);
        result.put(FIREBASE_MODEL_DIDTAKEPAINRELIEVERS, didTakePainRelievers);
        result.put(FIREBASE_MODEL_PAINRELIEVERS, painRelievers);
        result.put(FIREBASE_MODEL_USERID, userId);
        return result;
    }

    public String getDateTimeString() {
        return dateTimeString;
    }

    public void setDateTimeString(String dateTimeString) {
        this.dateTimeString = dateTimeString;
    }
}
