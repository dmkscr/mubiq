package com.android.zigzag.mubi;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.ResultReceiver;
import android.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnFingerprintType;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnList;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupLocalStream;
import com.gracenote.gnsdk.GnLookupLocalStreamIngest;
import com.gracenote.gnsdk.GnLookupLocalStreamIngestStatus;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnMusicId;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnAudioSource;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnLookupLocalStreamIngestEvents;
import com.gracenote.gnsdk.IGnStatusEvents;
import com.gracenote.gnsdk.IGnSystemEvents;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


// MainActivity
public class SendMessageToWear extends ActionBarActivity implements ResultCallback<Status> {

    private String TAG = "Mubiq";

    /**
     * Messaging
     */
    private final String MESSAGE_PATH = "/message";
    private GoogleApiClient mGoogleApiClient;
    private NodeApi.NodeListener nodeListener;
    private String remoteNodeId;
    private MessageApi.MessageListener messageListener;
    private Handler handler;

    /**
     * Location grabbing
     */
    private Location mLastLocation;
    private String mLastUpdateTime;
    private AddressResultReceiver mResultReceiver;
    private boolean mAddressRequested;
    private String mAddressOutput;
    protected static final String ADDRESS_REQUESTED_KEY = "address-request-pending";
    protected static final String LOCATION_ADDRESS_KEY = "location-address";

    /**
     * Geofencing
     */
    protected ArrayList<Geofence> mGeofenceList;
    private boolean mGeofencesAdded;
    private PendingIntent mGeofencePendingIntent;
    private SharedPreferences mSharedPreferences;

    private ParseGeoPoint geoPoint;

    /**
     * Gracenote
     */
    static final String gnsdkClientId = "9148416";
    static final String gnsdkClientTag = "EA1C43BD1FFE51ED7ECF272A2F04DA45";
    static final String gnsdkLicenseFilename = "license.txt";
    private GnManager gnManager;
    private GnUser gnUser;
    private GnMusicId gnMusicId;

    private String albumTitle = "";
    private String artist = "";
    private String track = "";
    private String newTrack;
    private String coverArtUrl = "";
    private String nearestAddress = "";
    private Bitmap coverArt;

