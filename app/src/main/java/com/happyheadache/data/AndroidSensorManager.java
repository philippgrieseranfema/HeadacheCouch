package com.happyheadache.data;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.happyheadache.models.SensorData;

import java.util.concurrent.CountDownLatch;

import static com.happyheadache.Constants.APP_TAG;

/**
 * Created by Alexandra Fritzen on 17/10/2016.
 */

class AndroidSensorManager {

    private SensorManager mSensorManager;
    private final CountDownLatch mCountDownLatch;
    private SensorData mSensorData;

    AndroidSensorManager(CountDownLatch countDownLatch) {
        Log.d(APP_TAG, "AndroidSensorManager created.");
        mCountDownLatch = countDownLatch;
    }

    void startGetSensorData(Context context) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorData = new SensorData();

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) != null) {
            mSensorManager.registerListener(pressureListener, mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mCountDownLatch.countDown();
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY) != null) {
            mSensorManager.registerListener(relativeHumidityListener, mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mCountDownLatch.countDown();
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            mSensorManager.registerListener(lightListener, mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mCountDownLatch.countDown();
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE) != null) {
            mSensorManager.registerListener(ambientTemperatureListener, mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE), SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            mCountDownLatch.countDown();
        }
    }

    SensorData getSensorData() {
        return mSensorData;
    }

    private final SensorEventListener pressureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float millibarsOfPressure = sensorEvent.values[0];
            Log.e(APP_TAG, "Pressure: " + millibarsOfPressure + "mbar");
            mSensorManager.unregisterListener(this);

            // Save to sensor data object
            mSensorData.setPressure(millibarsOfPressure);
            mCountDownLatch.countDown();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private final SensorEventListener ambientTemperatureListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float ambientTemperature = sensorEvent.values[0];
            Log.e(APP_TAG, "Ambient temperature: " + ambientTemperature + "°C");
            mSensorManager.unregisterListener(this);

            // Save to sensor data object
            mSensorData.setAmbientTemperature(ambientTemperature);
            mCountDownLatch.countDown();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private final SensorEventListener lightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float light = sensorEvent.values[0];
            Log.e(APP_TAG, "Light: " + light + "lx");
            mSensorManager.unregisterListener(this);

            // Save to sensor data object
            mSensorData.setLight(light);
            mCountDownLatch.countDown();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };

    private final SensorEventListener relativeHumidityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            float relativeHumidity = sensorEvent.values[0];
            Log.e(APP_TAG, "Relative humidity: " + relativeHumidity + "%");
            mSensorManager.unregisterListener(this);

            // Save to sensor data object
            mSensorData.setRelativeHumidity(relativeHumidity);
            mCountDownLatch.countDown();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
        }
    };
}
