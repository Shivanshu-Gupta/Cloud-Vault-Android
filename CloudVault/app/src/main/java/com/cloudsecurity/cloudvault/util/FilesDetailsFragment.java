package com.cloudsecurity.cloudvault.util;

/**
 * Created by Noman on 11/28/2015.
 */
public class FilesDetailsFragment {
    String fileName;
    long fileSize;
    String[] cloudList;
    int minClouds;
    String timeStamp;

    public FilesDetailsFragment(String name, long size, String[] list, int minClouds, String time)
    {
        fileName = name;
        fileSize = size;
        cloudList = list;
        this.minClouds = minClouds;
        timeStamp = time;
    }

    public String getMessage()
    {
        String answer;
        String sizeDetails = getSizeMessage();
        String timeDetails = getTimeMessage();
        String cloudDetails = getCloudMessage();
        String minCloudsDetails = getMinCloudsMessage();
        answer = sizeDetails + "\n" + timeDetails + "\n" + cloudDetails + "\n" + minCloudsDetails;
        return answer;
    }

    String getSizeMessage()
    {
        if(fileSize > 0){
            String reply = "FILE SIZE : ";
            reply = reply + fileSize + " bytes\n";
            return reply;
        }

        return "\n";
    }
    String getTimeMessage()
    {
        String reply = "LAST MODIFIED : ";
        if(timeStamp.equals(""))
            reply = reply + "Not Available\n";
        else
            reply = reply + timeStamp + "\n";
        return reply;
    }

    String getCloudMessage()
    {
        String reply = "STORED ON : \n";
        if(cloudList == null || cloudList.equals("")) {
            reply = reply + "Not Available\n";
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

    String getMinCloudsMessage() {
        if(minClouds > 0){
            String reply = "MINIMUM CLOUD COUNT : ";
            reply = reply + minClouds + "\n";
            return reply;
        }

        return "\n";
    }
}
