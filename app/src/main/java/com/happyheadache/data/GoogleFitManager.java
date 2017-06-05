package com.happyheadache.data;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResult;
import com.happyheadache.models.Activity;
import com.happyheadache.models.GoogleFitData;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.happyheadache.Constants.APP_TAG;
import static java.text.DateFormat.getTimeInstance;

/**
 * Created by Alexandra Fritzen on 17/10/2016.
 */

public class GoogleFitManager {

    public interface GoogleFitDataListener {
        void googleFitConnectedAndPermissionsAcquired();

        void googleFitPermissionFailed();
    }

    private GoogleApiClient mClient = null;
    private CountDownLatch mCountDownLatch;
    private GoogleFitDataListener mListener;
    private int mClientId = 0;
    private GoogleFitData mGoogleFitData;

    private boolean mShouldOpenDialog;
    private long mStartTime;
    private long mEndTime;

    public GoogleFitManager() {
    }

    // Helper methods
    private long getStartTimeOfYesterday() {
        Calendar yesterday = Calendar.getInstance();

        yesterday.set(Calendar.DATE, yesterday.get(Calendar.DATE) - 1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);

        return yesterday.getTimeInMillis();
    }

    private long getEndTimeOfYesterday() {
        Calendar yesterday = Calendar.getInstance();

        yesterday.set(Calendar.DATE, yesterday.get(Calendar.DATE) - 1);
        yesterday.set(Calendar.HOUR_OF_DAY, 23);
        yesterday.set(Calendar.MINUTE, 59);
        yesterday.set(Calendar.SECOND, 59);
        yesterday.set(Calendar.MILLISECOND, 999);

        return yesterday.getTimeInMillis();
    }

    private long getStartTimeOfToday() {
        Calendar today = Calendar.getInstance();

        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        return today.getTimeInMillis();
    }

