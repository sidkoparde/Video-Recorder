package com.danielkim.soundrecorder.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.danielkim.soundrecorder.R;

/**
 * Created by Gagan on 2018-04-04.
 */

public class HelpPageFragment extends Fragment {

    private static final String ARG_POSITION = "position";
    private static final String LOG_TAG = "HelpPageFragment";

    public static HelpPageFragment newInstance(int position) {
        HelpPageFragment f = new HelpPageFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_POSITION, position);
        f.setArguments(b);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_help_page, container, false);

        return v;
    }
}
