package com.happyheadache.newheadache;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.NavUtils;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;
import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.happyheadache.Constants.EMPTY_STRING;
import static com.happyheadache.Constants.FIREBASE_CHILD_HEADACHES;
import static com.happyheadache.Constants.INTENT_EDIT_HEADACHE_ENTRY;
import static com.happyheadache.Constants.INTENT_RETURN_TO_HEADACHE_HISTORY;
import static com.happyheadache.Constants.PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT;
import static com.happyheadache.Constants.getCurrentUserId;

public class NewHeadacheActivity extends AppCompatActivity implements BasicsFragment.OnFragmentInteractionListener, MedicineFragment.OnFragmentInteractionListener, SymptomsFragment.OnFragmentInteractionListener {

    // Enumeration to reference tabs within code without numbers
    public enum mTab {
        TAB_BASICS(0), TAB_MEDICINE(1), TAB_SYMPTOMS(2);

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

    private ViewPager mViewPager;
    private NewHeadacheActivity mHeadacheActivity;
    private TabLayout mTabLayout;
    private FloatingActionButton mBackwardFab;
    private FloatingActionButton mForwardFab;
    private HeadacheEntry mHeadacheEntry;
    private boolean mIsEditMode;
    private boolean mReturnToHeadacheHistory;
    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_newheadache);

        mHeadacheActivity = this;
        String serializabledHeadacheEntry = getIntent().getStringExtra(INTENT_EDIT_HEADACHE_ENTRY);
        if (serializabledHeadacheEntry == null) {
            mIsEditMode = false;
            mHeadacheEntry = new HeadacheEntry();
        } else {
            mIsEditMode = true;
            mHeadacheEntry = (new Gson()).fromJson(serializabledHeadacheEntry, HeadacheEntry.class);
        }
        mReturnToHeadacheHistory = getIntent().getBooleanExtra(INTENT_RETURN_TO_HEADACHE_HISTORY, false);

        // Check if timer on 'Home' page was used & get Calendar and duration if so
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor e = sharedPreferences.edit();

