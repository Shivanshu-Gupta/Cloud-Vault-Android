package com.cloudsecurity.cloudvault.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.cloud.dropbox.DropboxAuthenticator;
import com.cloudsecurity.cloudvault.cloud.dropbox.DropboxHandle;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;
import com.cloudsecurity.cloudvault.util.DirectoryChooserDialog;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link CloudListFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link CloudListFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class CloudListFragment extends Fragment implements AddCloudDialogFragment.AddCloudDialogListener, DirectoryChooserDialog.ChosenDirectoryListener {
    private static final String TAG = "CloudVault";

    private static final int NEW_FOLDERCLOUD_REQUEST_CODE = 1;
    private static final int DROPBOX_LOGIN_REQUEST_CODE = 2; // onActivityResult request code
    private static final int DROPBOX_LOGOUT_REQUEST_CODE = -2; // onActivityResult request code
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_ITEM_ID = "cloud_list_fragment";

    private Activity activity;
    private ListView cloudListView;
    private List<CloudMeta> cloudMetas;
    private CloudListAdapter cloudListAdapter;
    private CloudSharedPref cloudSharedPref;

    private Button addCloudButton;

    private OnFragmentInteractionListener mListener;

    public CloudListFragment() {
        // Required empty public constructor
    }

    public static CloudListFragment newInstance() {
        CloudListFragment fragment = new CloudListFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "CloudListFragment : onCreate");
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
        }
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "CloudListFragment : onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cloud_list, container, false);
        cloudSharedPref = new CloudSharedPref(activity);
        cloudMetas = cloudSharedPref.getClouds(activity);
        if (cloudMetas == null) {
            showAlert(getResources().getString(R.string.no_clouds), getResources().getString(R.string.no_clouds_msg));
        } else {
            if (cloudMetas.size() == 0) {
                showAlert(getResources().getString(R.string.no_clouds), getResources().getString(R.string.no_clouds_msg));
            } else if (cloudMetas.size() < 4) {
                showAlert(getResources().getString(R.string.not_enough_clouds), getResources().getString(R.string.not_enough_clouds_msg));
            }

            cloudListView = (ListView) view.findViewById(R.id.list_clouds);
            cloudListAdapter = new CloudListAdapter(activity, cloudMetas);
            cloudListView.setAdapter(cloudListAdapter);

            //Set required ActionListeners to the cloudListView
        }
        addCloudButton = (Button) view.findViewById(R.id.add_cloud);
        addCloudButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCloudDialog();
            }
        });
        return view;
    }

    private void showAddCloudDialog() {
        AddCloudDialogFragment addCloudDialog = new AddCloudDialogFragment();
        addCloudDialog.setTargetFragment(this, 0);
        addCloudDialog.show(getFragmentManager(), "new cloud");
    }

    public void showAlert(String title, String message) {
        if (activity != null && !activity.isFinishing()) {
            AlertDialog alertDialog = new AlertDialog.Builder(activity)
                    .create();
            alertDialog.setTitle(title);
            alertDialog.setMessage(message);
            alertDialog.setCancelable(false);

            // setting OK Button
            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                    new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            // activity.finish();
                            getFragmentManager().popBackStackImmediate();
                        }
                    });
            alertDialog.show();
        }
    }

    @Override
    public void onResume() {
        Log.v(TAG, "CloudListFragment : onResume");
        getActivity().setTitle(R.string.clouds);
        super.onResume();
    }

    @Override
    public void onAttach(Activity activity) {
        Log.v(TAG, "CloudListFragment : onAttach");
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
            Log.e(TAG, activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        Log.v(TAG, "CloudListFragment : onDetach");
        super.onDetach();
        mListener = null;
    }

    //for AddCloudDialogListener
    @Override
    public void onCloudTypeSelected(DialogFragment dialog, int cloudType) {
        switch (cloudType) {
            case 0:
                //Create a new Dropbox cloud
                //Start authentication for a new dropbox cloud
                Intent intent = new Intent(getActivity(), DropboxAuthenticator.class);
                intent.setAction(DropboxAuthenticator.ACTION_LOGIN);
                ArrayList<String> uids = new ArrayList<>();
                for(CloudMeta cloudMeta : cloudMetas) {
                    if(cloudMeta.getName()==DropboxHandle.DROPBOX) {
                        uids.add(cloudMeta.getMeta().get(DropboxHandle.UID));
                    }
                }
                String[] alreadyAuthedUids = uids.toArray(new String[0]);
                intent.putExtra(DropboxAuthenticator.ALREADY_AUTHED_UIDS, alreadyAuthedUids);
                startActivityForResult(intent, DROPBOX_LOGIN_REQUEST_CODE);
                break;
            case 1:
                //Create a new folder cloud
                // Create DirectoryChooserDialog and register a callback
                String root = Environment.getExternalStorageDirectory().getAbsolutePath();
                DirectoryChooserDialog directoryChooserDialog =
                        new DirectoryChooserDialog(activity, this);
                // Toggle new folder button enabling
                directoryChooserDialog.setNewFolderEnabled(true);
                // Load directory chooser dialog for initial 'm_chosenDir' directory.
                // The registered callback will be called upon final directory selection.
                directoryChooserDialog.chooseDirectory(root);
                break;
        }
    }
    @Override
    public void onChosenDir(String chosenDir) {
        try {
            showToast("Chosen Path: " + chosenDir);
            File file = new File(chosenDir);
            if(file.isDirectory()) {
                ConcurrentHashMap<String, String> meta = new ConcurrentHashMap<>();
                meta.put(FolderCloud.PATH, file.getPath());
                CloudMeta cloudMeta = new CloudMeta(cloudMetas.size(), FolderCloud.FOLDERCLOUD, meta);
                addNewCloud(cloudMeta);
                Toast.makeText(activity, getResources().getString(R.string.cloud_added), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Only a folder may be used as a folder cloud", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e("FileSelectorTest", "File select error", e);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
//        cloudSharedPref = new CloudSharedPref(activity);
//        cloudMetas = cloudSharedPref.getClouds(activity);
        switch (requestCode) {
            case NEW_FOLDERCLOUD_REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri = " + uri.toString());

                        try {
                            // Get the file path from the URI
                            String path = FileUtils.getPath(activity, uri);
                            File file = new File(path);
                            if(file.isDirectory()) {
                                ConcurrentHashMap<String, String> meta = new ConcurrentHashMap<>();
                                meta.put(FolderCloud.PATH, file.getPath());
                                CloudMeta cloudMeta = new CloudMeta(cloudMetas.size(), FolderCloud.FOLDERCLOUD, meta);
                                addNewCloud(cloudMeta);
                                Toast.makeText(activity, getResources().getString(R.string.cloud_added), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(activity, "Only a folder may be used as a folder cloud", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Log.e("FileSelectorTest", "File select error", e);
                        }
                    }
                }
                break;
            case DROPBOX_LOGIN_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.v(TAG, "logged into dropbox");
                    String oauth2AccessToken = data.getStringExtra(DropboxAuthenticator.ACCESS_SECRET_NAME);
                    String uid = data.getStringExtra(DropboxHandle.UID);
                    Log.v(TAG, "Access Token: " + oauth2AccessToken);
                    ConcurrentHashMap<String, String> meta = new ConcurrentHashMap<>();
                    meta.put(DropboxHandle.ACCESS_SECRET_NAME, oauth2AccessToken);
                    meta.put(DropboxHandle.UID, uid);
                    CloudMeta cloudMeta = new CloudMeta(cloudMetas.size(), DropboxHandle.DROPBOX, meta);
                    addNewCloud(cloudMeta);
                    Toast.makeText(activity, getResources().getString(R.string.cloud_added), Toast.LENGTH_SHORT).show();
                } else {
                    showToast("Could not link to Dropbox");
                }
                break;
            case DROPBOX_LOGOUT_REQUEST_CODE:
                if(resultCode == Activity.RESULT_OK) {
                    Log.v(TAG, "Unlinked from Dropbox");
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addNewCloud(CloudMeta cloudMeta) {
        //TODO: find out why cloud list's not stored in the shared preferences on restart
        cloudSharedPref.addCloud(activity, cloudMeta);
        //TODO: adds two records for some reason! Fix.
        cloudListAdapter.add(cloudMeta);
        Toast.makeText(activity, "New Cloud Added", Toast.LENGTH_SHORT).show();
    }

    private void showToast(String msg) {
        Toast error = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        error.show();
    }

    @Override
    public void onPause() {
        Log.v(TAG, "CloudListFragment : onPause");
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "CloudListFragment : onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.v(TAG, "CloudListFragment : onDestroyView");
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        Log.v(TAG, "CloudListFragment : onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.v(TAG, "CloudListFragment : onStop");
        super.onStop();
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }


//    private void showChooser() {
//        // Use the GET_CONTENT intent from the utility class
//        Intent target = FileUtils.createGetContentIntent();
//        // Create the chooser Intent
//        Intent intent = Intent.createChooser(
//                target, getString(R.string.foldercloud_chooser_title));
//        try {
//            startActivityForResult(intent, NEW_FOLDERCLOUD_REQUEST_CODE);
//        } catch (ActivityNotFoundException e) {
//            // The reason for the existence of aFileChooser
//        }
//    }
}
