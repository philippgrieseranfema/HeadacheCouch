package com.happyheadache.data;

import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobRequest;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.happyheadache.models.GoogleFitData;
import com.happyheadache.models.SHealthData;

import java.util.Calendar;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.happyheadache.Constants.APP_TAG;
import static com.happyheadache.Constants.FIREBASE_CHILD_GOOGLEFIT;
import static com.happyheadache.Constants.FIREBASE_CHILD_SHEALTH;
import static com.happyheadache.Constants.PREFERENCE_GOOGLE_FIT_DISABLED;
import static com.happyheadache.Constants.PREFERENCE_S_HEALTH_DISABLED;
import static com.happyheadache.Constants.getCurrentUserId;

/**
 * Created by Alexandra Fritzen on 18/10/2016.
 */

public class DailyDataCollectionJob extends Job {

    public static final String TAG = "job_daily_tag";

    public static void schedule() {
        schedule(true);
    }

    private static void schedule(boolean updateCurrent) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        // 1 AM - 6 AM, ignore seconds
        long startMs = TimeUnit.MINUTES.toMillis(60 - minute) + TimeUnit.HOURS.toMillis((24 - hour) % 24);
        long endMs = startMs + TimeUnit.HOURS.toMillis(5);

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
            CountDownLatch sHealthManagerCountDownLatch = null, googleFitManagerCountDownLatch = null;
            SHealthManager sHealthManager = null;
            GoogleFitManager googleFitManager = null;

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

            // Only start data collection job if user didn't manually disable s health
            boolean sHealthDisabled = sharedPreferences.getBoolean(PREFERENCE_S_HEALTH_DISABLED, false);
            if (!sHealthDisabled) {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }

                sHealthManager = new SHealthManager();
                sHealthManagerCountDownLatch = new CountDownLatch(sHealthManager.mKeySet.size());
                sHealthManager.startGetDataForYesterday(getContext(), sHealthManagerCountDownLatch);
            }

            // Only start data collection job if user didn't manually disable google fit
            boolean googleFitDisabled = sharedPreferences.getBoolean(PREFERENCE_GOOGLE_FIT_DISABLED, false);
            if (!googleFitDisabled) {
                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }

                googleFitManager = new GoogleFitManager();
                googleFitManagerCountDownLatch = new CountDownLatch(5);
                googleFitManager.startGetDataForYesterday(getContext(), googleFitManagerCountDownLatch);
            }

            // Wait until both jobs are finished
            if (sHealthManager != null) {
                if (sHealthManagerCountDownLatch.getCount() > 0) {
                    sHealthManagerCountDownLatch.await();
                }

                Log.d(APP_TAG, "S Health Manager data collection done.");

                // Save data to database
                SHealthData sHealthData = sHealthManager.getSHealthData();
                if (sHealthData != null) {
                    Log.d(APP_TAG, "Saving S Health data to database...");
                    String userId = getCurrentUserId();
                    sHealthData.setUserId(userId);
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    mDatabase.child(FIREBASE_CHILD_SHEALTH).child(userId).push().setValue(sHealthData);
                }

                sHealthManager.disconnect();
            }
            if (googleFitManager != null) {
                if (googleFitManagerCountDownLatch.getCount() > 0) {
                    googleFitManagerCountDownLatch.await();
                }
                googleFitManagerCountDownLatch.await();

                Log.d(APP_TAG, "Google Fit Manager data collection done.");

                // Save data to database
                GoogleFitData googleFitData = googleFitManager.getGoogleFitData();
                if (googleFitData != null) {
                    Log.d(APP_TAG, "Saving Google Fit data to database...");
                    String userId = getCurrentUserId();
                    googleFitData.setUserId(userId);
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                    mDatabase.child(FIREBASE_CHILD_GOOGLEFIT).child(userId).push().setValue(googleFitData);
                }

                googleFitManager.disconnect();
            }
        } catch (InterruptedException ignored) {
            Log.d(APP_TAG, ignored.getMessage());
        } finally {
            schedule(false); // don't update current, it would cancel this currently running job
        }
        return Result.SUCCESS;
    }
}
