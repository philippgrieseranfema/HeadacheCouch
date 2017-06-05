package com.happyheadache.headachehistory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.charts.CombinedChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.CombinedData;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.happyheadache.Constants.BROADCAST_DATA_REFRESH;

public class HeadacheGraphFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private CombinedChart mChart;
    private UpdatedHeadacheEntriesBroadcastReceiver mReceiver;

    public HeadacheGraphFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_headachegraph, container, false);

        mChart = (CombinedChart) view.findViewById(R.id.combinedchart_headachehistory);
        mChart.getDescription().setEnabled(false);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setDrawGridBackground(false);
        mChart.setDrawBarShadow(false);
        mChart.setHighlightFullBarEnabled(false);

        IAxisValueFormatter xAxisFormatter = new DayAxisValueFormatter(mChart);
        XAxis xAxis = mChart.getXAxis();
        xAxis.setValueFormatter(xAxisFormatter);

        updateGraph();

        return view;
    }

    @Override
    public void onResume() {
        if (mListener != null && mListener.getHeadacheEntries() != null) {
            updateGraph();
        }
        IntentFilter filter = new IntentFilter(BROADCAST_DATA_REFRESH);
        mReceiver = new UpdatedHeadacheEntriesBroadcastReceiver();
        getActivity().registerReceiver(mReceiver, filter);

        super.onResume();
    }

    public class UpdatedHeadacheEntriesBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mListener != null && mListener.getHeadacheEntries() != null) {
                updateGraph();
            }
        }
    }

    private void updateGraph() { // TODO @p.grieser
        // TODO: What if multiple headaches on one day?
        if (mListener.getHeadacheEntries() != null && mListener.getHeadacheEntries().size() > 0) {
            ArrayList<HeadacheEntry> headacheEntries = mListener.getHeadacheEntries();

            long fromTimeMillis = headacheEntries.get(headacheEntries.size() - 1).getDateTime();
            long toTimeMillis = headacheEntries.get(0).getDateTime();

            List<Long> dates = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(fromTimeMillis);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            while (cal.getTimeInMillis() <= toTimeMillis) {
                dates.add(cal.getTimeInMillis());
                cal.add(Calendar.DATE, 1);
            }

            List<BarEntry> durationEntries = new ArrayList<>();
            List<Entry> intensityEntries = new ArrayList<>();
            int i = 1;
            HeadacheEntry currEntry = headacheEntries.get(headacheEntries.size() - i);
            for (int j = 0; j < dates.size(); j++) {
                long currDate = dates.get(j);

                long nextDate;
                if (j + 1 < dates.size()) {
                    nextDate = dates.get(j + 1);
                } else {
                    nextDate = currDate + 1000 * 60 * 60 * 24;
                }

                cal.setTimeInMillis(currDate);

                if (currEntry.getDateTime() >= currDate && currEntry.getDateTime() < nextDate) {
                    durationEntries.add(new BarEntry(cal.get(Calendar.DAY_OF_YEAR) - 1, currEntry.getDuration(), currEntry));
                    intensityEntries.add(new Entry(cal.get(Calendar.DAY_OF_YEAR) - 1, currEntry.getIntensity(), currEntry));
                    i++;
                    if (i <= headacheEntries.size()) {
                        currEntry = headacheEntries.get(headacheEntries.size() - i);
                    }
                } else {
                    durationEntries.add(new BarEntry(cal.get(Calendar.DAY_OF_YEAR) - 1, 0));
                    intensityEntries.add(new BarEntry(cal.get(Calendar.DAY_OF_YEAR) - 1, 0));
                }
            }

            BarDataSet barDataSet = new BarDataSet(durationEntries, getString(R.string.all_durationofheadache)); // add entries to dataset
            int barColor = ContextCompat.getColor(getContext(), R.color.colorPrimary);
            barDataSet.setColor(barColor);
            barDataSet.setValueTextColor(barColor);
            barDataSet.setValueTextSize(10f);
            barDataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
            barDataSet.setValueFormatter(new CustomValueFormatter());
            BarData barData = new BarData(barDataSet);

            LineDataSet lineDataSet = new LineDataSet(intensityEntries, getString(R.string.all_intensity)); // add entries to dataset
            int lineColor = ContextCompat.getColor(getContext(), R.color.colorAccent);
            lineDataSet.setColor(lineColor);
            lineDataSet.setLineWidth(2.5f);
            lineDataSet.setCircleColor(lineColor);
            lineDataSet.setCircleRadius(5f);
            lineDataSet.setFillColor(lineColor);
            lineDataSet.setValueTextSize(10f);
            lineDataSet.setValueTextColor(lineColor);
            lineDataSet.setAxisDependency(YAxis.AxisDependency.RIGHT);
            lineDataSet.setValueFormatter(new CustomValueFormatter());
            LineData lineData = new LineData(lineDataSet);

            CombinedData data = new CombinedData();
            data.setData(barData);
            data.setData(lineData);
            mChart.setData(data);
            mChart.invalidate();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        getActivity().unregisterReceiver(mReceiver);
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

    public interface OnFragmentInteractionListener {
        ArrayList<HeadacheEntry> getHeadacheEntries();
    }

    class CustomValueFormatter implements IValueFormatter
    {

        private DecimalFormat mFormat;

        CustomValueFormatter() {
            mFormat = new DecimalFormat("###,###,###,##0");
        }

        @Override
        public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
            return mFormat.format(value);
        }
    }
}
