package com.happyheadache.home;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.happyheadache.IntroActivity;
import com.happyheadache.R;
import com.happyheadache.data.DailyDataCollectionJob;
import com.happyheadache.data.HourlyDataCollectionJob;
import com.happyheadache.headachehistory.HeadacheHistoryActivity;
import com.happyheadache.menu.LoginActivity;
import com.happyheadache.menu.PrivacyNoticeActivity;
import com.happyheadache.menu.SettingsActivity;
import com.happyheadache.newheadache.NewHeadacheActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Set;
import java.util.TimeZone;

import static com.happyheadache.Constants.APP_TAG;
import static com.happyheadache.Constants.EMAIL_TYPE;
import static com.happyheadache.Constants.EMPTY_STRING;
import static com.happyheadache.Constants.INITIAL_SURVEY_LINK;
import static com.happyheadache.Constants.PREFERENCE_FIRST_START;
import static com.happyheadache.Constants.PREFERENCE_HAS_OPENED_SURVEY;
import static com.happyheadache.Constants.PREFERENCE_LOGIN;
import static com.happyheadache.Constants.PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT;
import static com.happyheadache.Constants.getCurrentUserId;

public class HomeActivity extends AppCompatActivity {

    // Enumeration to reference tabs within code without numbers
    public enum mTab {
        TAB_TRIGGERS(0), TAB_MY_HELP(1), TAB_STATS(2);

        final int numTab;

        mTab(int num) {
            this.numTab = num;
        }

        public int getValue() {
            return this.numTab;
        }

        public static mTab toMTab(int val) {
            mTab retMTab = null;
            for (mTab tempTab : mTab.values()) {
                if (tempTab.getValue() == val) {
                    retMTab = tempTab;
                    break;
                }
            }
            return retMTab;
        }
    }

    private FloatingActionButton mNewHeadacheTimerFab;
    private HomeActivity mHomeActivity;
    private LinearLayout mLinearLayout;
    private SharedPreferences mSharedPreferences;
    private HomeViewPager mViewPager;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseAnalytics mFirebaseAnalytics;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        mHomeActivity = this;

        mLinearLayout = (LinearLayout) findViewById(R.id.linearlayout_home);

