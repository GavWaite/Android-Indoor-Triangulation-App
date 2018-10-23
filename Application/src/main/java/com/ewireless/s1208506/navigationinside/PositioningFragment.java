package com.ewireless.s1208506.navigationinside;

import android.content.Context;
import android.content.IntentFilter;
import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Gavin Waite
 * The Android Fragment which represents the 'Positioning' Tab of the Navigation application
 *
 * This Fragment controls the updating of a GoogleMap (via API) and the determination of the user's
 * current location. Outdoor positioning is done by an instantiated LocationModel which uses
 * the FusedLocationProvider API. The indoor positioning algorithm is custom and uses the Room
 * database API to access the training reference points and the WiFi Manager API to get the current
 * WiFi data.
 */
public class PositioningFragment extends Fragment implements View.OnClickListener, AdapterView.OnItemSelectedListener{

    // Constant hard-coded LatLng for the engineering department in KB
    private LatLng KB = new LatLng(55.922547, -3.172174);

    // UI handles
    private TextView titleText;
    private TextView infoText;

    private MapView mMapView;    // the View for viewing the map
    private GoogleMap googleMap; // the physical map object

    private Button overlayButton;
    private Button inoutButton;

    private Spinner powerSpin;

    // Local copy of the database
    private List<LocData> dataB;

    // Link back to the MainActivity - for use in setting up the FusedLocationProvider
    private MainActivity ma;


