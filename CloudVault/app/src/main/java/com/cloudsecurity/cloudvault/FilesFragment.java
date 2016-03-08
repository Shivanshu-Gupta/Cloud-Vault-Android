package com.cloudsecurity.cloudvault;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
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
import android.widget.CursorAdapter;
import android.widget.RelativeLayout;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.cloudsecurity.cloudvault.util.FilesDetailsFragment;
import com.cloudsecurity.cloudvault.settings.SettingsActivity;
import com.google.gson.Gson;
import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;

public class FilesFragment extends ListFragment {
    private static final String TAG = "CloudVault";
    public static final String ARG_ITEM_ID = "files_fragment";

    //view elements
    private Button uploadButton;
    private Button syncButton;
    private Button settingButton;

    private DatabaseHelper db = null;
    private AsyncTask task = null;
    private OnFragmentInteractionListener mListener;

    VaultClient client;
    boolean mBound;
    private ServiceConnection mConnection;

    private LocalBroadcastManager mLocalBroadcastManager = null;
    private BroadcastReceiver receiver = null;
    private IntentFilter filter;

    //intent request codes
    private static final int UPLOAD_REQUEST_CODE = 100; // onActivityResult request code
    private static final int SETTING_REQUEST_CODE = 200; // onActivityResult request code

