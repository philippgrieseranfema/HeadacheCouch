package com.happyheadache.models;

import java.util.concurrent.TimeUnit;

/**
 * Created by Alexandra Fritzen on 04/11/2016.
 */

public class Exercise {
    private float calories;
    private long duration;
    private int exerciseType;
    private float distance;
    private String customExerciseType;

    public Exercise(float calories, long duration, int exerciseType, float distance, String customExerciseType) {
        this.calories = calories;
        this.duration = duration;
        this.exerciseType = exerciseType;
        this.distance = distance;
        this.customExerciseType = customExerciseType;
    }

    @Override
    public String toString() {
        String humanReadable = String.format("%d hours, %d min, %d sec",
                TimeUnit.MILLISECONDS.toHours(duration),
                TimeUnit.MILLISECONDS.toMinutes(duration) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)),
                TimeUnit.MILLISECONDS.toSeconds(duration) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
        );

        String exerciseName = customExerciseType;
        if (exerciseType > 0) {
            exerciseName = exerciseType + "";
        }

        return exerciseName + " for " + humanReadable + ", " + calories + " burned calories, " + distance + "m distance";
    }

    public float getCalories() {
        return calories;
    }

    public void setCalories(float calories) {
        this.calories = calories;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public int getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(int exerciseType) {
        this.exerciseType = exerciseType;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public String getCustomExerciseType() {
        return customExerciseType;
    }

    public void setCustomExerciseType(String customExerciseType) {
        this.customExerciseType = customExerciseType;
    }
}
