package com.ewireless.s1208506.navigationinside;

import android.location.Location;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;

import com.google.android.gms.tasks.OnSuccessListener;

/**
 * Author: Gavin Waite
 * Encapsulation of the outdoor location gathering mechanism for the Navigation app
 * Uses the FusedLocationProvider API to request location updates and then handle them via
 * callback. The parent Fragment is then notified of the new values.
 */
public class LocationModel {

    // Handles to the parent Activity and Fragment
    private MainActivity ma;
    private PositioningFragment frag;

    // The FusedLocationProviderClient for use with the API is instantiated in the fragment
    // A reference is passed in via the constructor
    private FusedLocationProviderClient flpc;

    // The current location of teh user and the last known location
    private Location lastLocation;
    private Location currentLocation;

    // Customisation variables for the power management of the location updates
    public int priority = LocationRequest.PRIORITY_LOW_POWER;
    public int interval = 10*1000; // 10s

    // Support for Location requesting and callbacks
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    /**
     * Constructor - Setup a new instance of LocationModel and perform initialisation
     * @param ma   - reference to parent MainActivity
     * @param frag - reference to parent PositioningFragment
     * @param flpc - reference to instantiated FusedLocationProviderClient
     */
    public LocationModel(MainActivity ma, PositioningFragment frag, FusedLocationProviderClient flpc) {
        this.ma = ma;
        this.frag = frag;
        this.flpc = flpc;

        init();
    }

    /**
     * Initialisation
     * Sets the current location to the last known location
     * Define the callback method to respond to a location update and alert the parent fragment
     * so that the UI can be updated
     */
    private void init(){
        // Define a new location request with the default accuracy/speed settings
        createLocationRequest(priority, interval);

        // Initialise the current location to the last known location
        currentLocation = getLastLoc();

        // Setup a listener callback for new location updates from the LocationProvider
        mLocationCallback = new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult){
                if (locationResult == null){
                    return;
                }
                // Get the new location from the callback and assign it to the currentLocation
                // The parent fragment should then be notified to trigger a UI update
                for (Location location : locationResult.getLocations()){
                    currentLocation = location;
                    frag.updateLocation(currentLocation); // notify the parent fragment to update UI
                }
            }
        };
    }

    /**
     * Request to the FusedLocationProviderClient to begin returning Location updates
     * These are handled by the mLocationCallback method that was defined in init()
     */
    public void startLocationUpdates() {
        try {
            flpc.requestLocationUpdates(mLocationRequest, mLocationCallback, null);
        }catch (SecurityException se){
            se.printStackTrace();
        }
    }

    /**
     * Request to the FusedLocationProviderClient to stop sending Location updates
     * Can save power when not needed
     */
    public void stopLocationUpdates(){
        flpc.removeLocationUpdates(mLocationCallback);
    }

    /**
     * Returns the last known location from the FusedLocationProviderClient
     */
    public Location getLastLoc() {
        // Get the last knownlocation of the device from the LocationProvider for
        // initialising currentLocation
        try {
            flpc.getLastLocation().addOnSuccessListener(ma, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    if (location != null) {
                        lastLocation = location;
                    }
                    // location can occasionally be null on a fresh device
                }
            });
        }
        catch (SecurityException se){
            se.printStackTrace();
        }
        return lastLocation;
    }

    /**
     * Define the parameters for the location updates
     * @param priority - controls the accuracy of the updates given
     * @param interval - controls the desired time between updates
     */
    private void createLocationRequest(int priority, int interval) {
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(interval);
        mLocationRequest.setFastestInterval(interval/2);
        mLocationRequest.setPriority(priority);
    }

    /**
     * Public method to support the user changing the location update parameters
     * Stops the location updates, changes the parameters and then restarts it
     * @param priority - the new priority level
     * @param interval - the new interval time
     */
    public void modifyLocationRequest(int priority, int interval){
        stopLocationUpdates();
        createLocationRequest(priority, interval);
        startLocationUpdates();
    }
}
