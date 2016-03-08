package com.cloudsecurity.cloudvault.setup;

import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.cloudsecurity.cloudvault.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link SetupOneFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link SetupOneFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SetupOneFragment extends Fragment {
    private static final String TAG = "CloudVault";

    public static final String ARG_ITEM_ID = "setup_two_fragment";
    private static final String ARG_PARAM1 = "param1";

    // TODO: Rename and change types of parameters
    private String mParam1;

    private OnFragmentInteractionListener mListener;

    // view elements
    private Button createButton;
    private Button nocreateButton;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @return A new instance of fragment SetupOneFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SetupOneFragment newInstance(String param1) {
        SetupOneFragment fragment = new SetupOneFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    public SetupOneFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "SetupOneFragment : onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "SetupOneFragment : onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_setup_one, container, false);

        createButton = (Button) view.findViewById(R.id.create_button);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onInstallTypeSelected(true);
            }
        });

        nocreateButton = (Button) view.findViewById(R.id.nocreate_button);
        nocreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onInstallTypeSelected(false);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v(TAG, "SetupOneFragment : onAttach");
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        public void onInstallTypeSelected(boolean create);
    }

}
