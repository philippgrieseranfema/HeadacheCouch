package com.happyheadache.headachehistory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;
import com.happyheadache.newheadache.NewHeadacheActivity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import static com.happyheadache.Constants.APP_TAG;
import static com.happyheadache.Constants.BROADCAST_DATA_REFRESH;
import static com.happyheadache.Constants.EMPTY_STRING;
import static com.happyheadache.Constants.FIREBASE_CHILD_HEADACHES;
import static com.happyheadache.Constants.INTENT_EDIT_HEADACHE_ENTRY;
import static com.happyheadache.Constants.INTENT_RETURN_TO_HEADACHE_HISTORY;
import static com.happyheadache.Constants.formatHeadacheEntry;
import static com.happyheadache.Constants.getCurrentUserId;

public class HeadacheHistoryActivity extends AppCompatActivity implements HeadacheListFragment.OnFragmentInteractionListener, HeadacheCalendarFragment.OnFragmentInteractionListener, HeadacheGraphFragment.OnFragmentInteractionListener {

    private ArrayList<HeadacheEntry> mHeadacheEntries;
    private CoordinatorLayout mCoordinatorLayout;
    private HeadacheHistoryViewPager mViewPager;
    private FirebaseAnalytics mFirebaseAnalytics;

    // Enumeration to reference tabs within code without numbers
    public enum mTab {
        TAB_LIST(0), TAB_CALENDAR(1), TAB_GRAPHS(2);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_headachehistory);

