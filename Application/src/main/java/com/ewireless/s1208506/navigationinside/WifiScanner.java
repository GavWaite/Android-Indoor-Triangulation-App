package com.ewireless.s1208506.navigationinside;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.util.List;


/**
 * Author: Gavin Waite
 * Implements a WiFi Scanner which uses the application WifiManager to scan the area for nearby
 * WiFi access points and their signal level. This list is then passed back to the appropriate
 * return Fragment which created the Scanner. The data is then used to either record reference
 * points in the TrainingFragment or to work out the most likely location of the user in the
 * PositioningFragment
 */

public class WifiScanner {

    // Handle to the parent Fragment
    private TrainingFragment returnFragment;
    private PositioningFragment returnFragment2;

    // Link to the WiFi manager of the device and the internal ScanReceiver
    private WifiManager wm;
    private WifiScanReceiver wsr;

    private String wifi_networks[];

    /**
     * Constructor for calling from the TrainingFragment - provides a link back to the Fragment
     * and a reference to the device WifiManager. Calls the initialisation method
     */
    public WifiScanner(TrainingFragment returnFragment, WifiManager wm){
        Log.d("WIFI", "Made a new Wifi Scanner");
        this.returnFragment = returnFragment;
        this.returnFragment2 = null;
        this.wm = wm;
        init();
    }

    /**
     * Constructor for calling from the PositioningFragment - provides a link back to the Fragment
     * and a reference to the device WifiManager. Calls the initialisation method
     */
    public WifiScanner(PositioningFragment returnFragment, WifiManager wm){
        Log.d("WIFI2", "Made a new Wifi Scanner");
        this.returnFragment = null;
        this.returnFragment2 = returnFragment;
        this.wm = wm;
        init();
    }

    /**
     * Initialisation
     * Creates a new WifiScanReceiver and ensure that the device WiFi is Enabled
     */
    private void init(){
        wsr = new WifiScanReceiver();

        if (wm.getWifiState() == WifiManager.WIFI_STATE_DISABLED){
            wm.setWifiEnabled(true);
        }
    }

    /**
     * Scan the area for wifi and return the list of access points
     */
    public void scanForWifi(){
        Log.d("WIFI", "Started scan");
        wm.startScan();
    }

    public WifiScanReceiver getReceiver(){
        return wsr;
    }

    /**
     * Internal class
     * Defines a custom BroadcastReceiver which gets the latest Scan results from the Wifi Manager
     * It stores the information as a String wifi_networks[]
     * It then also passes it back to the parent Fragment as a List<ScanList> where it can be
     * decoded and used
     */
    private class WifiScanReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            List<ScanResult> wifiScanList = wm.getScanResults();
            wifi_networks = new String[wifiScanList.size()];

            for (int i=0; i<wifiScanList.size(); i++){
                wifi_networks[i] = wifiScanList.get(i).SSID +
                        ", " + wifiScanList.get(i).BSSID +
                        ", " + String.valueOf(wifiScanList.get(i).level);
            }

            if (returnFragment != null) {
                Log.d("WIFI", "Received data");
                returnFragment.wifiScanReturn(wifiScanList);
            }
            if (returnFragment2 != null) {
                Log.d("WIFI2", "Received data");
                returnFragment2.wifiScanReturn(wifiScanList);
            }
        }
    }

}
