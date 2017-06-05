package com.happyheadache.data;

import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.happyheadache.models.SensorData;
import com.happyheadache.models.WeatherLibData;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.happyheadache.Constants.APP_TAG;
import static com.happyheadache.Constants.FIREBASE_CHILD_SENSOR;
import static com.happyheadache.Constants.FIREBASE_CHILD_WEATHERLIB;
import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 18/10/2016.
 */

public class HourlyDataCollectionJob extends Job {

    public static final String TAG = "job_hourly_tag";

    public static void schedule() {
        schedule(true);
    }

    private static void schedule(boolean updateCurrent) {
        // Next hour
        long startMs = TimeUnit.MINUTES.toMillis(60 - Calendar.getInstance().get(Calendar.MINUTE));
        long endMs = startMs + TimeUnit.HOURS.toMillis(1);

        new JobRequest.Builder(TAG)
                .setExecutionWindow(startMs, endMs)
                .setPersisted(true)
                .setUpdateCurrent(updateCurrent)
                .build()
                .schedule();
    }

    @NonNull
    @Override
    protected Result onRunJob(Params params) {
        try {
            if (Looper.myLooper() == null) {
                Looper.prepare();
            }

            final CountDownLatch weatherLibCountDownLatch = new CountDownLatch(1);
            WeatherLibManager weatherLibManager = new WeatherLibManager(weatherLibCountDownLatch);
            weatherLibManager.startGetWeatherData(getContext());

            final CountDownLatch sensorCountDownLatch = new CountDownLatch(4);
            AndroidSensorManager sensorManager = new AndroidSensorManager(sensorCountDownLatch);
            sensorManager.startGetSensorData(getContext());

            // Wait until both jobs are finished
            weatherLibCountDownLatch.await();
            Log.d(APP_TAG, "Weather Lib data collection done.");

            sensorCountDownLatch.await();
            Log.d(APP_TAG, "Sensor data collection done.");

            // Save data to database
            WeatherLibData weatherLibData = weatherLibManager.getWeatherLibData();
            if (weatherLibData != null) {
                Log.d(APP_TAG, "Saving Weather Lib data to database...");
                String userId = getCurrentUserId();
                weatherLibData.setUserId(userId);
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                mDatabase.child(FIREBASE_CHILD_WEATHERLIB).child(userId).push().setValue(weatherLibData);
            }

            // Save data to database
            SensorData sensorData = sensorManager.getSensorData();
            if (sensorData != null) {
                Log.d(APP_TAG, "Saving Sensor data to database...");
                String userId = getCurrentUserId();
                sensorData.setUserId(userId);
                DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                mDatabase.child(FIREBASE_CHILD_SENSOR).child(userId).push().setValue(sensorData);
            }
        } catch (InterruptedException ignored) {
            Log.d(APP_TAG, ignored.getMessage());
        } finally {
            schedule(false); // don't update current, it would cancel this currently running job
        }
        return Result.SUCCESS;
    }
}
