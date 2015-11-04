package com.cloudsecurity.cloudvault.cloud;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cloudsecurity.cloudvault.R;
import com.cloudsecurity.cloudvault.util.CloudSharedPref;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created on 25-Oct-15.
 */
public class CloudListAdapter extends ArrayAdapter<CloudMeta> {

    private Context context;
    List<CloudMeta> clouds;
    CloudSharedPref sharedPreference;

    public CloudListAdapter(Context context, List<CloudMeta> clouds) {
        super(context, R.layout.cloudmeta_list_item, clouds);
        this.context = context;
        this.clouds = clouds;
        sharedPreference = new CloudSharedPref(context);
    }

    private class ViewHolder {
        TextView cloudNameTxt;
        TextView cloudMetaTxt;
    }

    @Override
    public int getCount() {
        return clouds.size();
    }

    @Override
    public CloudMeta getItem(int position) {
        return clouds.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.cloudmeta_list_item, null);
            holder = new ViewHolder();
            holder.cloudNameTxt = (TextView) convertView
                    .findViewById(R.id.txt_cld_name);
            holder.cloudMetaTxt = (TextView) convertView
                    .findViewById(R.id.txt_cld_meta);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        CloudMeta cloudMeta = (CloudMeta) getItem(position);
        holder.cloudNameTxt.setText(cloudMeta.getName());
        ConcurrentHashMap<String, String> meta = cloudMeta.getMeta();
        //TODO: output the meta data properly for each cloud type
        holder.cloudMetaTxt.setText(meta.toString());

        return convertView;
    }

//    /*Checks whether a particular cloudMeta exists in CloudSharedPrefs*/
//    public boolean checkFavoriteItem(CloudMeta checkCloudMeta) {
//        boolean check = false;
//        List<CloudMeta> favorites = sharedPreference.getFavorites(context);
//        if (favorites != null) {
//            for (CloudMeta cloudMeta : favorites) {
//                if (cloudMeta.equals(checkCloudMeta)) {
//                    check = true;
//                    break;
//                }
//            }
//        }
//        return check;
//    }

    @Override
    public void add(CloudMeta cloudMeta) {
        super.add(cloudMeta);
//        clouds.add(cloudMeta);
        notifyDataSetChanged();
    }

    @Override
    public void remove(CloudMeta cloudMeta) {
        super.remove(cloudMeta);
//        clouds.remove(cloudMeta);
        notifyDataSetChanged();
    }
}