    private void printCalendar(Calendar cal) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Log.d(APP_TAG, dateFormat.format(cal.getTime()));
    }

    public void startGetDataForYesterday(Context context, CountDownLatch countDownLatch) {
        mGoogleFitData = new GoogleFitData();
        mCountDownLatch = countDownLatch;

        mStartTime = getStartTimeOfYesterday();
        mEndTime = getEndTimeOfYesterday();

        buildFitnessClient(context, null);
    }

    public void startGetDataForToday(Context context, CountDownLatch countDownLatch) {
        mGoogleFitData = new GoogleFitData();
        mCountDownLatch = countDownLatch;

        mStartTime = getStartTimeOfToday();
        mEndTime = System.currentTimeMillis();

        buildFitnessClient(context, null);
    }

    public void attach(Context context, FragmentActivity activity, boolean shouldOpenDialog) {
        if (context instanceof GoogleFitDataListener) {
            mListener = (GoogleFitDataListener) context;
            mShouldOpenDialog = shouldOpenDialog;
            buildFitnessClient(context, activity);
        } else {
            throw new RuntimeException(context.toString() + " must implement GoogleFitDataListener");
        }
    }

    public GoogleFitData getGoogleFitData() {
        return mGoogleFitData;
    }

    public void attach(Context context, FragmentActivity activity) {
        attach(context, activity, true);
    }

    public void disconnect() {
        Log.e(APP_TAG, "Disconnecting google fitness client...");
        if (mClient != null) {
            mClient.disconnect();
        }
    }

    private void buildFitnessClient(Context context, final FragmentActivity activity) {
        Log.e(APP_TAG, "Building fitness client...");

        GoogleApiClient.Builder builder = new GoogleApiClient.Builder(context)
                .addApi(Fitness.HISTORY_API)
                .addScope(new Scope(Scopes.FITNESS_LOCATION_READ))
                .addScope(new Scope(Scopes.FITNESS_ACTIVITY_READ))
                .addScope(new Scope(Scopes.FITNESS_BODY_READ))
                .addScope(new Scope(Scopes.FITNESS_NUTRITION_READ))
                .addScope(new Scope(Scopes.PROFILE))
                .addConnectionCallbacks(connectionCallbacks);

        if (activity != null && mShouldOpenDialog) {
            if (mClientId == 0) {
                builder.enableAutoManage(activity, mClientId, mOnConnectionFailedListener);
                mClientId++;
            } else {
                // TODO: Google API Client will not show login popup a second time - WHY? --> https://code.google.com/p/android/issues/detail?id=218157
                mListener.googleFitPermissionFailed();
            }
        } else {
            builder.addOnConnectionFailedListener(mOnConnectionFailedListener);
        }

        mClient = builder.build();
        if (activity == null || !mShouldOpenDialog) {
            mClient.connect();
        }
    }

    private final GoogleApiClient.OnConnectionFailedListener mOnConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult result) {
            Log.e(APP_TAG, "Google Play services connection failed. Cause: " + result.toString());
            if (result.getErrorCode() == ConnectionResult.CANCELED) {
                mClient.disconnect();
                mClient = null;
            }
            if (mListener != null) {
                mListener.googleFitPermissionFailed();
            } else if (mCountDownLatch != null) {
                // Count down to notify job that we're done
                long count = mCountDownLatch.getCount();
                for (int i = 0; i < count; i++) {
                    mCountDownLatch.countDown();
                }
                mGoogleFitData = null;
            }
        }
    };

    private final GoogleApiClient.ConnectionCallbacks connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected(Bundle bundle) {
            Log.e(APP_TAG, "Google API Client is connected.");

            if (mCountDownLatch == null) {
                mListener.googleFitConnectedAndPermissionsAcquired();
            } else {
                // Make calls to the Fitness APIs
                mGoogleFitData.setDateTime(getEndTimeOfYesterday());

                readYesterdayStepData(mStartTime, mEndTime);
                readYesterdayNutritionData(mStartTime, mEndTime);
                readYesterdayDistanceData(mStartTime, mEndTime);
                readYesterdayActivityData(mStartTime, mEndTime);
                readYesterdayCaloriesExpendedData(mStartTime, mEndTime);
                //readYesterdayHeartRateData(mStartTime, mEndTime);
                readYesterdayHydrationData(mStartTime, mEndTime);
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            // If your connection to the sensor gets lost at some point,
            // you'll be able to determine the reason and react to it here.
            if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_NETWORK_LOST) {
                Log.e(APP_TAG, "Connection lost. Cause: Network Lost.");
            } else if (i == GoogleApiClient.ConnectionCallbacks.CAUSE_SERVICE_DISCONNECTED) {
                Log.e(APP_TAG, "Connection lost. Reason: Service Disconnected");
            }
        }
    };

    private void readYesterdayStepData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                                printData(dataReadResult);
            }
        });
    }

    private void readYesterdayNutritionData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_NUTRITION, DataType.AGGREGATE_NUTRITION_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }

    private void readYesterdayHydrationData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HYDRATION, DataType.AGGREGATE_HYDRATION)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }

    // TODO: Body sensors only available with API 20+
    /*private void readYesterdayHeartRateData(long mStartTime, long mEndTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_HEART_RATE_BPM, DataType.AGGREGATE_HEART_RATE_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(mStartTime, mEndTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }*/

    private void readYesterdayDistanceData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_DISTANCE_DELTA, DataType.AGGREGATE_DISTANCE_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }

    private void readYesterdayCaloriesExpendedData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_CALORIES_EXPENDED, DataType.AGGREGATE_CALORIES_EXPENDED)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }

    private void readYesterdayActivityData(long startTime, long endTime) {
        DataReadRequest readRequest = new DataReadRequest.Builder()
                .aggregate(DataType.TYPE_ACTIVITY_SEGMENT, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
        Fitness.HistoryApi.readData(mClient, readRequest).setResultCallback(new ResultCallback<DataReadResult>() {
            @Override
            public void onResult(@NonNull DataReadResult dataReadResult) {
                printData(dataReadResult);
            }
        });
    }

    private void printData(DataReadResult dataReadResult) {
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(APP_TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    dumpDataSet(dataSet);
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(APP_TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                dumpDataSet(dataSet);
            }
        }
        mCountDownLatch.countDown();
    }

    private void dumpDataSet(DataSet dataSet) {
        Log.i(APP_TAG, "Data returned for Data type: " + dataSet.getDataType().getName());
        DateFormat dateFormat = getTimeInstance();

        for (DataPoint dp : dataSet.getDataPoints()) {
            if (dp.getDataType().getName().equals(DataType.TYPE_STEP_COUNT_DELTA.getName())) {
                for (Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("steps")) {
                        mGoogleFitData.setSteps(dp.getValue(field).asInt());
                    }
                }
            }

            // TODO: Nutrition, hydration

            if (dp.getDataType().getName().equals(DataType.TYPE_CALORIES_EXPENDED.getName())) {
                for (Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("calories")) {
                        mGoogleFitData.setCaloriesExpended(dp.getValue(field).asFloat());
                    }
                }
            }

            if (dp.getDataType().getName().equals(DataType.TYPE_DISTANCE_DELTA.getName())) {
                for (Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("distance")) {
                        mGoogleFitData.setDistance(dp.getValue(field).asFloat());
                    }
                }
            }

            if (dp.getDataType().getName().equals(DataType.AGGREGATE_ACTIVITY_SUMMARY.getName())) {
                Activity activity = new Activity();
                for (Field field : dp.getDataType().getFields()) {
                    if (field.getName().equals("activity")) {
                        activity.setActivityType(dp.getValue(field).asInt());
                    } else if (field.getName().equals("duration")) {
                        activity.setDuration(dp.getValue(field).asInt());
                    }
                }
                if (mGoogleFitData.getActivities() == null) {
                    mGoogleFitData.setActivities(new ArrayList<Activity>());
                }
                mGoogleFitData.getActivities().add(activity);
            }

            Log.i(APP_TAG, "Data point:");
            Log.i(APP_TAG, "\tType: " + dp.getDataType().getName());
            Log.i(APP_TAG, "\tStart: " + dateFormat.format(dp.getStartTime(TimeUnit.MILLISECONDS)));
            Log.i(APP_TAG, "\tEnd: " + dateFormat.format(dp.getEndTime(TimeUnit.MILLISECONDS)));
            for (Field field : dp.getDataType().getFields()) {
                Log.i(APP_TAG, "\tField: " + field.getName() + " Value: " + dp.getValue(field));
            }
        }
    }

}
