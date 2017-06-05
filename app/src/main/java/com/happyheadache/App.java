package com.happyheadache;

import android.app.Application;

import com.evernote.android.job.JobManager;
import com.happyheadache.data.CustomJobCreator;

/**
 * Created by Alexandra Fritzen on 18/10/2016.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        JobManager.create(this).addJobCreator(new CustomJobCreator());
    }
}
