package com.ewireless.s1208506.navigationinside;

import android.net.wifi.ScanResult;

import java.util.List;

/**
 * Author: Gavin Waite
 * Support class to store information about the training readings taken during a training session
 */
public class TrainingReading {

    public long timeOfReading;
    public List<ScanResult> wifiScanData;

    public TrainingReading(long time, List<ScanResult> data){
        this.timeOfReading = time;
        this.wifiScanData = data;
    }
}
