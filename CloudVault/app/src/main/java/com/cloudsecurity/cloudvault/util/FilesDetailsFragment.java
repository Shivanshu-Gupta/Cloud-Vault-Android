package com.cloudsecurity.cloudvault.util;

import java.util.ArrayList;

/**
 * Created by Noman on 11/28/2015.
 */
public class FilesDetailsFragment {
    String fileName;
    String fileSize;
    String[] cloudList;
    String timeStamp;

    public FilesDetailsFragment(String name, String size, String[] list, String time)
    {
        fileName = name;
        fileSize = size;
        cloudList = list;
        timeStamp = time;
    }

    public String getMessage()
    {
        String answer;
        String sizeDetails = getSizeMessage();
        String timeDetails = getTimeMessage();
        String cloudDetails = getCloudMessage();
        answer = sizeDetails + "\n" + timeDetails + "\n" + cloudDetails;
        return answer;
    }

    String getSizeMessage()
    {
        String reply = "FILE SIZE : ";
        if(fileSize.equals(""))
            reply = reply + "Not Available";
        else
            reply = reply + fileSize + " bytes\n";
        return reply;

    }
    String getTimeMessage()
    {
        String reply = "LAST MODIFIED\n";
        if(timeStamp.equals(""))
            reply = reply + "Not Available";
        else
            reply = reply + timeStamp + "\n";
        return reply;
    }

    String getCloudMessage()
    {
        String reply = "CLOUD DETAILS\n";
        if(cloudList == null || cloudList.equals("")) {
            reply = reply + "Not Available";
        } else {
            int index = 1;
            for(String cloud : cloudList){
                String[] cloudParams = cloud.split("--");
                reply = reply + index + ". " + cloudParams[0] + ": " + cloudParams[1] +  "\n";
                index++;
            }
        }
        return reply;
    }
}
