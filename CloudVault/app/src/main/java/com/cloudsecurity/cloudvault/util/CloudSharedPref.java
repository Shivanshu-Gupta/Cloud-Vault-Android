package com.cloudsecurity.cloudvault.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

import com.cloudsecurity.cloudvault.cloud.CloudMeta;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudSharedPref {
    private static final String TAG = "CloudVault";

	public static final String PREFS_NAME = "CLOUDVAULT";
	public static final String CLOUDS_META = "CloudsMetaData";
	public static final String NEXT_ID = "Next Available Cloud ID";

	public CloudSharedPref(Context context) {
		super();
	}

	// These four methods are used for maintaining clouds meta data.
	public void saveClouds(Context context, List<CloudMeta> clouds) {
        Log.v(TAG, "CloudSharedPref: saveClouds");
        SharedPreferences settings;
		Editor editor;

		settings = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		editor = settings.edit();

		Gson gson = new Gson();
		String jsonClouds = gson.toJson(clouds);
        Log.v(TAG, "CloudsMetaData: " + jsonClouds);
		editor.putString(CLOUDS_META, jsonClouds);
		if(clouds.size() > 0)
			editor.putInt(NEXT_ID, clouds.get(clouds.size() - 1).getId() + 1);
		else
			editor.putInt(NEXT_ID, 0);
		editor.commit();
	}

	public void addCloud(Context context, CloudMeta cloudMeta) {
        Log.v(TAG, "CloudSharedPref: addClouds");
        List<CloudMeta> clouds = getClouds(context);
		if (clouds == null)
			clouds = new ArrayList<>();
		clouds.add(cloudMeta);
		saveClouds(context, clouds);
	}

	public void removeCloud(Context context, CloudMeta cloudMeta) {
		Log.v(TAG, "CloudSharedPref : removeClouds");
		ArrayList<CloudMeta> clouds = getClouds(context);
		if (clouds != null) {
			clouds.remove(cloudMeta);
			saveClouds(context, clouds);
		}
	}

	public ArrayList<CloudMeta> getClouds(Context context) {
		Log.v(TAG, "CloudSharedPref: getClouds");
		SharedPreferences settings;
		List<CloudMeta> clouds = new ArrayList<>();

		settings = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		if (settings.contains(CLOUDS_META)) {
			String jsonClouds = settings.getString(CLOUDS_META, null);
			Gson gson = new Gson();
			CloudMeta[] cloudArray = gson.fromJson(jsonClouds,
					CloudMeta[].class);

			clouds = Arrays.asList(cloudArray);
			clouds = new ArrayList<>(clouds);
		} else {
            Log.i(TAG, "preferences don't contain CloudsMetaData. Creating the new preference.");
			saveClouds(context, clouds);
        }

		return (ArrayList<CloudMeta>) clouds;
	}

    public int getNextID(Context context) {
        Log.v(TAG, "CloudSharedPref: getClouds");
        SharedPreferences settings;
        int nextUID;
        settings = context.getSharedPreferences(PREFS_NAME,
                Context.MODE_PRIVATE);
        if(!settings.contains(NEXT_ID)) {
            List<CloudMeta> clouds = new ArrayList<>();
            saveClouds(context, clouds);
        }
        nextUID = settings.getInt(NEXT_ID, 0);

        return nextUID;
    }

	public int getCloudCount(Context context) {
		SharedPreferences settings;
		List<CloudMeta> clouds;

		settings = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);

		if (settings.contains(CLOUDS_META)) {
			String jsonClouds = settings.getString(CLOUDS_META, null);
			Gson gson = new Gson();
			CloudMeta[] cloudArray = gson.fromJson(jsonClouds,
					CloudMeta[].class);

			clouds = Arrays.asList(cloudArray);
			clouds = new ArrayList<CloudMeta>(clouds);
		} else
			return -1;

		return clouds.size();
	}
}
