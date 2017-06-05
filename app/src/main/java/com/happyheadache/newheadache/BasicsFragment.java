package com.happyheadache.newheadache;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;

import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;

import java.util.Calendar;

import static com.happyheadache.Constants.formatDate;
import static com.happyheadache.Constants.formatIntensity;
import static com.happyheadache.Constants.formatTime;

public class BasicsFragment extends Fragment implements View.OnClickListener, DatePickerDialog.OnDateSetListener, TimePickerDialog.OnTimeSetListener {

    private OnFragmentInteractionListener mListener;
    private View mView;
    private SeekBar mDurationSeekBar;
    private SeekBar mIntensitySeekBar;

    public BasicsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_basics, container, false);

        // Set up date & time buttons
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(mListener.getHeadacheEntry().getDateTime());
        Button dateButton = (Button) mView.findViewById(R.id.button_newheadache_date);
        Button timeButton = (Button) mView.findViewById(R.id.button_newheadache_time);
        String dateText;
        String timeText;
        if (mListener.getHeadacheEntry().getDateTime() == 0) {
            dateText = getString(R.string.newheadache_date);
            timeText = getString(R.string.newheadache_time);
        } else {
            dateText = formatDate(mListener.getHeadacheEntry().getDateTime(), getContext());
            timeText = formatTime(mListener.getHeadacheEntry().getDateTime(), getContext());
        }
        dateButton.setText(dateText);
        timeButton.setText(timeText);
        dateButton.setOnClickListener(this);
        timeButton.setOnClickListener(this);

        // Set up duration seek bar
        mDurationSeekBar = (SeekBar) mView.findViewById(R.id.seekbar_newheadache_duration);
        mDurationSeekBar.setOnSeekBarChangeListener(durationSeekBarListener);
        mDurationSeekBar.setProgress(mListener.getHeadacheEntry().getDuration());

        // Set up intensity seek bar
        mIntensitySeekBar = (SeekBar) mView.findViewById(R.id.seekbar_newheadache_intensity);
        mIntensitySeekBar.setOnSeekBarChangeListener(intensitySeekBarListener);
        mIntensitySeekBar.setProgress(mListener.getHeadacheEntry().getIntensity());

        // Inflate the layout for this fragment
        return mView;
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

    @Override
    public void onClick(View v) {
        long dateTime = mListener.getHeadacheEntry().getDateTime();
        switch (v.getId()) {
            case R.id.button_newheadache_date:
                Calendar cal = Calendar.getInstance();
                if (dateTime != 0) {
                    cal.setTimeInMillis(dateTime);
                }

                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH);
                int day = cal.get(Calendar.DAY_OF_MONTH);

                DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), this, year, month, day);
                datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
                datePickerDialog.show();
                break;
            case R.id.button_newheadache_time:
                cal = Calendar.getInstance();
                if (dateTime != 0) {
                    cal.setTimeInMillis(dateTime);
                }

                int hour = cal.get(Calendar.HOUR_OF_DAY);
                int minute = cal.get(Calendar.MINUTE);

                TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(), this, hour, minute, DateFormat.is24HourFormat(getActivity()));
                timePickerDialog.show();
                break;
        }
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        // Show date on button in the device's date format
        Calendar c = Calendar.getInstance();
        c.set(year, month, day, 0, 0);
        String date = formatDate(c.getTimeInMillis(), getContext());
        Button dateButton = (Button) getActivity().findViewById(R.id.button_newheadache_date);
        dateButton.setText(date);

        // Store date in headache entry
        Calendar cal = Calendar.getInstance();
        if (mListener.getHeadacheEntry().getDateTime() != 0) {
            cal.setTimeInMillis(mListener.getHeadacheEntry().getDateTime());
        }
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, day);
        mListener.getHeadacheEntry().setDateTime(cal.getTimeInMillis());
    }

    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        String time = formatTime(hourOfDay, minute, getContext());

        // Show time on button depending on device's time format
        Button timeButton = (Button) getActivity().findViewById(R.id.button_newheadache_time);
        timeButton.setText(time);

        // Store time in headache entry
        Calendar cal = Calendar.getInstance();
        if (mListener.getHeadacheEntry().getDateTime() != 0) {
            cal.setTimeInMillis(mListener.getHeadacheEntry().getDateTime());
        }
        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
        cal.set(Calendar.MINUTE, minute);
        mListener.getHeadacheEntry().setDateTime(cal.getTimeInMillis());
    }

    public interface OnFragmentInteractionListener {
        HeadacheEntry getHeadacheEntry();
    }

    private final SeekBar.OnSeekBarChangeListener durationSeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            TextView durationTextView = (TextView) mView.findViewById(R.id.textview_newheadache_duration);
            durationTextView.setText(String.format(getResources().getQuantityString(R.plurals.newheadache_hours, progress), progress));
            mListener.getHeadacheEntry().setDuration(mDurationSeekBar.getProgress());
        }
    };

    private final SeekBar.OnSeekBarChangeListener intensitySeekBarListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            TextView intensityTextView = (TextView) mView.findViewById(R.id.textview_newheadache_intensity);
            String intensity = formatIntensity(progress, getContext());
            intensityTextView.setText(intensity);
            mListener.getHeadacheEntry().setIntensity(mIntensitySeekBar.getProgress());
        }
    };

}