        long startTimeMillis = sharedPreferences.getLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0);

        // Reset values & timer
        e.putLong(PREFERENCE_NEW_HEADACHE_TIMER_STARTING_POINT, 0);
        e.apply();

        if (startTimeMillis != 0) {
            long endTimeMillis = Calendar.getInstance().getTimeInMillis();

            mHeadacheEntry.setDateTime(startTimeMillis);

            int duration = (int) TimeUnit.MILLISECONDS.toHours(endTimeMillis - startTimeMillis);
            mHeadacheEntry.setDuration(duration);
        }

        // Show toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_newheadache);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Enable up navigation (back arrow)
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setTitle(mIsEditMode ? R.string.headachehistory_editentry : R.string.all_newheadache);
        }

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize pager adapter and view pager to enable tabs and swiping on screen as navigation
        NewHeadachePagerAdapter mNewHeadachePagerAdapter = new NewHeadachePagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.viewpager_newheadache);
        mViewPager.setAdapter(mNewHeadachePagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                recordFragment();
            }
        });
        recordFragment();

        // Set up tab layout
        mTabLayout = (TabLayout) findViewById(R.id.tablayout_newheadache);
        mTabLayout.setupWithViewPager(mViewPager);

        TabLayout.Tab basicsTab = mTabLayout.getTabAt(mTab.TAB_BASICS.getValue());
        TabLayout.Tab medicineTab = mTabLayout.getTabAt(mTab.TAB_MEDICINE.getValue());
        TabLayout.Tab symptomsTab = mTabLayout.getTabAt(mTab.TAB_SYMPTOMS.getValue());

        ArrayList<TabLayout.Tab> tabs = new ArrayList<>(Arrays.asList(basicsTab, medicineTab, symptomsTab));
        ArrayList<Integer> tabIcons = new ArrayList<>(Arrays.asList(R.drawable.ic_info_accent_24dp, R.drawable.ic_painkiller, R.drawable.ic_mood_bad_white_24dp));
        int tintColor = Color.parseColor(getResources().getString(0 + R.color.textColorPrimary));

        for (TabLayout.Tab tab : tabs) {
            tab.setIcon(tabIcons.get(tabs.indexOf(tab)));
            Drawable icon = tab.getIcon();
            if (icon != null) {
                icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }
        }

        // Set up backward and forward buttons
        mBackwardFab = (FloatingActionButton) findViewById(R.id.floatingactionbutton_newheadache_backward);
        mBackwardFab.setOnClickListener(backwardFabListener);
        mForwardFab = (FloatingActionButton) findViewById(R.id.floatingactionbutton_newheadache_forward);
        mForwardFab.setOnClickListener(forwardFabListener);

        // Add listener to tab layout to hide/show buttons depending on which tab is selected
        mTabLayout.addOnTabSelectedListener(onTabSelectedListener);
        if (basicsTab != null) {
            basicsTab.select();
        }
    }

    private void recordFragment() {
        String name = EMPTY_STRING;
        switch (mTab.toMTab(mViewPager.getCurrentItem())) {
            case TAB_BASICS:
                name = "basics";
                break;
            case TAB_MEDICINE:
                name = "medicine";
                break;
            case TAB_SYMPTOMS:
                name = "symptoms";
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mViewPager.getCurrentItem() + EMPTY_STRING);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, name + "_tab");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }

    private void removeFocusFromEditText() {
        View view = mHeadacheActivity.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }

    private final View.OnClickListener backwardFabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (mTab.toMTab(mTabLayout.getSelectedTabPosition())) {
                case TAB_BASICS:
                    // Do nothing
                    return;
                case TAB_MEDICINE:
                    mViewPager.setCurrentItem(mTab.TAB_BASICS.getValue(), true);
                    return;
                case TAB_SYMPTOMS:
                    mViewPager.setCurrentItem(mTab.TAB_MEDICINE.getValue(), true);
            }
        }
    };

    private final View.OnClickListener forwardFabListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            switch (mTab.toMTab(mTabLayout.getSelectedTabPosition())) {
                case TAB_BASICS:
                    mViewPager.setCurrentItem(mTab.TAB_MEDICINE.getValue(), true);
                    return;
                case TAB_MEDICINE:
                    mViewPager.setCurrentItem(mTab.TAB_SYMPTOMS.getValue(), true);
                    return;
                case TAB_SYMPTOMS:
                    // Generate error message
                    String errorMessage = EMPTY_STRING;
                    mTab navigateTo = null;
                    if (mHeadacheEntry.getDateTime() == 0) {
                        errorMessage = getString(R.string.newheadache_errorenterdatetime);
                        navigateTo = mTab.TAB_BASICS;
                    } else if (mHeadacheEntry.isDidTakePainRelievers() && (mHeadacheEntry.getPainRelievers().equals(EMPTY_STRING) || mHeadacheEntry.getPainRelievers() == null)) {
                        errorMessage = getString(R.string.newheadache_errorpainrelievers);
                        navigateTo = mTab.TAB_MEDICINE;
                    }

                    if (errorMessage.equals(EMPTY_STRING)) {
                        // Save/update headache entry to database
                        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_HEADACHES).child(getCurrentUserId());
                        String key = mHeadacheEntry.getId();

                        if (key == null || key.equals(EMPTY_STRING)) {
                            key = mDatabase.push().getKey();
                            mHeadacheEntry.setId(key);
                        }
                        Map<String, Object> entryValues = mHeadacheEntry.toMap();
                        Map<String, Object> childUpdates = new HashMap<>();
                        childUpdates.put("/" + key, entryValues);
                        mDatabase.updateChildren(childUpdates);

                        if (mIsEditMode || mReturnToHeadacheHistory) {
                            // Go back to 'Headache History' screen
                            finish();
                        } else {
                            // Go back to 'Home' screen
                            NavUtils.navigateUpFromSameTask(mHeadacheActivity);
                        }
                    } else {
                        // Show error message
                        Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show();

                        // Navigate to corresponding tab
                        if (navigateTo != null) {
                            mViewPager.setCurrentItem(navigateTo.getValue(), true);
                        }
                    }
            }
        }
    };

    private final TabLayout.ViewPagerOnTabSelectedListener onTabSelectedListener = new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            styleFabs(tab);
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
            removeFocusFromEditText();
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            styleFabs(tab);
        }

        void styleFabs(TabLayout.Tab tab) {
            mBackwardFab.show();
            mForwardFab.show();
            mForwardFab.setImageResource(R.drawable.ic_chevron_right_accent_24dp);
            int position = tab.getPosition();
            switch (mTab.toMTab(position)) {
                case TAB_BASICS:
                    mBackwardFab.hide();
                    return;
                case TAB_SYMPTOMS:
                    mForwardFab.setImageResource(R.drawable.ic_done_accent_24dp);
                    return;
            }
            removeFocusFromEditText();
        }
    };

    public class NewHeadachePagerAdapter extends FragmentPagerAdapter {

        NewHeadachePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (mTab.toMTab(position)) {
                case TAB_BASICS:
                    return new BasicsFragment();
                case TAB_MEDICINE:
                    return new MedicineFragment();
                case TAB_SYMPTOMS:
                    return new SymptomsFragment();
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
                case TAB_BASICS:
                    return getString(R.string.newheadache_basics);
                case TAB_MEDICINE:
                    return getString(R.string.newheadache_medicine);
                case TAB_SYMPTOMS:
                    return getString(R.string.newheadache_symptoms);
                default:
                    return null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        if (getIntent().getStringExtra(INTENT_EDIT_HEADACHE_ENTRY) != null) {
            getMenuInflater().inflate(R.menu.menu_newheadache, menu);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_newheadache_delete:
                // Delete headache entry from database
                final DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_HEADACHES);
                mDatabase.child(mHeadacheEntry.getId()).removeValue();

                // Store deleted object in preferences in case user wants to undo his action
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                SharedPreferences.Editor e = sharedPreferences.edit();
                e.putString(INTENT_EDIT_HEADACHE_ENTRY, (new Gson()).toJson(mHeadacheEntry));
                e.apply();

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mViewPager.getCurrentItem() + EMPTY_STRING);
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "delete");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "delete_button");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                // Close window
                finish();

                return true;
            case android.R.id.home:
                if (mIsEditMode || mReturnToHeadacheHistory) {
                    finish();
                    return true;
                } else {
                    return super.onOptionsItemSelected(item);
                }
        }
        return super.onOptionsItemSelected(item);
    }

    public HeadacheEntry getHeadacheEntry() {
        return mHeadacheEntry;
    }
}