    private Activity activity;
    private Context context;
    private ListView matchList;
    private MatchAdapter matchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.message_to_wear);

        activity = this;
        context = this.getApplicationContext();
        handler = new Handler();

        matchList = (ListView) findViewById(R.id.match_list);


        // check the client id and tag have been set
        if ((gnsdkClientId == null) || (gnsdkClientTag == null)) {
            showError("Please set Client ID and Client Tag");
            return;
        }

        // get the gnsdk license from the application assets
        String gnsdkLicense = null;
        if ((gnsdkLicenseFilename == null) || (gnsdkLicenseFilename.length() == 0)) {
            showError("License filename not set");
        } else {
            gnsdkLicense = getAssetAsString(gnsdkLicenseFilename);
            if (gnsdkLicense == null) {
                showError("License file not found: " + gnsdkLicenseFilename);
                return;
            }
        }

        try {

            gnManager = new GnManager(context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString);
            gnManager.systemEventHandler(new SystemEvents());
            gnUser = new GnUser(new GnUserStore(context), gnsdkClientId, gnsdkClientTag, TAG);

            GnStorageSqlite.enable();


            // enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
            //GnLookupLocalStream.enable();

            Thread localeThread = new Thread(
                    new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
                            GnLanguage.kLanguageEnglish,
                            GnRegion.kRegionGlobal,
                            GnDescriptor.kDescriptorDefault,
                            gnUser)
            );
            localeThread.start();

            Thread ingestThread = new Thread(new LocalBundleIngestRunnable(context));
            ingestThread.start();

            gnMusicId = new GnMusicId(gnUser);
            gnMusicId.options().lookupData(GnLookupData.kLookupDataContent, true);
            gnMusicId.options().lookupData(GnLookupData.kLookupDataSonicData, true);
            gnMusicId.options().preferResultCoverart(true);
            gnMusicId.options().resultSingle(true);

        } catch (GnException e) {

            Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            showError(e.errorAPI() + ": " + e.errorDescription());
            return;

        } catch (Exception e) {
            if (e.getMessage() != null) {
                Log.e(TAG, e.getMessage());
                showError(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return;

        }


        nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                Log.v(TAG, "MOBILE onPeerConnected");
                remoteNodeId = node.getId();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplication(), getString(R.string.peer_connected), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onPeerDisconnected(Node node) {
                Log.v(TAG, "MOBILE onPeerDisconnected");
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplication(), getString(R.string.peer_disconnected), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };


        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(final MessageEvent messageEvent) {
                Log.v(TAG,"onMessageReceived");
                if (messageEvent.getPath().equals(MESSAGE_PATH)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "Message from wear received.");

                            String result = new String(messageEvent.getData());
                            identifyFingerprint(result);
                        }
                    });
                }
            }

        };


        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Log.d(TAG, "onConnected: Wearable.MessageApi.addListener and NodeApi.addListener registered.");
                Wearable.NodeApi.addListener(mGoogleApiClient, nodeListener);
                Wearable.MessageApi.addListener(mGoogleApiClient, messageListener);
                // If there is a connected node, get it's id that is used when sending messages
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                            remoteNodeId = getConnectedNodesResult.getNodes().get(0).getId();
                            Log.d(TAG, "remoteNodeId from mobile: " + remoteNodeId);
                        } else{
                            Log.d(TAG, "remoteNodeId not working: ");
                        }
                    }
                });

                // TODO do this on every positive match
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

                if (mLastLocation != null) {
                    if (!Geocoder.isPresent()) {
                        Log.d(TAG, "" + R.string.no_geocoder_available);
                        Toast.makeText(getApplication(), getString(R.string.no_geocoder_available), Toast.LENGTH_LONG).show();
                        return;
                    }

                    if (mAddressRequested) {
                        startIntentService();
                    }
                }

            }


            @Override
            public void onConnectionSuspended(int i) {
                // tbd
            }
        })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE)
                            Toast.makeText(getApplicationContext(), getString(R.string.wearable_api_unavailable), Toast.LENGTH_LONG).show();
                    }
                })
                .addApi(Wearable.API)
                .addApi(LocationServices.API)
                .build();

        mResultReceiver = new AddressResultReceiver(new Handler());

        // Set defaults, then update using values stored in the Bundle.
        mAddressRequested = true;
        mAddressOutput = "";
        updateValuesFromBundle(savedInstanceState);

        // Empty list for storing geofences.
        mGeofenceList = new ArrayList<Geofence>();
        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;
        // Retrieve an instance of the SharedPreferences object.
        mSharedPreferences = getSharedPreferences(Constants.SHARED_PREFERENCES_NAME, MODE_PRIVATE);
        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(Constants.GEOFENCES_ADDED_KEY, false);
        // Get the geofences used. Geofence data is hard coded in this sample.
        populateGeofenceList();

    }

    private void identifyFingerprint(String fingerprintData) {
        try {
            GnResponseAlbums list = gnMusicId.findAlbums(fingerprintData, GnFingerprintType.kFingerprintTypeStream6);

            Log.v(TAG, "found " + list.resultCount() + " matches");

            if (list.resultCount() > 0) {
                GnAlbumIterator iterator = list.albums().getIterator();
                int i = 1;
                while (iterator.hasNext()) {
                    GnAlbum album = iterator.next();

                    coverArtUrl = album.coverArt().asset(GnImageSize.kImageSizeLarge).url();
                    coverArtUrl = "http://" + coverArtUrl;

                    albumTitle = album.title().display();

                    artist = album.trackMatched().artist().name().display();

                    if (artist.isEmpty()) {
                        artist = album.artist().name().display();
                    }

                    if (album.trackMatched() != null) {
                        track = album.trackMatched().title().display();
                    }

                    // TODO re-enable
                    if( !track.equals(newTrack) ) {

                        Log.v(TAG, "track: " + track + ", artist: " + artist + ", album: " + albumTitle + ", coverArtUrl: " + coverArtUrl);

                        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                        mLastUpdateTime = DateFormat.getDateTimeInstance().format(new Date());

                        if (mLastLocation != null) {
                            geoPoint = new ParseGeoPoint(mLastLocation.getLatitude(), mLastLocation.getLongitude());
                            post();
                        }

                        newTrack = track;

                        i++;

                   } else{
                        Log.v(TAG, "duplicated track, skipping");
                   }
                }
            }

        } catch (GnException e) {
            e.printStackTrace();
        }
    }

    private class getBitmapFromUrl extends AsyncTask<String, String, Bitmap> {

        @Override
        protected Bitmap doInBackground(String... args) {
            try {

                PutDataMapRequest dataMap = PutDataMapRequest.create("/albumDetails");

                Log.d(TAG,"doInBackground");
                if(!coverArtUrl.equals("http://")){
                    coverArt = BitmapFactory.decodeStream((InputStream)new URL(coverArtUrl).getContent());
                } else{
                    coverArt = null;
                }
                if(coverArt== null) {
                    Log.d(TAG, "cover null");

                } else {
                    Asset asset = createAssetFromBitmap(coverArt);
                    dataMap.getDataMap().putAsset("coverImg", asset);
                }

                Log.d(TAG, "YYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYYY");

                dataMap.getDataMap().putString("albumTitle", " " + albumTitle);
                dataMap.getDataMap().putString("artist", " " + artist);
                dataMap.getDataMap().putString("track", " " + track);
                dataMap.getDataMap().putString("nearestAddress", " " + nearestAddress.replaceAll("\\r\\n|\\r|\\n", " "));

                PutDataRequest request = dataMap.asPutDataRequest();
                PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                        .putDataItem(mGoogleApiClient, request);



            } catch (Exception e) {
                e.printStackTrace();
            }
            return coverArt;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Log.d(TAG, "onPostExecute");
        }

    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private void post() {

        Log.e(TAG, "post to PARSE");

        MubiqPost post = new MubiqPost();

        post.setLocation(geoPoint);
        post.setAlbum(albumTitle);
        post.setArtist(artist);
        post.setTrack(track);
        post.setUser(ParseUser.getCurrentUser());
        post.setCoverArtUrl(coverArtUrl);
        post.setNearestAddress(nearestAddress);
        ParseACL acl = new ParseACL();

        // Give public read access
        acl.setPublicReadAccess(true);
        post.setACL(acl);

        // Save the post
        post.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                setMatchList();
            }
        });

        sendingResult();

    }

    /**
     * Send matched result back to wear
     */
    public void sendingResult() {

        Log.e(TAG, "sendingResult");

        Log.d(TAG, "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ");

        AlbumDataMap albumDataMap = new AlbumDataMap(albumTitle,artist,track,nearestAddress);

        if (mGoogleApiClient.isConnected()) {

            new getBitmapFromUrl().execute(coverArtUrl);

            Log.e(TAG, "sendingResult end");

        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        menu.findItem(R.id.action_settings).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(SendMessageToWear.this, SettingsActivity.class));
                return true;
            }
        });
        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();

        setMatchList();

        // Check is Google Play Services available
        int connectionResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (connectionResult != ConnectionResult.SUCCESS) {
            // Google Play Services is NOT available. Show appropriate error dialog
            GooglePlayServicesUtil.showErrorDialogFragment(connectionResult, this, 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        } else {
            mGoogleApiClient.connect();
        }
    }


    @Override
    protected void onPause() {
        // Unregister Node and Message listeners, disconnect GoogleApiClient
        Wearable.NodeApi.removeListener(mGoogleApiClient, nodeListener);
        Wearable.MessageApi.removeListener(mGoogleApiClient, messageListener);
        mGoogleApiClient.disconnect();
        super.onPause();
    }

    /**
     * Loads a locale
     */
    class LocaleLoadRunnable implements Runnable {
        GnLocaleGroup group;
        GnLanguage language;
        GnRegion region;
        GnDescriptor descriptor;
        GnUser user;


        LocaleLoadRunnable(
                GnLocaleGroup group,
                GnLanguage language,
                GnRegion region,
                GnDescriptor descriptor,
                GnUser user) {
            this.group = group;
            this.language = language;
            this.region = region;
            this.descriptor = descriptor;
            this.user = user;
        }

        @Override
        public void run() {
            try {

                GnLocale locale = new GnLocale(group, language, region, descriptor, gnUser);
                locale.setGroupDefault();

            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }
    }


    /**
     * Loads a local bundle for MusicID-Stream lookups
     */
    class LocalBundleIngestRunnable implements Runnable {
        Context context;

        LocalBundleIngestRunnable(Context context) {
            this.context = context;
        }

        public void run() {
            try {

                InputStream bundleInputStream = null;
                int ingestBufferSize = 1024;
                byte[] ingestBuffer = new byte[ingestBufferSize];
                int bytesRead = 0;

                GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

                try {

                    bundleInputStream = context.getAssets().open("1557.b");

                    do {

                        bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
                        if (bytesRead == -1)
                            bytesRead = 0;

                        ingester.write(ingestBuffer, bytesRead);

                    } while (bytesRead != 0);

                } catch (IOException e) {
                    e.printStackTrace();
                }

                ingester.flush();

            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }

        }
    }


    /**
     * Receives system events from GNSDK
     */
    class SystemEvents implements IGnSystemEvents {
        @Override
        public void localeUpdateNeeded(GnLocale locale) {

            // Locale update is detected
            try {
                locale.update(gnUser);
            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }

        @Override
        public void listUpdateNeeded(GnList list) {
            // List update is detected
            try {
                list.update(gnUser);
            } catch (GnException e) {
                Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            }
        }

        @Override
        public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
            // only invoked if a memory warning limit is configured
        }
    }

    /**
     * GNSDK status event delegate
     */
    private class StatusEvents implements IGnStatusEvents {

        @Override
        public void statusEvent(GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable) {
            Log.v(TAG, "IGnStatusEvents statusEvent (status): " + status);
        }

    }

    ;

    /**
     * GNSDK bundle ingest status event delegate
     */
    private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents {

        @Override
        public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
            Log.v(TAG, "IGnLookupLocalStreamIngestEvents statusEvent (status): " + status);
        }
    }


    /**
     * Helpers to read license file from assets as string
     */
    private String getAssetAsString(String assetName) {

        String assetString = null;
        InputStream assetStream;

        try {

            assetStream = this.getApplicationContext().getAssets().open(assetName);
            if (assetStream != null) {

                java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");

                assetString = s.hasNext() ? s.next() : "";
                assetStream.close();

            } else {
                Log.e(TAG, "Asset not found:" + assetName);
            }

        } catch (IOException e) {

            Log.e(TAG, "Error getting asset as string: " + e.getMessage());

        }

        return assetString;
    }


    /**
     * Helper to show and error
     */
    private void showError(String errorMessage) {
        Log.v(TAG, "showError (errorMessage): " + errorMessage);
    }


    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }


    class AddressResultReceiver extends ResultReceiver {
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {

            mAddressOutput = resultData.getString(Constants.RESULT_DATA_KEY);
            displayAddressOutput();

            if (resultCode == Constants.SUCCESS_RESULT) {
//                Log.d(TAG, "" + getString(R.string.address_found));
            }

            mAddressRequested = false;

        }
    }


    /**
     * Updates fields based on data stored in the bundle.
     */
    private void updateValuesFromBundle(Bundle savedInstanceState) {

        if (savedInstanceState != null) {

            // Check savedInstanceState to see if the address was previously requested.
            if (savedInstanceState.keySet().contains(ADDRESS_REQUESTED_KEY)) {
                mAddressRequested = savedInstanceState.getBoolean(ADDRESS_REQUESTED_KEY);
            }
            // Check savedInstanceState to see if the location address string was previously found
            // and stored in the Bundle. If it was found, display the address string in the UI.
            if (savedInstanceState.keySet().contains(LOCATION_ADDRESS_KEY)) {
                mAddressOutput = savedInstanceState.getString(LOCATION_ADDRESS_KEY);
                displayAddressOutput();
            }
        }
    }

    protected void displayAddressOutput() {
        nearestAddress = mAddressOutput;
    }


    /**
     * Builds and returns a GeofencingRequest. Specifies the list of geofences to be monitored.
     * Also specifies how the geofence notifications are initially triggered.
     */
    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification when the geofence is added and if the device
        // is already inside that geofence.
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(mGeofenceList);

        // Return a GeofencingRequest.
        return builder.build();
    }

    /**
     * Adds geofences, which sets alerts to be notified when the device enters or exits one of the
     * specified geofences. Handles the success or failure results returned by addGeofences().
     */
    // TODO: Dynamically create geofences based on users location instead of hard coded
    public void addGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            LocationServices.GeofencingApi.addGeofences(
                    mGoogleApiClient,
                    // The GeofenceRequest object.
                    getGeofencingRequest(),
                    // A pending intent that that is reused when calling removeGeofences(). This
                    // pending intent is used to generate an intent when a matched geofence
                    // transition is observed.
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    /**
     * Removes geofences, which stops further notifications when the device enters or exits
     * previously registered geofences.
     */
    // TODO: Dynamically create geofences based on users location instead of hard coded
    public void removeGeofencesButtonHandler(View view) {
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences.
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult().
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e(TAG, "Invalid location permission. " +
                "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }

    /**
     * Runs when the result of calling addGeofences() and removeGeofences() becomes available.
     * Either method can complete successfully or with an error.
     * <p/>
     * Since this activity implements the {@link ResultCallback} interface, we are required to
     * define this method.
     *
     * @param status The Status returned through a PendingIntent when addGeofences() or
     *               removeGeofences() get called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(Constants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.commit();

            // Update the UI. Adding geofences enables the Remove Geofences button, and removing
            // geofences enables the Add Geofences button.
            setButtonsEnabledState();

            Toast.makeText(
                    this,
                    getString(mGeofencesAdded ? R.string.geofences_added :
                            R.string.geofences_removed),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            // Get the status code for the error and log it using a user-friendly message.
            String errorMessage = GeofenceErrorMessages.getErrorString(this,
                    status.getStatusCode());
            Log.e(TAG, errorMessage);
        }
    }

    /**
     * Gets a PendingIntent to send with the request to add or remove Geofences. Location Services
     * issues the Intent inside this PendingIntent whenever a geofence transition occurs for the
     * current list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceTransitionsIntentService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /**
     * This sample hard codes geofence data. A real app might dynamically create geofences based on
     * the user's location.
     */
    // TODO: Dynamically create geofences based on users location instead of hard coded
    public void populateGeofenceList() {
        for (Map.Entry<String, LatLng> entry : Constants.BAY_AREA_LANDMARKS.entrySet()) {

            mGeofenceList.add(new Geofence.Builder()
                    // Set the request ID of the geofence. This is a string to identify this
                    // geofence.
                    .setRequestId(entry.getKey())

                            // Set the circular region of this geofence.
                    .setCircularRegion(
                            entry.getValue().latitude,
                            entry.getValue().longitude,
                            Constants.GEOFENCE_RADIUS_IN_METERS
                    )

                            // Set the expiration duration of the geofence. This geofence gets automatically
                            // removed after this period of time.
                    .setExpirationDuration(Constants.GEOFENCE_EXPIRATION_IN_MILLISECONDS)

                            // Set the transition types of interest. Alerts are only generated for these
                            // transition. We track entry and exit transitions in this sample.
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_EXIT)

                            // Create the geofence.
                    .build());
        }
    }

    /**
     * Ensures that only one button is enabled at any time. The Add Geofences button is enabled
     * if the user hasn't yet added geofences. The Remove Geofences button is enabled if the
     * user has added geofences.
     */
    // TODO: Dynamically add geofence and remove through time expiration
    private void setButtonsEnabledState() {
        if (mGeofencesAdded) {
//            mAddGeofencesButton.setEnabled(false);
//            mRemoveGeofencesButton.setEnabled(true);
        } else {
//            mAddGeofencesButton.setEnabled(true);
//            mRemoveGeofencesButton.setEnabled(false);
        }
    }







    private void  setMatchList() {

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Posts");
        query.whereEqualTo("user", ParseUser.getCurrentUser());
        query.orderByDescending("createdAt");
        query.findInBackground(new FindCallback<ParseObject>() {
            public void done(List list, ParseException e) {
                if (e == null) {

                    ArrayList<MubiqPost> post = (ArrayList<MubiqPost>) list;

                    final MatchAdapter adapter = new MatchAdapter(SendMessageToWear.this, post);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            matchList.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }
                    });

                } else {
                    Log.e("Posts from Current User", "query done error " + e);
                }
            }
        });
    }

}