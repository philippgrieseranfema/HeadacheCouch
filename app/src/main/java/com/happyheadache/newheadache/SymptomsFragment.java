package com.happyheadache.newheadache;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.happyheadache.Constants;
import com.happyheadache.models.HeadacheEntry;
import com.happyheadache.R;

import java.util.ArrayList;
import java.util.HashMap;

public class SymptomsFragment extends Fragment {

    public interface OnFragmentInteractionListener {
        HeadacheEntry getHeadacheEntry();
    }

    private OnFragmentInteractionListener mListener;

    public SymptomsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragment_symptoms, container, false);

        ListView mListView = (ListView) mView.findViewById(R.id.listview_newheadache_symptoms);

        // TODO: get from static file? or db
        SymptomItem item1 = new SymptomItem(R.drawable.ic_phonophobia, Constants.SYMPTOM.PHONOPHOBIA.getValue(), getString(R.string.all_phonophobiatitle), getString(R.string.newheadache_phonophobiadescription));
        SymptomItem item2 = new SymptomItem(R.drawable.ic_photophobia, Constants.SYMPTOM.PHOTOPHOBIA.getValue(), getString(R.string.all_photophobiatitle), getString(R.string.newheadache_photophobiadescription));
        SymptomItem item3 = new SymptomItem(R.drawable.ic_osmophobia, Constants.SYMPTOM.OSMOPHOBIA.getValue(), getString(R.string.all_osmophobiatitle), getString(R.string.newheadache_osmophobiadescription));
        SymptomItem item4 = new SymptomItem(R.drawable.ic_nausea, Constants.SYMPTOM.NAUSEA.getValue(), getString(R.string.all_nauseatitle), getString(R.string.newheadache_nauseadescription));
        SymptomItem item5 = new SymptomItem(R.drawable.ic_blurred_vision, Constants.SYMPTOM.BLURRED_VISION.getValue(), getString(R.string.all_blurredvisiontitle), getString(R.string.newheadache_blurredvisiondescription));
        SymptomItem item6 = new SymptomItem(R.drawable.ic_lightheadedness, Constants.SYMPTOM.LIGHTHEADEDNESS.getValue(), getString(R.string.all_lightheadednesstitle), getString(R.string.newheadache_lightheadednessdescription));
        SymptomItem item7 = new SymptomItem(R.drawable.ic_fatigue, Constants.SYMPTOM.FATIGUE.getValue(), getString(R.string.all_fatiguetitle), getString(R.string.newheadache_fatiguedescription));
        SymptomItem item8 = new SymptomItem(R.drawable.ic_insomnia, Constants.SYMPTOM.INSOMNIA.getValue(), getString(R.string.all_insomniatitle), getString(R.string.newheadache_insomniadescription));

        ArrayList<SymptomItem> dataSet = new ArrayList<>();
        dataSet.add(item1);
        dataSet.add(item2);
        dataSet.add(item3);
        dataSet.add(item4);
        dataSet.add(item5);
        dataSet.add(item6);
        dataSet.add(item7);
        dataSet.add(item8);

        BaseAdapter mAdapter = new SymptomsAdapter(getActivity(), dataSet);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(onItemClickListener);

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

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
            // Change check box value to opposite
            CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox_newheadache_symptom);
            checkBox.setChecked(!checkBox.isChecked());
        }
    };

    public class SymptomsAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<SymptomItem> mDataSource;

        SymptomsAdapter(Context context, ArrayList<SymptomItem> items) {
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
            final View rowView = mInflater.inflate(R.layout.item_symptom, parent, false);

            TextView titleTextView = (TextView) rowView.findViewById(R.id.textview_newheadache_symptomtitle);
            TextView descriptionTextView = (TextView) rowView.findViewById(R.id.textview_newheadache_symptomdescription);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.imageview_newheadache_symptomicon);
            CheckBox checkBox = (CheckBox) rowView.findViewById(R.id.checkbox_newheadache_symptom);

            HashMap<String, Boolean> symptoms = mListener.getHeadacheEntry().getSymptoms();
            String title = mDataSource.get(position).getTitle();
            final String id = mDataSource.get(position).getId();

            titleTextView.setText(title);
            descriptionTextView.setText(mDataSource.get(position).getDescription());
            imageView.setImageResource(mDataSource.get(position).getIconId());
            checkBox.setChecked(symptoms.containsKey(id) && symptoms.get(id));
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    // Save change to headache entry
                    TextView titleTextView = (TextView) rowView.findViewById(R.id.textview_newheadache_symptomtitle);
                    HashMap<String, Boolean> symptoms = mListener.getHeadacheEntry().getSymptoms();
                    symptoms.put(id, b);
                }
            });

            return rowView;
        }
    }
}