    public FilesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FilesFragment.
     */
    //unused as of now.
    public static FilesFragment newInstance() {
        FilesFragment fragment = new FilesFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "FilesFragment : onCreate");
        super.onCreate(savedInstanceState);
        mConnection = new ServiceConnection() {

            @Override
            public void onServiceConnected(ComponentName className,
                                           IBinder service) {
                VaultClient.ClientBinder binder = (VaultClient.ClientBinder) service;
                client = binder.getService();
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName arg0) {
                mBound = false;
            }
        };
        setRetainInstance(true);
    }

    @Override
    public void onStart() {
        Log.v(TAG, "FilesFragment : onStart");
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(getActivity(), VaultClient.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "FilesFragment : onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        uploadButton = (Button) view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "FilesFragment : upload button clicked.");
                showChooser();
            }
        });

        syncButton = (Button) view.findViewById(R.id.sync_button);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "FilesFragment: sync button clicked.");
                client.sync();
            }
        });

        settingButton = (Button) view.findViewById(R.id.setting_button);
        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "FilesFragment : setting button clicked.");
                showSettingPreferences();
            }
        });


        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.v(TAG, "FilesFragment : onViewCreated");
        SimpleCursorAdapter adapter =
                new SimpleCursorAdapter(getActivity(), R.layout.file_row,
                        null, new String[]{
                        DatabaseHelper.FILENAME,
                        DatabaseHelper.SIZE},
                        new int[]{R.id.fileName, R.id.size},
                        0);

        setListAdapter(adapter);

        registerForContextMenu(getListView());

        db = DatabaseHelper.getInstance(getActivity());
        task = new LoadCursorTask().execute();
        mLocalBroadcastManager =  LocalBroadcastManager.getInstance(getActivity());
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if(intent !=null) {
                    final String action = intent.getAction();
                    if (action.equals(VaultClient.FILE_UPLOADED) || action.equals(VaultClient.FILE_DELETED)) {
                        Log.v(TAG, "FilesFragment : Reloading files list");
                        task = new LoadCursorTask().execute();
                    } else if(action.equals(VaultClient.FILES_DB_SYNCED)) {
                        Log.v(TAG, "FilesFragment : Reloading files list");
                        db = DatabaseHelper.getInstanceFresh(getActivity());
                        task = new LoadCursorTask().execute();
                    }
                }
            }
        };
        filter = new IntentFilter(VaultClient.FILE_UPLOADED);
        filter.addAction(VaultClient.FILE_DELETED);
        filter.addAction(VaultClient.FILES_DB_SYNCED);
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.v(TAG, "FilesFragment : onCreateContextMenu");
        Log.i("ContextMenu", "Context Menu Called");
        super.onCreateContextMenu(menu, v, menuInfo);
        Log.i("ContextMenu", "View ID = " + v.getId());
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_long_press, menu);
        Log.i("ContextMenu", "Menu Inflation Done");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.v(TAG, "FilesFragment : onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.i("ContextMenu", "OnContextItem Selected");
        RelativeLayout selectedRow = ((RelativeLayout) info.targetView);
        String  filename = ((TextView) selectedRow.getChildAt(0)).getText().toString();
        switch(item.getItemId()) {
            case R.id.download:
                client.download(filename);
                return true;
            case R.id.delete:
                client.delete(filename);
                return true;
            case R.id.properties:
                Cursor result = db.getReadableDatabase()
                                    .query(DatabaseHelper.FILES_TABLE,
                                            new String[]{"ROWID AS _id",
                                                    DatabaseHelper.SIZE,
                                                    DatabaseHelper.CLOUDLIST,
                                                    DatabaseHelper.MINCLOUDS,
                                                    DatabaseHelper.TIMESTAMP},
                                            DatabaseHelper.FILENAME + "=" + "'" + filename + "'",
                                            null, null, null, DatabaseHelper.FILENAME);
                long size = 0;
                String[] cloudsUsed = null;
                int minCLouds = 0;
                String timestamp = "";
                if(result.moveToFirst())
                {
                    size = result.getLong(result.getColumnIndex(DatabaseHelper.SIZE));
                    Gson gson = new Gson();
                    cloudsUsed = gson.fromJson(result.getString(result.getColumnIndex(DatabaseHelper.CLOUDLIST)), String[].class);
                    minCLouds = result.getInt(result.getColumnIndex(DatabaseHelper.MINCLOUDS));
                    timestamp = result.getString(result.getColumnIndex(DatabaseHelper.TIMESTAMP));
                }
                String message = new FilesDetailsFragment(filename, size, cloudsUsed, minCLouds, timestamp).getMessage();
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(message).setTitle(filename);
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }


    private void showChooser() {
        // Use the GET_CONTENT intent from the utility class
        Intent target = FileUtils.createGetContentIntent();
        // Create the chooser Intent
        Intent intent = Intent.createChooser(
                target, getString(R.string.upload_chooser_title));
        try {
            startActivityForResult(intent, UPLOAD_REQUEST_CODE);
        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
        }
    }

    private void showSettingPreferences() {

        Intent myIntent = new Intent (getActivity(),SettingsActivity.class);
//        try {
            startActivity(myIntent);
//        } catch (ActivityNotFoundException e) {
            // The reason for the existence of aFileChooser
//        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "FilesFragment : onActivityResult");
        switch (requestCode) {
            case UPLOAD_REQUEST_CODE:
                // If the file selection was successful
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(TAG, "Uri = " + uri.toString());

                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(getActivity(), uri);
                            File file = new File(path);
                            Toast.makeText(getActivity(),
                                    "File Selected: " + path, Toast.LENGTH_LONG).show();
                            client.upload(file);
                        } catch (Exception e) {
                            Log.e("FileSelectorTest", "File select error", e);
                        }
                    }
                }
                break;
            case SETTING_REQUEST_CODE:
                Toast.makeText(getActivity(),"Settings Done",Toast.LENGTH_LONG).show();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        Log.v(TAG, "FilesFragment : onResume");
        super.onResume();
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "FilesFragment : onPause");
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void onStop() {
        Log.v(TAG, "FilesFragment : onStop");
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "FilesFragment : onDestroy");
        Intent intent = new Intent(getActivity(), VaultClient.class);
        getActivity().stopService(intent);
        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
        if (task != null) {
            task.cancel(false);
        }

        ((CursorAdapter) getListAdapter()).getCursor().close();
        db.close();

        super.onDestroy();
    }

    abstract private class BaseTask<T> extends AsyncTask<T, Void, Cursor> {
        @Override
        public void onPostExecute(Cursor result) {
            //opPostExecute receives the result of the task i.e. the cursor here.
            //the cursor is then assigned to the listAdapter of the fragment
            ((CursorAdapter) getListAdapter()).changeCursor(result);
            task = null;
        }

        protected Cursor doQuery() {
            Cursor result =
                    db
                            .getReadableDatabase()
                            .query(DatabaseHelper.FILES_TABLE,
                                    new String[]{"ROWID AS _id",
                                            DatabaseHelper.FILENAME,
                                            DatabaseHelper.SIZE,
                                            DatabaseHelper.CLOUDLIST},
                                    null, null, null, null, DatabaseHelper.FILENAME);

            result.getCount();
            //return the cursor obtained
            return (result);
        }
    }

    private class LoadCursorTask extends BaseTask<Void> {
        @Override
        protected Cursor doInBackground(Void... params) {

            //return the cursor obtained from doing the query
            Cursor result = doQuery();
            db.close();
            return result;
        }
    }

    //    @Override
//    public void onAttach(Activity activity) {
//        super.onAttach(activity);
//        try {
//            mListener = (OnFragmentInteractionListener) activity;
//        } catch (ClassCastException e) {
//            throw new ClassCastException(activity.toString()
//                    + " must implement OnFragmentInteractionListener");
//        }
//    }

    //    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }
}
