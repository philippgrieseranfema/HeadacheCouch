package com.happyheadache;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;

import com.github.paolorotolo.appintro.AppIntro2;
import com.github.paolorotolo.appintro.AppIntro2Fragment;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.happyheadache.data.GoogleFitManager;
import com.happyheadache.data.SHealthManager;

import static com.happyheadache.Constants.INITIAL_SURVEY_LINK;
import static com.happyheadache.Constants.PREFERENCE_HAS_OPENED_SURVEY;
import static com.happyheadache.Constants.getCurrentUserId;

public class IntroActivity extends AppIntro2 implements SHealthManager.SHealthDataListener, GoogleFitManager.GoogleFitDataListener {

    private SHealthManager mSHealthManager;
    private GoogleFitManager mGoogleFitManager;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();

        // Add your slide fragments here.
        // AppIntro will automatically generate the dots indicator and buttons.
        AppIntroFragment fragment = AppIntroFragment.newInstance(getString(R.string.welcome_welcomehappyheadache), getString(R.string.welcome_welcomehappyheadachedetail), R.drawable.ic_data_brain_colored, ContextCompat.getColor(this, R.color.colorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary));
        AppIntroFragment fragment2 = AppIntro2Fragment.newInstance(getString(R.string.welcome_trackyourhealth), getString(R.string.welcome_trackyourhealthdetail), R.drawable.ic_track_colored, ContextCompat.getColor(this, R.color.colorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary));
        AppIntroFragment fragment3 = AppIntro2Fragment.newInstance(getString(R.string.welcome_accessingyourdata), getString(R.string.welcome_accessingyourdatadetail), R.drawable.ic_network_colored, ContextCompat.getColor(this, R.color.colorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary));
        AppIntroFragment fragment4 = AppIntro2Fragment.newInstance(getString(R.string.welcome_getpersonalizedtips), getString(R.string.welcome_getpersonalizedtipsdetail), R.drawable.ic_healthy_brain_colored, ContextCompat.getColor(this, R.color.colorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary), ContextCompat.getColor(this, R.color.textColorPrimary));

        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(fragment);
        addSlide(fragment2);
        addSlide(fragment3);
        addSlide(fragment4);

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        setVibrate(false);

        mSHealthManager = new SHealthManager();
        mSHealthManager.attach(this, false);

        mGoogleFitManager = new GoogleFitManager();
        mGoogleFitManager.attach(this, this, false);

        askForPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, 3);

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize SharedPreferences
        final SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        boolean hasOpenedSurvey = mSharedPreferences.getBoolean(PREFERENCE_HAS_OPENED_SURVEY, false);

        // If the user hasn't opened the survey yet...
        if (!hasOpenedSurvey) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.home_survey)
                    .setMessage(R.string.home_surveydetail)
                    .setPositiveButton(R.string.home_surveyaction, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "go_to_survey_button");
                            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                            // Make a new preferences editor
                            SharedPreferences.Editor e = mSharedPreferences.edit();

                            // Edit preference to make it false because we don't want this to run again
                            e.putBoolean(PREFERENCE_HAS_OPENED_SURVEY, true);
                            e.apply();

                            String url = INITIAL_SURVEY_LINK + getCurrentUserId();
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(url));
                            startActivity(i);
                        }
                    })
                    .setNegativeButton(R.string.home_later, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            Bundle bundle = new Bundle();
                            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "later_survey_button");
                            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                        }
                    })
                    .show();
        }
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        // Do something when users tap on Skip button.

        if (mSHealthManager != null) {
            mSHealthManager.disconnect();
        }
        if (mGoogleFitManager != null) {
            mGoogleFitManager.disconnect();
        }

        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        // Do something when users tap on Done button.

        if (mSHealthManager != null) {
            mSHealthManager.disconnect();
        }
        if (mGoogleFitManager != null) {
            mGoogleFitManager.disconnect();
        }

        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);

        // Ask for permission to use health center(s)
        if (getSlides().indexOf(oldFragment) == 2) {

            mSHealthManager.attach(this, true);

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) + ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleFitManager.attach(this, this);
            }
        }
    }

    @Override
    public void googleFitConnectedAndPermissionsAcquired() {
        if (mGoogleFitManager != null) {
            mGoogleFitManager.disconnect();
        }
    }

    @Override
    public void googleFitPermissionFailed() {
        // Do nothing
    }

    @Override
    public void sHealthConnectedAndPermissionsAcquired() {
        if (mSHealthManager != null) {
            mSHealthManager.disconnect();
        }
    }

    @Override
    public void sHealthPermissionFailed() {
        // Do nothing
    }
}
