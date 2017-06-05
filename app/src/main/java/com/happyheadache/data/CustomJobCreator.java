package com.happyheadache.data;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;

/**
 * Created by Alexandra Fritzen on 17/10/2016.
 */

public class CustomJobCreator implements JobCreator {

    @Override
    public Job create(String tag) {
        switch (tag) {
            case DailyDataCollectionJob.TAG:
                return new DailyDataCollectionJob();
            case HourlyDataCollectionJob.TAG:
                return new HourlyDataCollectionJob();
            default:
                return null;
        }
    }
}
