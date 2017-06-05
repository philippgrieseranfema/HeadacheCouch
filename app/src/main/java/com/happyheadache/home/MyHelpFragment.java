package com.happyheadache.home;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.happyheadache.R;

import java.util.ArrayList;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class MyHelpFragment extends Fragment {

    private ArrayList<HomeItem> mDataSet;
    private FirebaseAnalytics mFirebaseAnalytics;

    public MyHelpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View mView = inflater.inflate(R.layout.fragment_myhelp, container, false);

        ListView mListView = (ListView) mView.findViewById(R.id.listview_home_myhelp);

        // TODO: Get my help from data analytics/DB
        MyHelpItem itemCardio = new MyHelpItem(R.drawable.ic_directions_run_black_24dp, getString(R.string.home_cardiovascularexercise), getString(R.string.home_cardiovascularexercisemeasure), getString(R.string.home_cardiovascularexercisedetails));
        MyHelpItem itemMagnesium = new MyHelpItem(R.drawable.ic_vitamins, getString(R.string.home_magnesium), getString(R.string.home_magnesiummeasure), getString(R.string.home_magnesiumdetails));
        MyHelpItem itemRelaxation = new MyHelpItem(R.drawable.ic_relaxation, getString(R.string.home_relaxationexercises), getString(R.string.home_relaxationexercisesmeasure), getString(R.string.home_relaxationexercisesdetails));
        MyHelpItem itemLiquid = new MyHelpItem(R.drawable.ic_local_drink_black_24dp, getString(R.string.home_liquidintake), getString(R.string.home_liquidintakemeasure), getString(R.string.home_liquidintakedetails));

        mDataSet = new ArrayList<>();
        mDataSet.add(new ExplanationItem());
        mDataSet.add(itemCardio);
        mDataSet.add(itemMagnesium);
        mDataSet.add(itemRelaxation);
        mDataSet.add(itemLiquid);

        BaseAdapter mAdapter = new MyHelpAdapter(getActivity(), mDataSet);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(onItemClickListener);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(getContext());

        return mView;
    }

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapter, View v, int position, long arg3) {
            // Get corresponding my help item
            HomeItem item = mDataSet.get(position);

            if (item.getClass() == MyHelpItem.class) {
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, ((MyHelpItem) item).getTitle() + "_view");
                mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);

                new AlertDialog.Builder(getActivity())
                        .setTitle(((MyHelpItem) item).getTitle())
                        .setMessage(((MyHelpItem) item).getLongDescription())
                        .setPositiveButton(R.string.home_dontshowagain, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // TODO: Remove entry from db or save it at 'hiddenByUser'
                            }
                        })
                        .setNegativeButton(R.string.all_close, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // Do nothing
                            }
                        })
                        .show();
            }
        }
    };

    public class MyHelpAdapter extends BaseAdapter {

        private final Context mContext;
        private final LayoutInflater mInflater;
        private final ArrayList<HomeItem> mDataSource;

        MyHelpAdapter(Context context, ArrayList<HomeItem> items) {
            mContext = context;
            mDataSource = items;
            mInflater = (LayoutInflater) mContext.getSystemService(LAYOUT_INFLATER_SERVICE);
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
            if (homeItem.getClass() == MyHelpItem.class) {
                MyHelpItem myHelpItem = (MyHelpItem) homeItem;
                rowView = mInflater.inflate(R.layout.item_myhelp, parent, false);

                TextView titleTextView = (TextView) rowView.findViewById(R.id.textview_home_helptitle);
                TextView descriptionTextView = (TextView) rowView.findViewById(R.id.textview_home_helpdescription);
                ImageView imageView = (ImageView) rowView.findViewById(R.id.imageview_home_helpicon);

                titleTextView.setText(myHelpItem.getTitle());
                descriptionTextView.setText(myHelpItem.getShortDescription());
                imageView.setImageResource(myHelpItem.getIconId());
                imageView.setColorFilter(ContextCompat.getColor(getContext(), R.color.colorAccent));
            } else if (homeItem.getClass() == ExplanationItem.class) {
                rowView = mInflater.inflate(R.layout.item_explanation, parent, false);

                TextView textView = (TextView) rowView.findViewById(R.id.textview_home_explanationtext);
                textView.setText(R.string.home_myhelpexplanation);
            }
            return rowView;
        }
    }
}
