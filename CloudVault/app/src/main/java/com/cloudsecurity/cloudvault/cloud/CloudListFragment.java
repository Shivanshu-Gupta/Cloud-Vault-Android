package com.cloudsecurity.cloudvault.cloud;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.ListFragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.DatabaseHelper;
import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.cloud.dropbox.AccountInfo;
import com.cloudsecurity.cloudvault.cloud.dropbox.Dropbox;
import com.cloudsecurity.cloudvault.cloud.dropbox.DropboxAuthenticator;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;
import com.cloudsecurity.cloudvault.util.DirectoryChooserDialog;
import com.cloudsecurity.cloudvault.util.FilesDetailsFragment;
import com.google.gson.Gson;

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

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    public static final String ARG_ITEM_ID = "cloud_list_fragment";

    private static final int DROPBOX_LOGIN_REQUEST_CODE = 2; // onActivityResult request code
    private static final int DROPBOX_LOGOUT_REQUEST_CODE = -2; // onActivityResult request code

    private Activity activity;
    private ListView cloudListView;
    private List<CloudMeta> cloudMetas;
    private CloudListAdapter cloudListAdapter;
    private CloudSharedPref cloudSharedPref;

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver receiver = null;
    private IntentFilter filter;

    private OnFragmentInteractionListener mListener;

    private Button addCloudButton;

    private CloudMeta newCloudMeta;

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

