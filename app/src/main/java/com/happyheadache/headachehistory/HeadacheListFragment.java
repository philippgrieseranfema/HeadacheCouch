package com.happyheadache.headachehistory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.happyheadache.Constants;
import com.happyheadache.R;
import com.happyheadache.models.HeadacheEntry;
import com.happyheadache.newheadache.NewHeadacheActivity;
import com.wefika.flowlayout.FlowLayout;

import java.util.ArrayList;
import java.util.Map;

import static com.happyheadache.Constants.BROADCAST_DATA_REFRESH;
import static com.happyheadache.Constants.INTENT_EDIT_HEADACHE_ENTRY;
import static com.happyheadache.Constants.formatDate;
import static com.happyheadache.Constants.formatIntensity;

public class HeadacheListFragment extends Fragment {

    private HeadacheListAdapter mAdapter;
    private OnFragmentInteractionListener mListener;
    private UpdatedHeadacheEntriesBroadcastReceiver mReceiver;

    public HeadacheListFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_headachelist, container, false);

        ListView listView = (ListView) view.findViewById(R.id.listview_headachehistory);

        mAdapter = new HeadacheListAdapter(getActivity(), mListener.getHeadacheEntries());
        listView.setAdapter(mAdapter);

        return view;
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
    public void onResume() {
        if (mAdapter != null && mListener != null && mListener.getHeadacheEntries() != null) {
            mAdapter.setDataSource(mListener.getHeadacheEntries());
            mAdapter.notifyDataSetChanged();
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public class UpdatedHeadacheEntriesBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mAdapter != null && mListener != null && mListener.getHeadacheEntries() != null) {
                mAdapter.setDataSource(mListener.getHeadacheEntries());
                mAdapter.notifyDataSetChanged();
            }
        }
    }

    public class HeadacheListAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private ArrayList<HeadacheEntry> mDataSource;

        HeadacheListAdapter(Context context, ArrayList<HeadacheEntry> items) {
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

        public void setDataSource(ArrayList<HeadacheEntry> dataSource) {
            mDataSource = dataSource;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final View rowView = mInflater.inflate(R.layout.item_headachelist, parent, false);
            final HeadacheEntry entry = mDataSource.get(position);

            TextView headacheDateTextView = (TextView) rowView.findViewById(R.id.headache_date_text_view);
            ImageButton editImageButton = (ImageButton) rowView.findViewById(R.id.imagebutton_headachehistory_edit);

            // TODO: Calculate 'degree of badness' and color flag image view accordingly in traffic light colors
            //ImageView flagImageView = (ImageView) rowView.findViewById(R.id.imageview_headachehistory_flag);

            TextView durationTextView = (TextView) rowView.findViewById(R.id.textview_headachehistory_duration);
            TextView intensityTextView = (TextView) rowView.findViewById(R.id.textview_headachehistory_intensity);
            TextView painRelieverTextView = (TextView) rowView.findViewById(R.id.textview_headachehistory_painreliever);

            headacheDateTextView.setText(formatDate(entry.getDateTime(), getActivity()));

            int iconColor = Color.parseColor(getResources().getString(0 + R.color.colorPrimary));

            int durationImageResource;
            if (entry.getIntensity() < 2) {
                durationImageResource = R.drawable.ic_timer_empty;
            } else if (entry.getIntensity() < 6) {
                durationImageResource = R.drawable.ic_timer_almost_empty;
            } else if (entry.getIntensity() < 12) {
                durationImageResource = R.drawable.ic_timer_almost_full;
            } else {
                durationImageResource = R.drawable.ic_timer_full;
            }
            Drawable durationDrawable = getResources().getDrawable(durationImageResource);
            durationDrawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            durationTextView.setCompoundDrawablesWithIntrinsicBounds(null, durationDrawable, null, null);
            durationTextView.setText(String.format(getResources().getQuantityString(R.plurals.headachehistory_duration, entry.getDuration()), entry.getDuration()));

            int intensityImageResource;
            if (entry.getIntensity() < 4) {
                intensityImageResource = R.drawable.ic_smile_happy;
            } else if (entry.getIntensity() < 7) {
                intensityImageResource = R.drawable.ic_smile_neutral;
            } else {
                intensityImageResource = R.drawable.ic_smile_sad;
            }
            Drawable intensityDrawable = getResources().getDrawable(intensityImageResource);
            intensityDrawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            intensityTextView.setCompoundDrawablesWithIntrinsicBounds(null, intensityDrawable, null, null);
            String intensity = formatIntensity(entry.getIntensity(), getActivity());
            intensityTextView.setText(String.format(getResources().getString(R.string.headachehistory_intensity), intensity));

            int painRelieverImageResource;
            String painRelieverString;
            if (entry.isDidTakePainRelievers()) {
                painRelieverImageResource = R.drawable.ic_painkiller;
                painRelieverString = String.format(getResources().getString(R.string.headachehistory_didtakepainrelievers), entry.getPainRelievers());
            } else {
                painRelieverImageResource = R.drawable.ic_no_painkiller;
                painRelieverString = getResources().getString(R.string.headachehistory_didnttakepainrelievers);
            }
            Drawable painRelieverDrawable = getResources().getDrawable(painRelieverImageResource);
            painRelieverDrawable.setColorFilter(iconColor, PorterDuff.Mode.SRC_IN);
            painRelieverTextView.setCompoundDrawablesWithIntrinsicBounds(null, painRelieverDrawable, null, null);
            painRelieverTextView.setText(painRelieverString);

            editImageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Start the 'New Headache' activity, but with the current headache entry as basis instead of an empty one
                    Intent intent = new Intent(getActivity(), NewHeadacheActivity.class);
                    intent.putExtra(INTENT_EDIT_HEADACHE_ENTRY, (new Gson()).toJson(entry));
                    startActivity(intent);
                }
            });

            // Dynamically add symptoms
            FlowLayout layout = (FlowLayout) rowView.findViewById(R.id.flowlayout_headachehistory_symptoms);
            TextView yourSymptomsTextView = (TextView) rowView.findViewById(R.id.textview_headachehistory_yoursymptoms);
            if (entry.getSymptoms() != null && entry.getSymptoms().size() > 0) {
                yourSymptomsTextView.setText(getResources().getString(R.string.headachehistory_yoursymptoms));
                for (Map.Entry<String, Boolean> symptom : entry.getSymptoms().entrySet()) {
                    if (symptom.getValue()) {
                        TextView textView = (TextView) mInflater.inflate(R.layout.item_symptom2, layout, false);

                        int symptomImageResource = 0;
                        int symptomStringResource = 0;
                        switch (Constants.SYMPTOM.toSymptom(symptom.getKey())) {
                            case PHONOPHOBIA:
                                symptomImageResource = R.drawable.ic_phonophobia_small;
                                symptomStringResource = R.string.all_phonophobiatitle;
                                break;
                            case PHOTOPHOBIA:
                                symptomImageResource = R.drawable.ic_photophobia_small;
                                symptomStringResource = R.string.all_photophobiatitle;
                                break;
                            case OSMOPHOBIA:
                                symptomImageResource = R.drawable.ic_osmophobia_small;
                                symptomStringResource = R.string.all_osmophobiatitle;
                                break;
                            case NAUSEA:
                                symptomImageResource = R.drawable.ic_nausea_small;
                                symptomStringResource = R.string.all_nauseatitle;
                                break;
                            case BLURRED_VISION:
                                symptomImageResource = R.drawable.ic_blurred_vision_small;
                                symptomStringResource = R.string.all_blurredvisiontitle;
                                break;
                            case LIGHTHEADEDNESS:
                                symptomImageResource = R.drawable.ic_lightheadedness_small;
                                symptomStringResource = R.string.all_lightheadednesstitle;
                                break;
                            case FATIGUE:
                                symptomImageResource = R.drawable.ic_fatigue_small;
                                symptomStringResource = R.string.all_fatiguetitle;
                                break;
                            case INSOMNIA:
                                symptomImageResource = R.drawable.ic_insomnia_small;
                                symptomStringResource = R.string.all_insomniatitle;
                                break;
                        }
                        Drawable symptomDrawable = getResources().getDrawable(symptomImageResource);
                        textView.setCompoundDrawablesWithIntrinsicBounds(symptomDrawable, null, null, null);
                        textView.setText(getResources().getString(symptomStringResource));
                        layout.addView(textView);
                    }
                }
            } else {
                yourSymptomsTextView.setText(getResources().getString(R.string.headachehistory_nosymptoms));
            }

            return rowView;
        }
    }

    public interface OnFragmentInteractionListener {
        ArrayList<HeadacheEntry> getHeadacheEntries();
    }
}
