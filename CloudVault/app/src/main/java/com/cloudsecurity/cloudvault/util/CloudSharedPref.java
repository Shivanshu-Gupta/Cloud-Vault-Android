package com.cloudsecurity.cloudvault.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.cloudsecurity.cloudvault.cloud.CloudMeta;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudSharedPref {

	public static final String PREFS_NAME = "CLOUDVAULT";
	public static final String CLOUDS_META = "CloudsMetaData";

	public CloudSharedPref() {
		super();
	}

	// This four methods are used for maintaining clouds.
	public void saveClouds(Context context, List<CloudMeta> clouds) {
		SharedPreferences settings;
		Editor editor;

		settings = context.getSharedPreferences(PREFS_NAME,
				Context.MODE_PRIVATE);
		editor = settings.edit();

		Gson gson = new Gson();
		String jsonClouds = gson.toJson(clouds);

		editor.putString(CLOUDS_META, jsonClouds);

		editor.commit();
	}

	public void addCloud(Context context, CloudMeta product) {
		List<CloudMeta> clouds = getClouds(context);
		if (clouds == null)
			clouds = new ArrayList<CloudMeta>();
		clouds.add(product);
		saveClouds(context, clouds);
	}

	public void removeCloud(Context context, CloudMeta product) {
		ArrayList<CloudMeta> clouds = getClouds(context);
		if (clouds != null) {
			clouds.remove(product);
			saveClouds(context, clouds);
		}
	}

	public ArrayList<CloudMeta> getClouds(Context context) {
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
			return null;

		return (ArrayList<CloudMeta>) clouds;
	}
}