//        saveButton = (Button) view.findViewById(R.id.save_clouds);
//        saveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showSaveCloudsDialog();
//            }
//        });
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.v(TAG, "CloudListFragment : onViewCreated");
        mLocalBroadcastManager =  LocalBroadcastManager.getInstance(getActivity());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent !=null) {
                    switch (intent.getAction()) {
                        case AccountInfo.ACCOUNT_INFO :
                            long dbxID = intent.getLongExtra("uid", 0);
                            Log.v(TAG, "CloudListFragment : onViewCreated : uid " + String.valueOf(dbxID));
                            if(dbxID != 0) {
                                String uid = Long.toString(dbxID);
                                String email = intent.getStringExtra("email");
                                newCloudMeta.getMeta().put("uid", uid);
                                newCloudMeta.getMeta().put("email", email);
                                addNewCloud(newCloudMeta);
                            } else {
                                showToast("Failed to add new Dropbox cloud.");
                            }
                            break;
                        case AccountInfo.FETCH_ACCOUNT_FAILED:
                            showToast("Failed to add new Dropbox cloud.");
                            break;
                    }
                }
            }
        };
        registerForContextMenu((ListView) getActivity().findViewById(R.id.list_clouds));
        filter = new IntentFilter(AccountInfo.ACCOUNT_INFO);
        filter.addAction(AccountInfo.FETCH_ACCOUNT_FAILED);
    }


    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.v(TAG, "CloudListFragment : onCreateContextMenu");
        Log.i("ContextMenu", "Context Menu Called");
        super.onCreateContextMenu(menu, v, menuInfo);
        Log.i("ContextMenu", "View ID = " + v.getId());
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.cloud_long_press, menu);
        Log.i("ContextMenu", "Menu Inflation Done");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.v(TAG, "CloudListFragment : onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.i("ContextMenu", "OnContextItem Selected");
//        RelativeLayout selectedRow = ((RelativeLayout) info.targetView);
        int pos = info.position;
//        String cloudname = ((TextView) selectedRow.getChildAt(0)).getText().toString();
        switch(item.getItemId()) {
            case R.id.delete_cloud:
                showDeleteCloudsDialog(pos);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showDeleteCloudsDialog(final int pos) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        // Yes button clicked
                        CloudMeta cloudMeta = cloudMetas.get(pos);
                        String cloudGenericName = cloudMeta.getGenericName();
                        removeCloud(cloudMeta);
                        mListener.onCloudDeleted(cloudGenericName);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        // No button clicked
                        // do nothing
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Are you sure?")
                .setMessage("Deleting a cloud will lead to loss of all the data on that cloud!")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    private void showAddCloudDialog() {
        AddCloudDialogFragment addCloudDialog = new AddCloudDialogFragment();
        addCloudDialog.setTargetFragment(this, 0);
        addCloudDialog.show(getFragmentManager(), "new cloud");
    }

    @Override
    public void onResume() {
        Log.v(TAG, "CloudListFragment : onResume");
        getActivity().setTitle(R.string.clouds);
        super.onResume();
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "CloudListFragment : onPause");
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(receiver);
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
        ArrayList<String> uids = new ArrayList<>();
        String[] alreadyAuthedUids;
        switch (cloudType) {
            case 0:
                //Create a new Dropbox cloud
                //Start authentication for a new dropbox cloud
                Intent intent = new Intent(getActivity(), DropboxAuthenticator.class);
                intent.setAction(DropboxAuthenticator.ACTION_LOGIN);
                for (CloudMeta cloudMeta : cloudMetas) {
                    if (cloudMeta.getName().equals(Dropbox.DROPBOX)) {
                        uids.add(cloudMeta.getMeta().get("uid"));
                    }
                }
                alreadyAuthedUids = uids.toArray(new String[uids.size()]);
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
            boolean alreadyAddedPath = false;

            showToast("Chosen Path: " + chosenDir);
            File file = new File(chosenDir);
            String path = file.getPath();
            for (CloudMeta cloudMeta : cloudMetas) {
                if (cloudMeta.getName().equals(FolderCloud.FOLDERCLOUD)
                        && cloudMeta.getMeta().get(FolderCloud.PATH).equals(path)) {
                    alreadyAddedPath = true;
                    break;
                }
            }
            if (alreadyAddedPath) {
                showAlert("Add New Cloud", "The cloud has already been added. Please add a fresh cloud.");
            } else if (file.isDirectory()) {
                ConcurrentHashMap<String, String> meta = new ConcurrentHashMap<>();
                meta.put(FolderCloud.PATH, path);
                //the path is also used as the uid
                meta.put("uid", path);
                CloudMeta cloudMeta = new CloudMeta(cloudSharedPref.getNextID(activity), FolderCloud.FOLDERCLOUD, meta);
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
            case DROPBOX_LOGIN_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.v(TAG, "logged into dropbox");
                    String oauth2AccessToken = data.getStringExtra(DropboxAuthenticator.ACCESS_SECRET_NAME);
                    Log.v(TAG, "Access Token: " + oauth2AccessToken);
                    ConcurrentHashMap<String, String> meta = new ConcurrentHashMap<>();
                    meta.put(Dropbox.ACCESS_SECRET_NAME, oauth2AccessToken);

                    //insert of adding the new cloud already, store it till the uid is obtained.
                    newCloudMeta = new CloudMeta(cloudSharedPref.getNextID(activity), Dropbox.DROPBOX, meta);
                    //start the service to fetch the UID
                    Intent intent = new Intent(getActivity(), AccountInfo.class);
                    intent.setAction(AccountInfo.ACTION_FETCH_ACCOUNT);
                    intent.putExtra(AccountInfo.ACCESS_SECRET_NAME, oauth2AccessToken);
                    getActivity().startService(intent);
                } else {
                    showToast("Could not link to Dropbox");
                }
                break;
            case DROPBOX_LOGOUT_REQUEST_CODE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.v(TAG, "Unlinked from Dropbox");
                }
                break;

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addNewCloud(CloudMeta cloudMeta) {
        cloudSharedPref.addCloud(activity, cloudMeta);
        cloudListAdapter.add(cloudMeta);

        if (cloudMetas.size() > 1) {
            mListener.onCloudAdded();
        }

        Toast.makeText(activity, "New Cloud Added", Toast.LENGTH_SHORT).show();
    }

    public void removeCloud(CloudMeta cloudMeta) {
        cloudSharedPref.removeCloud(activity, cloudMeta);
        cloudListAdapter.remove(cloudMeta);

        Toast.makeText(activity, "Cloud Deleted", Toast.LENGTH_SHORT).show();
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

    private void showToast(String msg) {
        Toast error = Toast.makeText(getActivity(), msg, Toast.LENGTH_LONG);
        error.show();
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
        void onCloudAdded();
        void onCloudDeleted(String genericName);

        void onCloudsDangerChanged();
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