    /**
     * Called upon creation of the PositioningFragment
     * This is where the view is setup for the positioning tab - similar to onCreate() for activity
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Use the positioning_fragment xml to define the view
        View rootView = inflater.inflate(R.layout.positioning_fragment, container, false);

        // Save a reference to the MainActivity
        ma = (MainActivity) getActivity();

        // Perform initialisations for the features in this fragment
        linkInterface(rootView);
        setupLocationServices();
        setupWifi();
        setupMap(rootView, savedInstanceState);

        return rootView;
    }

    /**
     * Setup the Google Map where the user's location will be displayed
     */
    private void setupMap(View rootView, Bundle savedInstanceState){
        mMapView = (MapView) rootView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                // Set the initial map location to be engineering at KB for demo
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(KB, 18.0f));

                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);     // Do not want satellite for now
                mMap.getUiSettings().setCompassEnabled(true);
                mMap.getUiSettings().setRotateGesturesEnabled(true);
                mMap.getUiSettings().setScrollGesturesEnabled(true);
                mMap.getUiSettings().setTiltGesturesEnabled(true);
            }
        });
    }


    /**
     *  Use the FusedLocationProviderClient to get access to location updates for outdoor
     *  positioning. This is abstracted to the LocationModel class
     */
    private FusedLocationProviderClient mFusedLocationClient;
    public LocationModel locationModel;
    private void setupLocationServices(){
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(ma);
        locationModel = new LocationModel(ma, this, mFusedLocationClient);
        locationModel.startLocationUpdates();
    }

    /**
     * Callback from the LocationModel to alert the Fragment to a new location update
     * @param location - the latest Location information
     */
    public void updateLocation(Location location){
        if (!inside) {
            Log.d("Fragment", "Got new location");
            LatLng new_pos = new LatLng(location.getLatitude(), location.getLongitude());
            displayPosition(new_pos); // Physically
            infoText.setText("Acquired location with precision ±"+Float.toString(location.getAccuracy()) + " m" );
        }
    }

    /**
     * Sets up the Fragment internal references to the various interface elements
     * @param rootView - the root View object
     */
    private void linkInterface(View rootView){
        // The 'Toggle Overlay' button
        overlayButton = (Button) rootView.findViewById(R.id.overlayBut);
        overlayButton.setOnClickListener(this);
        overlayButton.setText("Add overlay");

        // The 'Inside/Outside' mode button
        inoutButton = (Button) rootView.findViewById(R.id.inoutBut);
        inoutButton.setOnClickListener(this);
        inoutButton.setText("Switch to Inside");

        // The title text - signifies which mode is actice
        titleText = (TextView) rootView.findViewById(R.id.titleText);
        titleText.setText("Outdoor Positioning");

        // The info text - provides location accuracy information
        infoText = (TextView) rootView.findViewById(R.id.infoText);
        infoText.setText("Gathering location");

        // Power vs. accuracy dropdown - used in outdoor positioning
        powerSpin = (Spinner) rootView.findViewById(R.id.powerSpinner);
        List<String> power_opts = new ArrayList<String>();
        power_opts.add("Low-power");
        power_opts.add("Balanced");
        power_opts.add("High accuracy");
        ArrayAdapter<String> spinAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item, power_opts);
        spinAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        powerSpin.setAdapter(spinAdapter);
        powerSpin.setOnItemSelectedListener(this);
    }

    /**
     * Support for the Power vs. accuracy drop down option
     * Allows the user to change the balance of high accuracy/fast updates vs. power savings for the
     * location gathering system in FusedLocationProvider
     */
    private int accuracy = LocationRequest.PRIORITY_LOW_POWER;
    private int refreshInterval = 5000;
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        String selection = (String) parent.getItemAtPosition(position);
        infoText.setText(selection);

        switch (selection){
            case "Low-power":
                accuracy = LocationRequest.PRIORITY_LOW_POWER;
                refreshInterval = 10000;
                break;
            case "Balanced":
                accuracy = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY;
                refreshInterval = 5000;
                break;
            case "High accuracy":
                accuracy = LocationRequest.PRIORITY_HIGH_ACCURACY;
                refreshInterval = 1000;
                break;
            default:
                break;
        }

        // Update the settings in the LocationModel
        locationModel.modifyLocationRequest(accuracy, refreshInterval);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Auto-generated method stub
    }

    /**
     * Method to update the UI location marker given a LatLng
     * The camera also adjusts to focus on this point
     */
    private Marker locationMarker;
    private void displayPosition(LatLng pos){
        if (locationMarker != null){
            locationMarker.remove();
        }
        locationMarker = googleMap.addMarker(new MarkerOptions().position(pos).title("Current Location"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
    }


    /**
     * Ground overlay with the KB map provided
     * The user can toggle the overlay on and off using a button
     * The position and bearing were manually adjusted to be as accurate as possible to aligning
     * with the satellite data. A custom overlay could be produced for any buildings which wish to
     * support this app.
     */
    private GroundOverlay kb_overlay;
    public void toggleOverlay(){
        if (kb_overlay == null) {
            GroundOverlayOptions KBmap = new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromResource(R.drawable.school_map_small))
                    .position(KB, 160f, 130f)
                    .bearing(238.0f)
                    .transparency(0.4f);
            kb_overlay = googleMap.addGroundOverlay(KBmap);
            overlayButton.setText("Remove overlay");
        }
        else {
            kb_overlay.remove();
            kb_overlay = null;
            overlayButton.setText("Add overlay");
        }

    }

    /**
     * The user can press a button to switch between the inside (wifi) positioning system and the
     * FusedLocationProvider outdoor positioning. When in Inside mode, the power spinner is hidden
     * as it only applies to the frequency and accuracy of the outdoor results
     */
    private boolean inside = false;
    private void toggleInoutMode(){
        if (inside){
            inside = false;
            inoutButton.setText("Switch to Inside");
            titleText.setText("Outdoor Positioning");
            locationModel.startLocationUpdates();
            powerSpin.setVisibility(View.VISIBLE);
        }
        else {
            inside = true;
            inoutButton.setText("Switch to Outside");
            titleText.setText("Inside Positioning");
            locationModel.stopLocationUpdates();
            wifiScanner.scanForWifi();
            powerSpin.setVisibility(View.GONE);
        }
    }

    /**
     * onClick Listener for the two UI buttons
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.overlayBut:
                toggleOverlay();
                break;
            case R.id.inoutBut:
                toggleInoutMode();
                break;
            default:
                // Do nothing
        }
    }

    /**
     * The implementation of the Room database task: load
     * Executes an asynchronous task as required by the Room API
     *
     * Queries the database for all LocData entries and then stores them in a local List<>
     * For a final real-world use-case then only a sub-set of the full database could be loaded
     * based upon matching any WiFi BSSIDs
     */
    private void loadDatabase(){
        new LoadDatabaseTask().execute();
    }
    private class LoadDatabaseTask extends AsyncTask<Void, Void, List<LocData>> {
        @Override
        protected List<LocData> doInBackground(Void... params){
            Log.d("DB","Starting background task");
            return ((MainActivity)getActivity()).db.locDao().getAll();
        }

        @Override
        protected void onPostExecute(List<LocData> locations){
            Log.d("DB","In post execute");

            if (dataB != null) {
                dataB.clear();
            }
            dataB = locations;
            String msg = "Found " + dataB.size() + " locations";
            Log.d("DB_Load",msg);
        }
    }

    /**
     * Initialisation code to setup a WiFi Scanner which will periodically report back with the
     * nearby WiFi network names and signal strengths
     */
    private WifiManager wifiManager;
    public WifiScanner wifiScanner;
    private void setupWifi(){
        wifiManager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanner = new WifiScanner(this, wifiManager);
        wifiScanner.scanForWifi();
    }

    /**
     * Register the listener for WiFi Scanner results on app resume and unregister on app pause
     */
    @Override
    public void onResume() {
        super.onResume();
        getActivity().registerReceiver(wifiScanner.getReceiver(), new IntentFilter(wifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }
    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(wifiScanner.getReceiver());
    }

    /**
     * The callback method from the WifiScanner. Provides the Fragment with a List<> of the latest
     * ScanResult objects. These contain the SSID, BSSID and signal level information for all nearby
     * access points.
     * @param wifiScanList
     */
    public void wifiScanReturn(List<ScanResult> wifiScanList){
        Log.d("WIFI2", "Received returned data");
        if (inside){
            getInsideLocation(wifiScanList);
        }
    }

    /**
     * Core method of the indoor positioning algorithm. Takes the current WiFi Scanner data and
     * compares this to the reference points in the loaded database. It then determines which point
     * is most similar and sets the current location to that point. The point i.d. and the 'accuracy'
     * are output in the info bar.
     * @param wifiScanList
     */
    private void getInsideLocation(List<ScanResult> wifiScanList){
        databaseCount();
        loadDatabase();
        if (numEntries == 0){
            infoText.setText("Database is empty or still loading");
            return;
        }
        else {
            String BSSID_1 = null;
            String BSSID_2 = null;
            String BSSID_3 = null;
            int dB_1 = 0;
            int dB_2 = 0;
            int dB_3 = 0;

            // Get the latest information
            if (wifiScanList.size()>0) {
                BSSID_1 = wifiScanList.get(0).BSSID;
                dB_1 = wifiScanList.get(0).level;
            }
            if (wifiScanList.size()>1) {
                BSSID_2 = wifiScanList.get(1).BSSID;
                dB_2 = wifiScanList.get(1).level;
            }
            if (wifiScanList.size()>2){
                BSSID_3 = wifiScanList.get(2).BSSID;
                dB_3 = wifiScanList.get(2).level;
            }

            // The relative importance of the three strongest WiFi networks in the decision algorithm
            float WIFI_1_WEIGHT = 1.0f;
            float WIFI_2_WEIGHT = 0.6f;
            float WIFI_3_WEIGHT = 0.3f;

            // Initial values of the closest point
            int bestScore = -1000;
            LocData closestPoint = null;
            int primaryError = 0;

            // If the database is still loading
            if (dataB == null){
                return;
            }

            int i = 0;
            // Walk through all reference points and look for the closest match
            for (LocData refPoint : dataB){
                Log.d("TestNo", Integer.toString(i));
                i++;
                int refPoint_score = 0;
                int difference = 0;
                int refPointError = 0;

                // First check the strongest access point
                if (BSSID_1 != null && dB_1 != 0){
                    if (BSSID_1.equals(refPoint.BSSID_1)){
                        difference = Math.abs(refPoint.dB_1 - dB_1);
                        refPoint_score += (int) (WIFI_1_WEIGHT*(100 - difference));
                    }
                    if (BSSID_1.equals(refPoint.BSSID_2)){
                        difference = Math.abs(refPoint.dB_2 - dB_1);
                        refPoint_score += (int) (WIFI_1_WEIGHT*(100 - difference));
                    }
                    if (BSSID_1.equals(refPoint.BSSID_3)){
                        difference = Math.abs(refPoint.dB_3 - dB_1);
                        refPoint_score += (int) (WIFI_1_WEIGHT*(100 - difference));
                    }
                    refPointError += difference;
                }

                // Next check the second strongest access point
                if (BSSID_2 != null && dB_2 != 0){
                    if (BSSID_2.equals(refPoint.BSSID_1)){
                        difference = Math.abs(refPoint.dB_1 - dB_2);
                        refPoint_score += (int) (WIFI_2_WEIGHT*(100 - difference));
                    }
                    if (BSSID_2.equals(refPoint.BSSID_2)){
                        difference = Math.abs(refPoint.dB_2 - dB_2);
                        refPoint_score += (int) (WIFI_2_WEIGHT*(100 - difference));
                    }
                    if (BSSID_2.equals(refPoint.BSSID_3)){
                        difference = Math.abs(refPoint.dB_3 - dB_2);
                        refPoint_score += (int) (WIFI_2_WEIGHT*(100 - difference));
                    }
                    refPointError += difference;
                }
                // Finally check the third strongest access point
                if (BSSID_3 != null && dB_3 != 0){
                    if (BSSID_3.equals(refPoint.BSSID_1)){
                        difference = Math.abs(refPoint.dB_1 - dB_3);
                        refPoint_score += (int) WIFI_3_WEIGHT*(100 - difference);
                    }
                    if (BSSID_3.equals(refPoint.BSSID_2)){
                        difference = Math.abs(refPoint.dB_2 - dB_3);
                        refPoint_score += (int) WIFI_3_WEIGHT*(100 - difference);
                    }
                    if (BSSID_3.equals(refPoint.BSSID_3)){
                        difference = Math.abs(refPoint.dB_3 - dB_3);
                        refPoint_score += (int) WIFI_3_WEIGHT*(100 - difference);
                    }
                    refPointError += difference;
                }

                Log.d("TestNo", "Score: " + Integer.toString(refPoint_score));

                // If the point is the new best then save it and continue looking
                if (refPoint_score > bestScore){
                    bestScore = refPoint_score;
                    closestPoint = refPoint;
                    primaryError = refPointError;
                }
            }

            // If no point was found then notify the user
            if (closestPoint == null){
                infoText.setText("No matching reference point found");
                return;
            }
            // Otherwise signal the found point and set the location on the Google Map
            else {
                LatLng refPos = new LatLng(closestPoint.latitude, closestPoint.longitude);
                displayPosition(refPos);
                infoText.setText("Reference point "+ closestPoint.uid+ " found with err ±" +primaryError +"dB" );
            }

        }
    }


    /**
     * Asynchronous task to count the number of entries
     */
    private void databaseCount(){
        new CountEntriesTask().execute();
    }

    private int numEntries = 0;
    private class CountEntriesTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params){
            Log.d("DB","Starting background task");
            return ((MainActivity)getActivity()).db.locDao().countEntries();
        }

        @Override
        protected void onPostExecute(Integer entries){
            Log.d("DB","In post execute");
            numEntries = entries;
        }
    }

}
