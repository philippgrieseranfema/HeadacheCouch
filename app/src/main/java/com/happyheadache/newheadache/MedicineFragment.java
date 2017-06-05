package com.happyheadache.newheadache;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.happyheadache.models.HeadacheEntry;
import com.happyheadache.R;

public class MedicineFragment extends Fragment {

    public interface OnFragmentInteractionListener {
        HeadacheEntry getHeadacheEntry();
    }

    private OnFragmentInteractionListener mListener;
    private View mView;
    private RadioButton mYesRadioButton;
    private AutoCompleteTextView mPainRelieversTextView;

    public MedicineFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mView = inflater.inflate(R.layout.fragment_medicine, container, false);

        // Set up auto complete text view with possible pain relievers
        mPainRelieversTextView = (AutoCompleteTextView) mView.findViewById(R.id.autocompletetextview_newheadache_painreliever);
        String[] painRelievers = getResources().getStringArray(R.array.newheadache_painrelieversarray);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, painRelievers);
        mPainRelieversTextView.setAdapter(adapter);
        mPainRelieversTextView.setOnFocusChangeListener(onFocusChangeListener);
        mPainRelieversTextView.setOnEditorActionListener(onEditorActionListener);
        mPainRelieversTextView.setText(mListener.getHeadacheEntry().getPainRelievers());

        // Set up radio buttons
        String painReliever = mListener.getHeadacheEntry().getPainRelievers();
        RadioButton noRadioButton = (RadioButton) mView.findViewById(R.id.radiobutton_newheadache_no);
        mYesRadioButton = (RadioButton) mView.findViewById(R.id.radiobutton_newheadache_yes);
        if (painReliever == null || painReliever.equals("")) {
            noRadioButton.toggle();
        } else {
            mYesRadioButton.toggle();
        }
        RadioGroup radioGroup = (RadioGroup) mView.findViewById(R.id.radiogroup_newheadache_painrelievers);
        radioGroup.setOnCheckedChangeListener(onCheckedChangeListener);

        // Inflate the layout for this fragment
        return mView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    private final TextView.OnEditorActionListener onEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // If user hits 'Done' button on keyboard, remove focus from the edit text
            boolean handled = false;
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                removeFocusFromEditText();
                handled = true;
            }
            return handled;
        }
    };

    private final View.OnFocusChangeListener onFocusChangeListener = new View.OnFocusChangeListener() {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            // Change toggle of radio buttons - when selecting the edit text, toggle 'yes' button
            // Otherwise, save the text of the edit text in our headache entry
            if (hasFocus) {
                mYesRadioButton.toggle();
            } else {
                String painRelievers;
                if (mYesRadioButton.isChecked()) {
                    painRelievers = mPainRelieversTextView.getText().toString();
                } else {
                    painRelievers = "";
                }
                mListener.getHeadacheEntry().setPainRelievers(painRelievers);
                mListener.getHeadacheEntry().setDidTakePainRelievers(mYesRadioButton.isChecked());
            }
        }
    };

    private final RadioGroup.OnCheckedChangeListener onCheckedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // Open keyboard for edit text when selecting 'Yes' button
            // Otherwise, when selecting 'No' button, close keyboard, remove text in edit text and remove focus
            if (checkedId == R.id.radiobutton_newheadache_yes) {
                mPainRelieversTextView.requestFocus();
            } else {
                mPainRelieversTextView.setText("");
                removeFocusFromEditText();
            }
        }
    };

    private void removeFocusFromEditText() {
        if (mView != null) {
            // Close keyboard
            InputMethodManager imm = (InputMethodManager)getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mView.getWindowToken(), 0);

            // Remove focus from edit text
            mView.clearFocus();
        }
    }
}
