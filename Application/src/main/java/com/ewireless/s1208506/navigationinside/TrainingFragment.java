package com.ewireless.s1208506.navigationinside;

import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlay;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Author: Gavin Waite
 * The Android Fragment which represents the 'Training' Tab of the Navigation application
 *
 * This Fragment allows the user to specify a start and end Location by tapping directly on a
 * Google Map. A line is then drawn on the map connected these points. The user should then walk
 * between these two points at a constant speed.
 */
public class TrainingFragment extends Fragment implements View.OnClickListener {

    private LatLng KB = new LatLng(55.922547, -3.172174);

    // UI map elements
    private MapView mMapView;    // The map container View
    private GoogleMap googleMap; // The map itself
    private MapClickListener mapClick; // Listener object for handling map touches

    // UI buttons
    private Button startPointButton;
    private Button endPointButton;
    private Button toggleOverlayButton;
    private Button togglePointsButton;
    private Button recordButton;

    // UI TextViews
    private TextView walkingTime;
    private TextView numReadings;

    // Current state of the training phase
    // Ensures that recording can only begin once both points have been set
    private boolean SettingStartPos = false;
    private boolean StartPoint_Set = false;
    private boolean SettingEndPos = false;
    private boolean EndPoint_Set = false;
    private boolean recording = false;

    // For storing the current start and end position
    private LatLng startPos;
    private LatLng endPos;

    // Also store a reference to the Markers and Polyline path so they can be removed later
    private Marker startMarker;
    private Marker endMarker;
    private Polyline drawnPath;

    // Stopwatch functionality
    private Handler stopwatchHandler;
    private long startTime;
    private long currentTime;
    private long endTime;
    private int Minutes, Seconds, MilliSeconds;

