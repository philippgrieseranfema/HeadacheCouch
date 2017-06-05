package com.happyheadache.headachehistory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.Gson;
import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;
import com.happyheadache.newheadache.NewHeadacheActivity;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;
import com.prolificinteractive.materialcalendarview.OnDateSelectedListener;
import com.prolificinteractive.materialcalendarview.spans.DotSpan;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import static com.happyheadache.Constants.BROADCAST_DATA_REFRESH;
import static com.happyheadache.Constants.INTENT_EDIT_HEADACHE_ENTRY;
import static com.happyheadache.Constants.formatHeadacheEntry;

public class HeadacheCalendarFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private MaterialCalendarView mCalendarView;
    private UpdatedHeadacheEntriesBroadcastReceiver mReceiver;

    public HeadacheCalendarFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragment_headachecalendar, container, false);

        mCalendarView = (MaterialCalendarView) mView.findViewById(R.id.materialcalendarview_headachehistory);

        // Pre-select all headache days on calendar and mark headache days where pain reliever was taken with dot
        paintDays();

        return mView;
    }

    @Override
    public void onResume() {
        if (mListener != null && mListener.getHeadacheEntries() != null) {
            paintDays();
        }

        IntentFilter filter = new IntentFilter(BROADCAST_DATA_REFRESH);
        mReceiver = new UpdatedHeadacheEntriesBroadcastReceiver();
        getActivity().registerReceiver(mReceiver, filter);

        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
    }

    private void paintDays() {
        // Reset calendar view decorators
        mCalendarView.removeDecorators();
        mCalendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_NONE);

        // Refill calendar with new headache entries
        mCalendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_MULTIPLE);
        final List<CalendarDay> headacheDays = new ArrayList<>();
        HashSet<CalendarDay> painRelieverDays = new HashSet<>();
        for (HeadacheEntry headacheEntry : mListener.getHeadacheEntries()) {
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(headacheEntry.getDateTime());
            CalendarDay calDay = CalendarDay.from(cal);
            mCalendarView.setDateSelected(calDay, true);
            headacheDays.add(calDay);
            if (headacheEntry.isDidTakePainRelievers()) {
                painRelieverDays.add(calDay);
            }
        }
        mCalendarView.addDecorator(new EventDecorator(R.color.colorPrimary, painRelieverDays));
        mCalendarView.setOnDateChangedListener(new OnDateSelectedListener() {
            @Override
            public void onDateSelected(@NonNull MaterialCalendarView widget, @NonNull CalendarDay date, boolean selected) {
                if (headacheDays.contains(date)) {
                    final HeadacheEntry entry = mListener.getHeadacheEntries().get(headacheDays.indexOf(date));
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.headachehistory_yourheadache))
                            .setMessage(formatHeadacheEntry(entry, "\n", getContext()))
                            .setPositiveButton(R.string.headachehistory_editentry, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(getActivity(), NewHeadacheActivity.class);
                                    intent.putExtra(INTENT_EDIT_HEADACHE_ENTRY, (new Gson()).toJson(entry));
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(R.string.all_close, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing
                                }
                            })
                            .show();
                }
                mCalendarView.setDateSelected(date, !selected);
            }
        });
    }

    public class UpdatedHeadacheEntriesBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListener != null && mListener.getHeadacheEntries() != null) {
                paintDays();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public class EventDecorator implements DayViewDecorator {

        private final int color;
        private final HashSet<CalendarDay> dates;

        EventDecorator(int color, Collection<CalendarDay> dates) {
            this.color = color;
            this.dates = new HashSet<>(dates);
        }

        @Override
        public boolean shouldDecorate(CalendarDay day) {
            return dates.contains(day);
        }

        @Override
        public void decorate(DayViewFacade view) {
            view.addSpan(new DotSpan(5, color));
        }
    }

    public interface OnFragmentInteractionListener {
        ArrayList<HeadacheEntry> getHeadacheEntries();
    }
}
