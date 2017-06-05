package com.happyheadache.home;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.happyheadache.R;

import java.util.ArrayList;

import static com.happyheadache.Constants.EMPTY_STRING;

public class TriggersFragment extends Fragment {

    public TriggersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragment_triggers, container, false);

        ListView mListView = (ListView) mView.findViewById(R.id.listview_home_triggers);

        // TODO: Get triggers from data analytics/DB
        String[] factors1 = {getString(R.string.home_factor1), getString(R.string.home_factor2)};
        String[] factors2 = {getString(R.string.home_factor3)};
        String[] factors3 = {getString(R.string.home_factor4), getString(R.string.home_factor5)};

        ArrayList<HomeItem> dataSet = new ArrayList<>();
        dataSet.add(new ExplanationItem());
        dataSet.add(new TriggerItem(77, factors1));
        dataSet.add(new TriggerItem(54, factors2));
        dataSet.add(new TriggerItem(37, factors3));

        BaseAdapter mAdapter = new TriggersAdapter(getActivity(), dataSet);
        mListView.setAdapter(mAdapter);

        return mView;
    }

    public class TriggersAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<HomeItem> mDataSource;

        TriggersAdapter(Context context, ArrayList<HomeItem> items) {
            mContext = context;
            mDataSource = items;
            mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount() {
            return mDataSource.size();
        }

        @Override
        public Object getItem(int position) {
            return mDataSource.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = null;
            HomeItem homeItem = mDataSource.get(position);
            if (homeItem.getClass() == TriggerItem.class) {
                TriggerItem triggerItem = (TriggerItem) homeItem;
                rowView = mInflater.inflate(R.layout.item_trigger, parent, false);

                TextView probabilityTextView = (TextView) rowView.findViewById(R.id.textview_home_triggerprobability);
                TextView descriptionTextView = (TextView) rowView.findViewById(R.id.textview_home_triggerdescription);

                probabilityTextView.setText(String.format(getResources().getString(R.string.home_triggerpercentage), (triggerItem.getProbability())));

                String descriptionText = EMPTY_STRING;
                String[] factors = triggerItem.getFactors();
                for (int i = 0; i < factors.length; i++) {
                    String factor = factors[i];
                    descriptionText += "- " + factor;
                    if (i < factors.length - 1) {
                        descriptionText += " &\n";
                    }
                }

                descriptionTextView.setText(descriptionText);

            } else if (homeItem.getClass() == ExplanationItem.class) {
                rowView = mInflater.inflate(R.layout.item_explanation, parent, false);

                TextView textView = (TextView) rowView.findViewById(R.id.textview_home_explanationtext);
                textView.setText(R.string.home_triggerexplanation);
            }

            return rowView;
        }
    }
}