    /**
     * Called upon creation of the TrainingFragment
     * This is where the view is setup for the positioning tab - similar to onCreate() for activity
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.training_fragment, container, false);

        // Obtain references to all UI elements and perform setup for each feature
        linkInterface(rootView);
        setupWifi();
        setupStopWatch();
        setupMap(rootView, savedInstanceState);

        // Start an asynchronous load from the database
        loadDatabase();

        return rootView;
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
     * Sets up the Fragment internal references to the various interface elements
     * @param rootView - the root View object
     */
    private void linkInterface(View rootView){
        startPointButton = (Button) rootView.findViewById(R.id.startPointBut);
        startPointButton.setOnClickListener(this);

        endPointButton = (Button) rootView.findViewById(R.id.endPointBut);
        endPointButton.setOnClickListener(this);

        toggleOverlayButton = (Button) rootView.findViewById(R.id.toggleOverlay);
        toggleOverlayButton.setOnClickListener(this);

        togglePointsButton = (Button) rootView.findViewById(R.id.togglePoints);
        togglePointsButton.setOnClickListener(this);

        recordButton = (Button) rootView.findViewById(R.id.recordBut);
        recordButton.setOnClickListener(this);

        numReadings = (TextView) rootView.findViewById(R.id.numReadings);

        walkingTime = (TextView) rootView.findViewById(R.id.walkingTime);
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

                // Allow for the in-built Google Maps location system to help the training user
                // find their training location
                try {
                    googleMap.setMyLocationEnabled(true);
                }catch (SecurityException se){
                    Log.e("LOCATION", "Got a security exception");
                }

                googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                googleMap.getUiSettings().setCompassEnabled(true);
                googleMap.getUiSettings().setRotateGesturesEnabled(true);
                googleMap.getUiSettings().setScrollGesturesEnabled(true);
                googleMap.getUiSettings().setTiltGesturesEnabled(true);

                mapClick = new MapClickListener();
                googleMap.setOnMapClickListener(mapClick);
            }
        });
    }

    /**
     *
     */
    private void setupStopWatch(){
        walkingTime.setText("00:00:00");
        stopwatchHandler = new Handler();
    }

    // Button handling -----------------------------------------------------------------------------

    /**
     * onClick Listener for the five UI buttons
     */
    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startPointBut:
                setStartPoint();
                break;
            case R.id.endPointBut:
                setEndPoint();
                break;
            case R.id.toggleOverlay:
                toggleOverlay();
                break;
            case R.id.togglePoints:
                togglePoints();
                break;
            case R.id.recordBut:
                startRecordingSession();
                break;
            default:
                // Do nothing
        }
    }

    /**
     * When the start point button is pressed, an existing start point Marker should be removed
     * Then the MapClickListener waits for the user to tap the map, signified by SettingStartPos
     */
    private void setStartPoint(){
        if (!recording){
            if (StartPoint_Set){
                // Remove old marker
                startMarker.remove();
            }
            SettingStartPos = true;
        }
    }

    /**
     * When the end point button is pressed, an existing end point Marker should be removed
     * Then the MapClickListener waits for the user to tap the map, signified by SettingEndPos
     */
    private void setEndPoint(){
        if (!recording){
            if (EndPoint_Set){
                // Remove old marker
                endMarker.remove();
            }
            SettingEndPos = true;
        }
    }

    /**
     * Ground overlay with the KB map provided
     * The user can toggle the overlay on and off using a button
     * The position and bearing were manually adjusted to be as accurate as possible to aligning
     * with the satellite data. A custom overlay could be produced for any buildings which wish to
     * support this app.
     */
    private GroundOverlay kb_overlay;
    private void toggleOverlay(){
        if (kb_overlay == null) {
            GroundOverlayOptions KBmap = new GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromResource(R.drawable.school_map_small))
                    .position(KB, 160f, 130f)
                    .bearing(238.0f)
                    .transparency(0.4f);
            kb_overlay = googleMap.addGroundOverlay(KBmap);
            toggleOverlayButton.setText("Remove overlay");
        }
        else {
            kb_overlay.remove();
            kb_overlay = null;
            toggleOverlayButton.setText("Add overlay");
        }
    }

    /**
     * Reference point visibility toggle button
     * The user may wish to see which points they have already collected, perhaps to decide where
     * to start a new training session to get optimal coverage.
     * This button either iterates through all the points in the database and draws them on the map
     * or removes them all. References to the Markers are stored in the refPoints[] list to allow
     * them to be removed
     */
    private List<Marker> refPoints = new ArrayList<>();
    private boolean pointsShown = false;
    private void togglePoints(){
        if (pointsShown){
            if (refPoints != null) {
                for (Marker refPoint : refPoints) {
                    refPoint.remove();
                }
                refPoints.clear();
            }
            togglePointsButton.setText("Show points");
            pointsShown = false;
        }
        else {
            loadDatabase();
            if (dataB != null){
                for (LocData data : dataB){
                    LatLng pos = new LatLng(data.latitude, data.longitude);
                    MarkerOptions markOpt = new MarkerOptions().position(pos);
                    Marker newMark = googleMap.addMarker(markOpt);
                    refPoints.add(newMark);
                }
            }
            togglePointsButton.setText("Hide points");
            pointsShown = true;
        }
    }

    /**
     * The record/ end record button
     * This first checks that the user has defined a start and end point and then starts the
     * stopwatch and begins checking the WiFi scanner to gather reference points.
     * A second press will stop the recording and prompt the points to be saved into the database
     */
    private void startRecordingSession(){
        if (!recording) {
            if (StartPoint_Set && EndPoint_Set) {
                startTime = SystemClock.uptimeMillis();

                beginStopwatch();
                recordButton.setText("Stop Training Session");
                recording = true;
            } else {
                Toast.makeText(getContext(), "Need to set path first", Toast.LENGTH_LONG).show();
            }
        }
        else {
            stopStopwatch();
            recordButton.setText("Start Training Session");
            recording = false;
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Recording Session methods -------------------------------------------------------------------

    /**
     * Implementation of a OnMapClickListener
     * If the start or end point button has been pressed, this listens for the user to tap the
     * Google Map and then adds a Marker to the appropriate point and saves it as the start or end
     * point. If the other point is already set then it also draws a line between the two points.
     */
    class MapClickListener implements GoogleMap.OnMapClickListener{
        @Override
        public void onMapClick(LatLng latLng) {
            if (SettingStartPos) {
                MarkerOptions start = new MarkerOptions().position(
                        new LatLng(latLng.latitude, latLng.longitude)).title("Start");
                startPos = latLng;
                startMarker = googleMap.addMarker(start);
                SettingStartPos = false;
                StartPoint_Set = true;
                if (EndPoint_Set){
                    drawPath();
                }
            }
            else if (SettingEndPos){
                MarkerOptions end = new MarkerOptions().position(
                        new LatLng(latLng.latitude, latLng.longitude)).title("End");
                endPos = latLng;
                endMarker = googleMap.addMarker(end);
                SettingEndPos = false;
                EndPoint_Set = true;
                if (StartPoint_Set){
                    drawPath();
                }
            }
        }
    }

    /**
     * Draws a red line on the google map between teh start and end point to make it easier for
     * the user to follow the path and check that their desired training route is correct
     */
    private void drawPath(){
        // First need to remove the path if it already exists
        if (drawnPath != null){
            drawnPath.remove();
        }
        PolylineOptions path = new PolylineOptions().add(startPos, endPos)
                .width(4)
                .color(Color.RED);

        drawnPath = googleMap.addPolyline(path);
    }

    /**
     * Upon starting recording an on-screen stopwatch is started for the user's convenience
     * The WiFi scanner is also explicitly requested to start scanning
     * trainingSession is a List which contains all the readings obtained in the current recording
     * session. These are TrainingReading objects which associate the WiFi Scan data with the time
     * at which the scan was obtained
     */
    private List<TrainingReading> trainingSession;
    private void beginStopwatch(){
        stopwatchHandler.postDelayed(stopWatchRun, 0);
        wifiScanner.scanForWifi();

        if (trainingSession == null){
            trainingSession = new ArrayList<>();
        }
        else {
            trainingSession.clear();
        }
    }

    /**
     * This ends a training session
     * The on-screen stopwatch is reset and then the collected trainingSession is analysed
     * The relative time of each reading can be computed given the end time and the time
     * of each reading (normalised to start at 0). This relative time is then used to find the
     * approximate location of the reading by interpolating between the start and end points of the
     * training path. This requires the user to walk at an approximately constant speed.
     * These are then added to the database.
     */
    private void stopStopwatch(){

        endTime = currentTime;
        stopwatchHandler.removeCallbacks(stopWatchRun);
        walkingTime.setText("00:00:00");

        for (TrainingReading reading : trainingSession){
            float relative_time = ((float)reading.timeOfReading / (float)endTime);
            Log.d("Time", Float.toString(relative_time));

            LatLng posOfReading = new LatLng(
                    ((1-relative_time)*startPos.latitude + relative_time*endPos.latitude),
                    ((1-relative_time)*startPos.longitude + relative_time*endPos.longitude));

            addNewTrainingPointToDatabase(posOfReading, reading.wifiScanData);
        }
        trainingSession.clear();
        loadDatabase();
    }

    /**
     * The repeating Runnable method which models the stopwatch
     * It calculates the currentTime, normalised by the startTime and updates the UI stopwatch
     */
    public Runnable stopWatchRun = new Runnable() {

        public void run() {
            currentTime = SystemClock.uptimeMillis() - startTime;

            Seconds = (int) (currentTime / 1000);
            Minutes = Seconds / 60;
            Seconds = Seconds % 60;
            MilliSeconds = (int) (currentTime % 1000);

            walkingTime.setText("" + Minutes + ":"
                    + String.format("%02d", Seconds) + ":"
                    + String.format("%03d", MilliSeconds));

            stopwatchHandler.postDelayed(this, 0);
        }

    };

    /**
     * This sets up the WiFi Scanner needed to collect data fro the reference points
     */
    private WifiManager wifiManager;
    public WifiScanner wifiScanner;
    private void setupWifi(){
        wifiManager = (WifiManager)getActivity().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiScanner = new WifiScanner(this, wifiManager);
    }

    /**
     * The callback from the WiFi Scanner to indicate that a ScanResult is available
     * If the user is in an active recording session then a new TrainingReading is created to store
     * the result
     */
    private int sensorEntries = 0;
    public void wifiScanReturn(List<ScanResult> wifiScanList){
        Log.d("WIFI", "Received returned data");
        // If the user is capturing, then save the current ScanResult
        if (recording){
            sensorEntries++;
            numReadings.setText("Readings: "+ Integer.toString(sensorEntries));
            trainingSession.add(new TrainingReading(currentTime, wifiScanList));
        }
    }

    /**
     * Sets up a new Reference point to be added to the database
     * Uses the WiFi BSSIDs as these uniquely identify an access point whereas the SSID can be
     * the same for multiple and is subject to change
     * @param pos - The calculated LatLng of the point
     * @param data - The raw WiFi List<ScanResult>
     */
    private void addNewTrainingPointToDatabase(LatLng pos, List<ScanResult> data){

        // Create a new LocData entry for the Room database API
        LocData newEntry = new LocData();

        // Set the uid to the next unused integer
        newEntry.uid = num_entries;
        num_entries++;
        // Set the latitude and longitude fields
        newEntry.latitude = pos.latitude;
        newEntry.longitude = pos.longitude;
        // Set the BSSID and dB fields, passing in NA if less than 3 WiFi networks were found
        String[] BSSIDs = new String[3];
        int[] dBs = new int[3];
        for (int i=0; i < 3; i++){
            if (i < data.size()){
                BSSIDs[i] = data.get(i).BSSID;
                dBs[i] = data.get(i).level;
            }
            else {
                BSSIDs[i] = "NA";
                dBs[i] = -200;
            }
        }
        newEntry.BSSID_1 = BSSIDs[0];
        newEntry.dB_1 = dBs[0];
        newEntry.BSSID_2 = BSSIDs[1];
        newEntry.dB_2 = dBs[1];
        newEntry.BSSID_3 = BSSIDs[2];
        newEntry.dB_3 = dBs[2];

        addToDatabase(newEntry);
    }

    // ---------------------------------------------------------------------------------------------
    // Database access -----------------------------------------------------------------------------

    private List<LocData> dataB; // the full database itself
    private int num_entries = 0;

    /**
     * The implementation of the Room database task: load
     * Executes an asynchronous task as required by the Room API
     *
     * Queries the database for all LocData entries and then stores them in a local List<>
     * For a final real-world use-case then only a sub-set of the full database could be loaded
     * based upon location
     */
    private void loadDatabase(){
        new AsyncTask<Void, Void, List<LocData>>(){
            @Override
            protected List<LocData> doInBackground(Void... params){
                Log.d("DB","Starting background task");
                return ((MainActivity)getActivity()).db.locDao().getAll();
            }

            @Override
            protected void onPostExecute(List<LocData> locations){
                Log.d("DB","In post execute");

                if (locations != null) {
                    dataB = locations;
                    num_entries = locations.size();
                }
                else {
                    num_entries = 0;
                }



            }
        }.execute();
    }

    /**
     * The implementation of the Room database task: insert
     * Executes an asynchronous task as required by the Room API
     *
     * Adds a new LocData entry into the Room database
     */
    private void addToDatabase(LocData entry){
        new InsertIntoDatabaseTask().execute(entry);
    }

    private class InsertIntoDatabaseTask extends AsyncTask<LocData, Void, Void>{
        @Override
        protected Void doInBackground(LocData... new_val){
            Log.d("DB","Adding to db");
            ((MainActivity)getActivity()).db.locDao().insertOne(new_val[0]);
            return null;
        }
    }
    // ---------------------------------------------------------------------------------------------
}
