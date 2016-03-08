package com.cloudsecurity.cloudvault.setup;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.cloud.CloudListFragment;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupTwoFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetupTwoFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupTwoFragment extends Fragment
        implements CloudListFragment.OnFragmentInteractionListener {
    private static final String TAG = "CloudVault";

    public static final String ARG_ITEM_ID = "setup_two_fragment";
    private static final String ARG_SETUP_TYPE = "setup type";

    private boolean mSetupType;

    private OnFragmentInteractionListener mListener;

    // view elements
    private TextView addCloudText;
    private Button completeButton;
    private Fragment cloudListFragment;
    private int nCloudsAdded = 0;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param setupType Parameter 1.
     * @return A new instance of fragment SetupTwoFragment.
     */
    public static SetupTwoFragment newInstance(boolean setupType) {
        SetupTwoFragment fragment = new SetupTwoFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_SETUP_TYPE, setupType);
        fragment.setArguments(args);
        return fragment;
    }

    public SetupTwoFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mSetupType = getArguments().getBoolean(ARG_SETUP_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setup_two, container, false);


        addCloudText = (TextView) view.findViewById(R.id.add_cloud_text);
        if(mSetupType)
            addCloudText.setText(getResources().getString(R.string.add_cloud_create));
        else
            addCloudText.setText(getResources().getString(R.string.add_cloud_nocreate));

        completeButton = (Button) view.findViewById(R.id.complete_button);
        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mListener.onStepTwoCompleted();
            }
        });
        completeButton.setEnabled(false);

        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if(savedInstanceState == null) {
            getChildFragmentManager().beginTransaction().add(R.id.nested_fragment_container, CloudListFragment.newInstance()).commit();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onCloudAdded() {
        nCloudsAdded++;
        if(nCloudsAdded >= 4) {
            completeButton.setEnabled(true);
        }
    }

    @Override
    public void onCloudDeleted(String genericName) {
        nCloudsAdded--;
        if(nCloudsAdded < 4) {
            completeButton.setEnabled(false);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onStepTwoCompleted();
    }

}
