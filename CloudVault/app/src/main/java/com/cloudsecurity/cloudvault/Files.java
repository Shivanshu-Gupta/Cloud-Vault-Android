package com.cloudsecurity.cloudvault;

import android.app.Activity;
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
import android.app.Fragment;
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

import com.ipaulpro.afilechooser.utils.FileUtils;

import java.io.File;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link Files.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link Files#newInstance} factory method to
 * create an instance of this fragment.
 */
public class Files extends ListFragment {
    private static final String TAG = "CloudVault";
    public static final String ARG_ITEM_ID = "cloud_list_fragment";

    //view elements
    private Button uploadButton;

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

    public Files() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment Files.
     */
    //unused as of now.
    public static Files newInstance(String param1, String param2) {
        Files fragment = new Files();
        Bundle args = new Bundle();
//        args.putString(ARG_PARAM1, param1);
//        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, "Files : onCreate");
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
        Log.v(TAG, "Files : onStart");
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(getActivity(), VaultClient.class);
        getActivity().startService(intent);
        getActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.v(TAG, "Files : onCreateView");
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_files, container, false);
        uploadButton = (Button) view.findViewById(R.id.upload_button);
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.v(TAG, "Files : upload button clicked.");
                showChooser();
            }
        });
        return view;

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.v(TAG, "Files : onViewCreated");
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
                    if (action.equals(VaultClient.FILE_UPLOADED))
                        task = new LoadCursorTask().execute();
                }
            }
        };
        filter = new IntentFilter(VaultClient.FILE_UPLOADED);
    }
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        Log.v(TAG, "Files : onCreateContextMenu");
        Log.i("ContextMenu", "Context Menu Called");
        super.onCreateContextMenu(menu, v, menuInfo);
        Log.i("ContextMenu","View ID = " + v.getId());
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.menu_long_press, menu);
        Log.i("ContextMenu", "Menu Inflation Done");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        Log.v(TAG, "Files : onContextItemSelected");
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        Log.i("ContextMenu", "OnContextItem Selected");
        switch(item.getItemId()) {
            case R.id.download:
                RelativeLayout selectedRow = ((RelativeLayout) info.targetView);
                String  filename = ((TextView) selectedRow.getChildAt(0)).getText().toString();
                client.download(filename);
                return true;
            case R.id.delete:
                Log.i("ContextMenu","Delete : " + info.position);
                // remove stuff here
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.v(TAG, "Files : onActivityResult");
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        Log.v(TAG, "Files : onResume");
        super.onResume();
        mLocalBroadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    public void onPause() {
        Log.v(TAG, "Files : onPause");
        super.onPause();
        mLocalBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void onStop() {
        Log.v(TAG, "Files : onStop");
        super.onStop();
        Intent intent = new Intent(getActivity(), VaultClient.class);
        getActivity().stopService(intent);
        // Unbind from the service
        if (mBound) {
            getActivity().unbindService(mConnection);
            mBound = false;
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "Files : onDestroy");
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
                            .query(DatabaseHelper.TABLE,
                                    new String[]{"ROWID AS _id",
                                            DatabaseHelper.FILENAME,
                                            DatabaseHelper.SIZE},
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
            return (doQuery());
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