        // Show toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_headachehistory);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        // Enable up navigation (back arrow)
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mCoordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorlayout_headachehistory);

        // Setup firebase analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize pager adapter and view pager to enable tabs and swiping on screen as navigation
        HeadacheHistoryPagerAdapter headacheHistoryPagerAdapter = new HeadacheHistoryPagerAdapter(getSupportFragmentManager());
        mViewPager = (HeadacheHistoryViewPager) findViewById(R.id.headachehistoryviewpager_headachehistory);
        mViewPager.setAdapter(headacheHistoryPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                recordFragment();
            }
        });
        recordFragment();

        // Set up tab layout
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tablayout_headachehistory);
        tabLayout.setupWithViewPager(mViewPager);
        tabLayout.addOnTabSelectedListener(onTabSelectedListener);

        // Set up tabs
        TabLayout.Tab listTab = tabLayout.getTabAt(mTab.TAB_LIST.getValue());
        TabLayout.Tab calendarTab = tabLayout.getTabAt(mTab.TAB_CALENDAR.getValue());
        TabLayout.Tab graphsTab = tabLayout.getTabAt(mTab.TAB_GRAPHS.getValue());

        ArrayList<TabLayout.Tab> tabs = new ArrayList<>(Arrays.asList(listTab, calendarTab, graphsTab));
        ArrayList<Integer> tabIcons = new ArrayList<>(Arrays.asList(R.drawable.ic_format_list_bulleted_white_24dp, R.drawable.ic_insert_invitation_white_24dp, R.drawable.ic_trending_up_white_24dp));
        int tintColor = Color.parseColor(getResources().getString(0 + R.color.textColorPrimary));

        for (TabLayout.Tab tab : tabs) {
            tab.setIcon(tabIcons.get(tabs.indexOf(tab)));
            Drawable icon = tab.getIcon();
            if (icon != null) {
                icon.setColorFilter(tintColor, PorterDuff.Mode.SRC_IN);
            }
        }

        // Get user's headache entries from datbase
        mHeadacheEntries = new ArrayList<>();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_HEADACHES).child(getCurrentUserId());

        ValueEventListener entryListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ArrayList<HeadacheEntry> tempEntries = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    HeadacheEntry entry = snapshot.getValue(HeadacheEntry.class);
                    tempEntries.add(entry);
                }
                Collections.sort(tempEntries, new Comparator<HeadacheEntry>() {
                    @Override
                    public int compare(HeadacheEntry o1, HeadacheEntry o2) {
                        Long l1 = o1.getDateTime();
                        Long l2 = o2.getDateTime();
                        return l2.compareTo(l1);
                    }
                });
                mHeadacheEntries = tempEntries;

                // Let tabs know that there is new data
                Intent i = new Intent(BROADCAST_DATA_REFRESH);
                sendBroadcast(i);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(APP_TAG, "loadHeadacheEntry:onCancelled", databaseError.toException());
            }
        };
        mDatabase.addValueEventListener(entryListener);
    }

    @Override
    protected void onResume() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String serializabledHeadacheEntry = sharedPreferences.getString(INTENT_EDIT_HEADACHE_ENTRY, EMPTY_STRING);
        if (!serializabledHeadacheEntry.equals(EMPTY_STRING)) {
            final HeadacheEntry entry = (new Gson()).fromJson(serializabledHeadacheEntry, HeadacheEntry.class);

            SharedPreferences.Editor e = sharedPreferences.edit();
            e.putString(INTENT_EDIT_HEADACHE_ENTRY, EMPTY_STRING);
            e.apply();

            // Create snack bar to let user undo remove action
            Snackbar.make(mCoordinatorLayout, R.string.headachehistory_headacheentryremoved, Snackbar.LENGTH_LONG).setAction(R.string.home_undo, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Re-insert headache entry
                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference().child(FIREBASE_CHILD_HEADACHES);
                    mDatabase.child(entry.getId()).setValue(entry);
                }
            }).show();
        }

        super.onResume();
    }

    private void recordFragment() {
        String name = EMPTY_STRING;
        switch (mTab.toMTab(mViewPager.getCurrentItem())) {
            case TAB_LIST:
                name = "list";
                break;
            case TAB_CALENDAR:
                name = "calendar";
                break;
            case TAB_GRAPHS:
                name = "graphs";
                break;
        }

        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, mViewPager.getCurrentItem() + EMPTY_STRING);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, name + "_tab");
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
    }


    public class HeadacheHistoryPagerAdapter extends FragmentPagerAdapter {

        HeadacheHistoryPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (mTab.toMTab(position)) {
                case TAB_LIST:
                    return new HeadacheListFragment();
                case TAB_CALENDAR:
                    return new HeadacheCalendarFragment();
                case TAB_GRAPHS:
                    return new HeadacheGraphFragment();
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
                case TAB_LIST:
                    return getString(R.string.headachehistory_list);
                case TAB_CALENDAR:
                    return getString(R.string.headachehistory_calendar);
                case TAB_GRAPHS:
                    return getString(R.string.headachehistory_graph);
                default:
                    return null;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_headachehistory, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_headachehistory_new:
                // Let New Headache know to navigate back to this view
                Intent intent = new Intent(this, NewHeadacheActivity.class);
                intent.putExtra(INTENT_RETURN_TO_HEADACHE_HISTORY, true);

                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "new_headache2_button");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                startActivity(intent);
                return true;
            case R.id.item_headachehistory_export:
                bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "export_headache_button");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                // Open chooser with headache entries as simple list
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getString(R.string.headachehistory_send)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private String getShareText() {
        String text = EMPTY_STRING;
        for (HeadacheEntry entry : mHeadacheEntries) {
            text += formatHeadacheEntry(entry, "\n", getApplicationContext()) + "\n\n";
        }
        return text;
    }

    private final TabLayout.ViewPagerOnTabSelectedListener onTabSelectedListener = new TabLayout.ViewPagerOnTabSelectedListener(mViewPager) {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            mViewPager.setSwipeable(tab.getPosition() == mTab.TAB_LIST.getValue());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {
        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {
            mViewPager.setSwipeable(tab.getPosition() == mTab.TAB_LIST.getValue());
        }

    };

    @Override
    public ArrayList<HeadacheEntry> getHeadacheEntries() {
        return mHeadacheEntries;
    }
}