        // Show toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_home);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Initiate firebase authentication
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user == null) {
                    // Create anonymous account
                    mAuth.signInAnonymously().addOnCompleteListener(mHomeActivity, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (!task.isSuccessful()) {
                                Log.w(APP_TAG, "signInAnonymously", task.getException());
                            }
                        }
                    });
                }
                invalidateOptionsMenu();
            }
        };

        // (Re-)schedule all jobs
        scheduleWeatherLibJob();
        scheduleDailyJob();

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize pager adapter and view pager to enable tabs and swiping on screen as navigation
        HomePagerAdapter mHomePagerAdapter = new HomePagerAdapter(getSupportFragmentManager());
        mViewPager = (HomeViewPager) findViewById(R.id.homeviewpager_home);
        mViewPager.setAdapter(mHomePagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                recordFragment();
            }
        });
        recordFragment();

        // Set up tab layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout_home);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        // Set up tabs
        TabLayout.Tab triggersTab = tabLayout.getTabAt(mTab.TAB_TRIGGERS.getValue());
        TabLayout.Tab myHelpTab = tabLayout.getTabAt(mTab.TAB_MY_HELP.getValue());
        TabLayout.Tab statsTab = tabLayout.getTabAt(mTab.TAB_STATS.getValue());

        ArrayList<TabLayout.Tab> tabs = new ArrayList<>(Arrays.asList(triggersTab, myHelpTab, statsTab));
        ArrayList<Integer> tabIcons = new ArrayList<>(Arrays.asList(R.drawable.ic_trigger, R.drawable.ic_medkit, R.drawable.ic_health));
        int tintColor = Color.parseColor(getResources().getString(0 + R.color.textColorPrimary));

        for (TabLayout.Tab tab : tabs) {
            tab.setIcon(tabIcons.get(tabs.indexOf(tab)));
            Drawable icon = tab.getIcon();
            if (icon != null) {
                icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }
        }

        // Initialize SharedPreferences
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

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

                            startActivity(getSurveyIntent());
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

        boolean isFirstStart = mSharedPreferences.getBoolean(PREFERENCE_FIRST_START, true);

        // If the activity has never started before...
        if (isFirstStart) {
            // Open 'Welcome' screens
            Intent i = new Intent(HomeActivity.this, IntroActivity.class);
            startActivity(i);

            // Make a new preferences editor
            SharedPreferences.Editor e = mSharedPreferences.edit();

            // Edit preference to make it false because we don't want this to run again
            e.putBoolean(PREFERENCE_FIRST_START, false);
            e.apply();
        }


        // Set up backward and forward buttons
        mNewHeadacheTimerFab = (FloatingActionButton) findViewById(R.id.floatingactionbutton_home_newheadachetimer);
        mNewHeadacheTimerFab.setOnClickListener(newHeadacheTimerFabListener);

        // Create a new boolean and preference and set it to false
        boolean newHeadacheTimerOn = mSharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0) != 0;
        int fabIcon = R.drawable.ic_timer_accent_24dp;
        if (newHeadacheTimerOn) {
            fabIcon = R.drawable.ic_timer_off_accent_24dp;
        }
        mNewHeadacheTimerFab.setImageResource(fabIcon);
    }

    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void recordFragment() {
        String name = EMPTY_STRING;
        switch (mTab.toMTab(mViewPager.getCurrentItem())) {
            case TAB_TRIGGERS:
                name = "triggers";
                break;
            case TAB_MY_HELP:
                name = "my_help";
                break;
            case TAB_STATS:
                name = "stats";
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mViewPager.getCurrentItem() + EMPTY_STRING);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, name + "_tab");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private Intent getSurveyIntent() {
        String url = INITIAL_SURVEY_LINK + getCurrentUserId();
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        return i;
    }

    private void scheduleDailyJob() {
        // (Re-)schedule daily data collection job if it doesn't already exist
        Set<JobRequest> scheduledJobs = JobManager.instance().getAllJobRequestsForTag(DailyDataCollectionJob.TAG);
        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(DailyDataCollectionJob.TAG);
        if (scheduledJobs.size() == 0 && jobRequests.size() == 0) {
            Log.e(APP_TAG, "Scheduling daily job...");
            new JobRequest.Builder(DailyDataCollectionJob.TAG)
                    .setExecutionWindow(1L, 10L)
                    .setPersisted(true)
                    .build()
                    .schedule();
        }
    }

    private void scheduleWeatherLibJob() {
        // (Re-)schedule hourly data collection job if it doesn't already exist
        Set<Job> scheduledJobs = JobManager.instance().getAllJobsForTag(HourlyDataCollectionJob.TAG);
        Set<JobRequest> jobRequests = JobManager.instance().getAllJobRequestsForTag(HourlyDataCollectionJob.TAG);
        if (scheduledJobs.size() == 0 && jobRequests.size() == 0) {
            Log.e(APP_TAG, "Scheduling hourly job...");
            new JobRequest.Builder(HourlyDataCollectionJob.TAG)
                    .setExecutionWindow(1L, 10L)
                    .setPersisted(true)
                    .build()
                    .schedule();
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);

        // Show either 'Login/Sign Up' or 'Logout'
        MenuItem logoutItem = menu.findItem(R.id.item_menu_logout);
        MenuItem loginSignupItem = menu.findItem(R.id.item_menu_loginsignup);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null && !user.getEmail().equals(EMPTY_STRING)) {
            logoutItem.setVisible(true);
            loginSignupItem.setVisible(false);
        } else {
            logoutItem.setVisible(false);
            loginSignupItem.setVisible(true);
        }

        return true;
    }

    public void onGroupItemClick(MenuItem item) {
        Intent intent = null;
        String itemName = EMPTY_STRING;
        switch (item.getItemId()) {
            case R.id.item_menu_loginsignup:
                SharedPreferences.Editor e = mSharedPreferences.edit();
                e.putBoolean(PREFERENCE_LOGIN, true);
                e.apply();

                intent = new Intent(this, LoginActivity.class);
                itemName = "login_signup";
                break;
            case R.id.item_menu_logout:
                FirebaseAuth.getInstance().signOut();

                itemName = "logout";

                break;
            case R.id.item_menu_settings:
                intent = new Intent(this, SettingsActivity.class);

                itemName = "settings";

                break;
            case R.id.item_menu_feedback:
                // Let user send feedback via email
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                emailIntent.setType(EMAIL_TYPE);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.feedback_subject));
                emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.feedback_message));
                emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{getString(R.string.feedback_email)});
                intent = Intent.createChooser(emailIntent, getString(R.string.feedback_send));

                itemName = "feedback";

                break;
            case R.id.item_menu_privacynotice:
                intent = new Intent(this, PrivacyNoticeActivity.class);
                itemName = "privacy_policy";
                break;
            case R.id.item_menu_opensurvey:
                intent = getSurveyIntent();
                itemName = "survey";
                break;
        }


        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, itemName);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, itemName + "_menu");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        if (intent != null) {
            startActivity(intent);
        }
    }

    public void openNewHeadache(View view) {
        Intent intent = new Intent(this, NewHeadacheActivity.class);

        // Create a new boolean and preference and set it to false
        boolean newHeadacheTimerOn = mSharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0) != 0;

        Bundle bundle = new Bundle();
        bundle.putBoolean("is_timer_on", newHeadacheTimerOn);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "new_headache");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "new_headache_button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        if (newHeadacheTimerOn) {
            mNewHeadacheTimerFab.setImageResource(R.drawable.ic_timer_accent_24dp);
        }

        startActivity(intent);
    }

    public void openHeadacheHistory(View view) {
        Intent intent = new Intent(this, HeadacheHistoryActivity.class);


        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "headache_history");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "headache_history_button");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

        startActivity(intent);
    }

    private final View.OnClickListener newHeadacheTimerFabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            SharedPreferences.Editor e = mSharedPreferences.edit();

            boolean newHeadacheTimerOn = mSharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0) != 0;
            if (!newHeadacheTimerOn) {
                // Set starting point of headache and change icon on fab
                e.putLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, Calendar.getInstance(TimeZone.getDefault()).getTimeInMillis());
                e.apply();

                mNewHeadacheTimerFab.setImageResource(R.drawable.ic_timer_off_accent_24dp);

                // Create snack bar
                Snackbar.make(mLinearLayout, R.string.home_newheadachetimerstarted, Snackbar.LENGTH_LONG).setAction(R.string.home_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mNewHeadacheTimerFab.setImageResource(R.drawable.ic_timer_accent_24dp);
                        SharedPreferences.Editor e = mSharedPreferences.edit();
                        e.putLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0);
                        e.apply();
                    }
                }).show();
            } else {
                openNewHeadache(null);
            }
        }
    };

    private final TabLayout.ViewPagerOnTabSelectedListener onTabSelectedListener = new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setSwipeable(tab.getPosition() != mTab.TAB_STATS.getValue());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            mViewPager.setSwipeable(tab.getPosition() != mTab.TAB_STATS.getValue());
        }

    };

    public class HomePagerAdapter extends FragmentPagerAdapter {

        HomePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (mTab.toMTab(position)) {
                case TAB_TRIGGERS:
                    return new TriggersFragment();
                case TAB_MY_HELP:
                    return new MyHelpFragment();
                case TAB_STATS:
                    return new StatsFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (mTab.toMTab(position)) {
                case TAB_TRIGGERS:
                    return getString(R.string.home_triggers);
                case TAB_MY_HELP:
                    return getString(R.string.home_myhelp);
                case TAB_STATS:
                    return getString(R.string.home_stats);
                default:
                    return null;
            }
        }
    }
}
