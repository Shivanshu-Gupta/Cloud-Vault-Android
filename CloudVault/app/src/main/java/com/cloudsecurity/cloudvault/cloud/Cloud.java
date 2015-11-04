package com.cloudsecurity.cloudvault.cloud;

import android.content.Context;

/**
 * Created by Shivanshu Gupta on 28-10-2015.
 */
public interface Cloud {
    public boolean upload(Context context, String cloudFileName, byte[] data);
    public byte[] download(Context context, String cloudFileName);

}
