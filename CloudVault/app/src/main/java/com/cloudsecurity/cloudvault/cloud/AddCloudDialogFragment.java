package com.cloudsecurity.cloudvault.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.cloudsecurity.cloudvault.R;

/**
 * Created on 25-Oct-15.
 */
public class AddCloudDialogFragment extends DialogFragment{
    private static final String TAG = "CloudVault";

    public interface AddCloudDialogListener {
        void onCloudTypeSelected(DialogFragment dialog, int cloudType);
    }

    AddCloudDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the AddCloudDialogListener so we can send events to the host
            mListener = (AddCloudDialogListener) getTargetFragment();
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement AddCloudDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_cloud_type)
                .setItems(R.array.supported_cloud_types_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onCloudTypeSelected(AddCloudDialogFragment.this, which);
                    }
                });
        return builder.create();
    }

}
