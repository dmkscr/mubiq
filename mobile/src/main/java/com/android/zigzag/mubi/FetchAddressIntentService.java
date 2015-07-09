package com.android.zigzag.mubi;


import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FetchAddressIntentService extends IntentService {

    private static final String             TAG                            = "Mubiq - Fetch Address";

    protected ResultReceiver                mReceiver;

    public FetchAddressIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        String errorMessage = "";

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);

        if (mReceiver == null) {
            Log.d(TAG, "No receiver received.");
            return;
        }

        Location location = intent.getParcelableExtra( Constants.LOCATION_DATA_EXTRA );

        if (location == null) {
            errorMessage = getString(R.string.no_location_data_provided);
            Log.d(TAG, errorMessage);
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
            return;
        }

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses = null;

        try {
            addresses = geocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    // In this sample, get just a single address.
                    1);
        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage = getString(R.string.service_not_available);
            Log.e( TAG, errorMessage, ioException );
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage = getString(R.string.invalid_lat_long_used);
            Log.e( TAG, errorMessage + ". " + "Latitude = " + location.getLatitude() + ", Longitude = " + location.getLongitude(), illegalArgumentException );
        }

        // Handle case where no address was found.
        if (addresses == null || addresses.size()  == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage = getString(R.string.no_address_found);
                Log.e( TAG, errorMessage );
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for(int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
//            Log.i( TAG, getString(R.string.address_found ));
            deliverResultToReceiver(Constants.SUCCESS_RESULT, TextUtils.join(System.getProperty("line.separator"), addressFragments));
        }

    }

    private void deliverResultToReceiver(int resultCode, String message) {
        Bundle bundle = new Bundle();
        bundle.putString(Constants.RESULT_DATA_KEY, message);
//        Log.d( TAG, "deliverResultToReceiver bundle: " + bundle );
        mReceiver.send(resultCode, bundle);
    }

}
