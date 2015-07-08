package com.android.zigzag.mubi;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

public final class Constants {

    /**
     * Fetch Address
     */
    public static final int         SUCCESS_RESULT          = 0;
    public static final int         FAILURE_RESULT          = 1;
    public static final String      PACKAGE_NAME            = "com.android.zigzag.mubi";
    public static final String      RECEIVER                = PACKAGE_NAME + ".RECEIVER";
    public static final String      RESULT_DATA_KEY         = PACKAGE_NAME + ".RESULT_DATA_KEY";
    public static final String      LOCATION_DATA_EXTRA     = PACKAGE_NAME + ".LOCATION_DATA_EXTRA";

    /**
     * Geofencing
     */
    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";
    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 50;
    /**
     * Map for storing information about airports in the San Francisco bay area.
     */
    // TODO: Dynamically create geofences based on users location instead of hard coded
    public static final HashMap<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<String, LatLng>();
    static {
        // San Francisco International Airport.
        BAY_AREA_LANDMARKS.put("SFO", new LatLng(37.621313, -122.378955));

        // Googleplex.
        BAY_AREA_LANDMARKS.put("GOOGLE", new LatLng(37.422611,-122.0840577));
    }
}