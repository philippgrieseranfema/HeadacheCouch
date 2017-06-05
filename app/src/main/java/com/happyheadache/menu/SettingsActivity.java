package com.happyheadache.menu;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.CompoundButton;
import android.widget.Switch;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.happyheadache.R;
import com.happyheadache.data.GoogleFitManager;
import com.happyheadache.data.SHealthManager;

import static com.happyheadache.Constants.PREFERENCE_GOOGLE_FIT_DISABLED;
import static com.happyheadache.Constants.PREFERENCE_S_HEALTH_DISABLED;

public class SettingsActivity extends AppCompatActivity implements SHealthManager.SHealthDataListener, GoogleFitManager.GoogleFitDataListener {

    private SHealthManager mSHealthManager;
    private GoogleFitManager mGoogleFitManager;
    private SharedPreferences mSharedPreferences;

    private boolean mFirstOpenedSHealth;
    private boolean mFirstOpenedGoogleFit;

    private Switch mSHealthSwitch;
    private Switch mGoogleFitSwitch;

    private static final int REQUEST_PERMISSION_LOCATION = 19;

    private SettingsActivity mActivity;

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Show toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_settings);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Enable up navigation (back arrow)
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mActivity = this;

        mFirstOpenedSHealth = true;
        mFirstOpenedGoogleFit = true;

        mSHealthManager = new SHealthManager();
        mSHealthManager.attach(mActivity, false);

        mGoogleFitManager = new GoogleFitManager();
        mGoogleFitManager.attach(mActivity, mActivity, false);

        //  Initialize SharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Set up switches
        mSHealthSwitch = (Switch) findViewById(R.id.switch_settings_shealth);
        mSHealthSwitch.setOnCheckedChangeListener(mSHealthSwitchOnCheckedChangeListener);
        mGoogleFitSwitch = (Switch) findViewById(R.id.switch_settings_googlefit);
        mGoogleFitSwitch.setOnCheckedChangeListener(mGoogleFitSwitchOnCheckedChangeListener);

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
    }

    private final CompoundButton.OnCheckedChangeListener mSHealthSwitchOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "s_health_switch");
            bundle.putBoolean("isChecked", isChecked);
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "s_health_switch");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            if (isChecked) {
                // Open permission dialogue
                mSHealthManager.attach(mActivity, true, true);
            } else {
                // "Disable" S Health
                // Afaik, there is no way to revoke permissions that were once given by the user
                // Therefore, we introduce a shared preference that manages the user manually disabling s health
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putBoolean(PREFERENCE_S_HEALTH_DISABLED, true);
                e.apply();
            }
        }
    };

    private final CompoundButton.OnCheckedChangeListener mGoogleFitSwitchOnCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "google_fit_switch");
            bundle.putBoolean("isChecked", isChecked);
            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "google_fit_switch");
            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

            if (isChecked) {
                // Open permission dialogue
                // TODO: Only works once if the user cancels the dialog - WHY?

                if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
                } else {
                    mGoogleFitManager.attach(mActivity, mActivity);
                }
            } else {
                // "Disable" Google Fit
                // Afaik, there is no way to revoke permissions that were once given by the user
                // Therefore, we introduce a shared preference that manages the user manually disabling google fit
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putBoolean(PREFERENCE_GOOGLE_FIT_DISABLED, true);
                e.apply();
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGoogleFitManager.attach(mActivity, mActivity);
                } else {
                    googleFitSwitchSetSecretFalse();
                    Snackbar.make(mActivity.findViewById(R.id.linearlayout_settings), "We need your location permission to use Google Fit.", Snackbar.LENGTH_LONG).show();
                }
        }
    }

    private void updateGoogleFitSwitch(boolean googleFitConnected) {
        boolean googleFitDisabled = mSharedPreferences.getBoolean(PREFERENCE_GOOGLE_FIT_DISABLED, false);

        boolean oldSwitchState = mGoogleFitSwitch.isChecked();
        boolean newSwitchState = !googleFitDisabled && googleFitConnected;

        if (newSwitchState != oldSwitchState) {
            mGoogleFitSwitch.setChecked(newSwitchState);
        }
    }

    @Override
    public void googleFitConnectedAndPermissionsAcquired() {
        if (mFirstOpenedGoogleFit) {
            mFirstOpenedGoogleFit = false;
        } else {
            SharedPreferences.Editor e = mSharedPreferences.edit();
            e.putBoolean(PREFERENCE_GOOGLE_FIT_DISABLED, false);
            e.apply();
        }

        updateGoogleFitSwitch(true);
    }

    private void googleFitSwitchSetSecretFalse() {
        mGoogleFitSwitch.setOnCheckedChangeListener(null);
        mGoogleFitSwitch.setChecked(false);
        mGoogleFitSwitch.setOnCheckedChangeListener(mGoogleFitSwitchOnCheckedChangeListener);
    }

    @Override
    public void googleFitPermissionFailed() {
        googleFitSwitchSetSecretFalse();
    }

    private void updateSHealthSwitch() {
        boolean sHealthDisabled = mSharedPreferences.getBoolean(PREFERENCE_S_HEALTH_DISABLED, false);
        boolean sHealthPermissionsGranted = mSHealthManager.arePermissionsGranted();

        boolean oldSwitchState = mSHealthSwitch.isChecked();
        boolean newSwitchState = !sHealthDisabled && sHealthPermissionsGranted;

        if (newSwitchState != oldSwitchState) {
            mSHealthSwitch.setChecked(newSwitchState);
        }
    }

    @Override
    public void sHealthConnectedAndPermissionsAcquired() {
        if (mFirstOpenedSHealth) {
            mFirstOpenedSHealth = false;

            mSHealthSwitch.setOnCheckedChangeListener(null);
            mSHealthSwitch.setChecked(true);
            mSHealthSwitch.setOnCheckedChangeListener(mSHealthSwitchOnCheckedChangeListener);
        } else {
            SharedPreferences.Editor e = mSharedPreferences.edit();
            e.putBoolean(PREFERENCE_S_HEALTH_DISABLED, false);
            e.apply();
            updateSHealthSwitch();
        }
    }

    @Override
    public void sHealthPermissionFailed() {
        mSHealthSwitch.setOnCheckedChangeListener(null);
        mSHealthSwitch.setChecked(false);
        mSHealthSwitch.setOnCheckedChangeListener(mSHealthSwitchOnCheckedChangeListener);
    }

    @Override
    public void onDestroy() {
        mSHealthManager.disconnect();
        mGoogleFitManager.disconnect();

        super.onDestroy();
    }

    @Override
    protected void onStop() {
        if (mGoogleFitManager != null) {
            mGoogleFitManager.disconnect();
        }
        super.onStop();
    }
}
