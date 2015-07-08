/* Gracenote Android Music SDK Sample Application
 *
 * Copyright (C) 2010 Gracenote, Inc. All Rights Reserved.
 */

package com.android.zigzag.mubi.gracenote;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.support.wearable.activity.ConfirmationActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.android.zigzag.mubi.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.gracenote.gnsdk.GnAlbum;
import com.gracenote.gnsdk.GnAlbumIterator;
import com.gracenote.gnsdk.GnAudioFile;
import com.gracenote.gnsdk.GnDescriptor;
import com.gracenote.gnsdk.GnError;
import com.gracenote.gnsdk.GnException;
import com.gracenote.gnsdk.GnFingerprintType;
import com.gracenote.gnsdk.GnImageSize;
import com.gracenote.gnsdk.GnLanguage;
import com.gracenote.gnsdk.GnLicenseInputMode;
import com.gracenote.gnsdk.GnList;
import com.gracenote.gnsdk.GnLocale;
import com.gracenote.gnsdk.GnLocaleGroup;
import com.gracenote.gnsdk.GnLog;
import com.gracenote.gnsdk.GnLogColumns;
import com.gracenote.gnsdk.GnLogFilters;
import com.gracenote.gnsdk.GnLogPackageType;
import com.gracenote.gnsdk.GnLookupData;
import com.gracenote.gnsdk.GnLookupLocalStream;
import com.gracenote.gnsdk.GnLookupLocalStreamIngest;
import com.gracenote.gnsdk.GnLookupLocalStreamIngestStatus;
import com.gracenote.gnsdk.GnLookupMode;
import com.gracenote.gnsdk.GnManager;
import com.gracenote.gnsdk.GnMic;
import com.gracenote.gnsdk.GnMusicId;
import com.gracenote.gnsdk.GnMusicIdFile;
import com.gracenote.gnsdk.GnMusicIdFileCallbackStatus;
import com.gracenote.gnsdk.GnMusicIdFileInfo;
import com.gracenote.gnsdk.GnMusicIdFileResponseType;
import com.gracenote.gnsdk.GnMusicIdStream;
import com.gracenote.gnsdk.GnMusicIdStreamIdentifyingStatus;
import com.gracenote.gnsdk.GnMusicIdStreamPreset;
import com.gracenote.gnsdk.GnMusicIdStreamProcessingStatus;
import com.gracenote.gnsdk.GnRegion;
import com.gracenote.gnsdk.GnResponseAlbums;
import com.gracenote.gnsdk.GnResponseDataMatches;
import com.gracenote.gnsdk.GnStatus;
import com.gracenote.gnsdk.GnStorageSqlite;
import com.gracenote.gnsdk.GnUser;
import com.gracenote.gnsdk.GnUserStore;
import com.gracenote.gnsdk.IGnAudioSource;
import com.gracenote.gnsdk.IGnCancellable;
import com.gracenote.gnsdk.IGnLookupLocalStreamIngestEvents;
import com.gracenote.gnsdk.IGnMusicIdFileEvents;
import com.gracenote.gnsdk.IGnMusicIdStreamEvents;
import com.gracenote.gnsdk.IGnStatusEvents;
import com.gracenote.gnsdk.IGnSystemEvents;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


/**
 * <p>
 * This reference application introduces the basic coding techniques for
 * accessing Gracenote's file and stream recognition technology, and metadata and 
 * content (such as Cover Art).
 * </p>
 * <p>
 * Recognize all audio in MediaStore by pressing "LibraryID" button, utilizing file
 * recognition technology.
 * Recognize audio from the device's microphone by pressing the "IDNow" button,
 * utilizing stream recognition technology.
 * </p>
 * <p>
 * Once a file or a stream has been recognized its metadata and Cover Art is displayed 
 * in the main results pane. Note to save memory only the first 10 results display Cover
 * Art. Additional detail can be viewed by pressing on a result.
 * </p>
 */
public class GracenoteMusicID extends Activity {

	// set these values before running the sample
	static final String gnsdkClientId 			= "9148416";
	static final String gnsdkClientTag 			= "EA1C43BD1FFE51ED7ECF272A2F04DA45";
	static final String gnsdkLicenseFilename 	= "license.txt";	// app expects this file as an "asset"
	private static final String gnsdkLogFilename 		= "sample.log";
	private static final String appString				= "GFM Sample";
    private static final String TAG = appString;
	
	private Activity activity;
	private Context context;
	
	// ui objects
	private TextView statusText;
	private LinearLayout linearLayoutVisContainer;
	private boolean						visShowing;

	protected ViewGroup metadataListing;
	private final int 	metadataMaxNumImages 	= 10;

	// Gracenote objects
	private GnManager 					gnManager;
	private GnUser 						gnUser;
	private GnMusicId        			gnMusicIdStream;
	private IGnAudioSource				gnMicrophone;
	
	// store some tracking info about the most recent MusicID-Stream lookup
	protected volatile boolean 			lastLookup_local		 = false;	// indicates whether the match came from local storage
	protected volatile long				lastLookup_matchTime 	 = 0;  		// total lookup time for query
	protected volatile long				lastLookup_startTime;  				// start time of query
	private volatile boolean			audioProcessingStarted   = false;
	private volatile boolean			analyzingCollection 	 = false;
	private volatile boolean			analyzeCancelled 	 	 = false;

    private GoogleApiClient                 mGoogleApiClient;
    private NodeApi.NodeListener            nodeListener;
    private String                          remoteNodeId = "";
    private MessageApi.MessageListener      messageListener;
    private final String                    MESSAGE_PATH                   = "/message";
    private Handler                         handler;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;
		context  = this.getApplicationContext();
        handler = new Handler();

		// check the client id and tag have been set
		if ( (gnsdkClientId == null) || (gnsdkClientTag == null) ){
			showError( "Please set Client ID and Client Tag" );
			return;
		}
		
		// get the gnsdk license from the application assets
		String gnsdkLicense = null;
		if ( (gnsdkLicenseFilename == null) || (gnsdkLicenseFilename.length() == 0) ){
			showError( "License filename not set" );
		} else {
			gnsdkLicense = getAssetAsString( gnsdkLicenseFilename );
			if ( gnsdkLicense == null ){
				showError( "License file not found: " + gnsdkLicenseFilename );
				return;
			}
		}
		
		try {
			
			// GnManager must be created first, it initializes GNSDK
			gnManager = new GnManager( context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString );
			
			// provide handler to receive system events, such as locale update needed
			gnManager.systemEventHandler( new SystemEvents() );
			
			// get a user, if no user stored persistently a new user is registered and stored 
			// Note: Android persistent storage used, so no GNSDK storage provider needed to store a user
			gnUser = new GnUser( new GnUserStore(context), gnsdkClientId, gnsdkClientTag, appString );

			// enable storage provider allowing GNSDK to use its persistent stores
			GnStorageSqlite.enable();
			
			// enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
			//GnLookupLocalStream.enable();
			
			// Loads data to support the requested locale, data is downloaded from Gracenote Service if not
			// found in persistent storage. Once downloaded it is stored in persistent storage (if storage
			// provider is enabled). Download and write to persistent storage can be lengthy so perform in 
			// another thread
			Thread localeThread = new Thread(
									new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
										GnLanguage.kLanguageEnglish, 
										GnRegion.kRegionGlobal,
										GnDescriptor.kDescriptorDefault,
										gnUser) 
									);
			localeThread.start();	
			
			// Ingest MusicID-Stream local bundle, perform in another thread as it can be lengthy
			Thread ingestThread = new Thread( new LocalBundleIngestRunnable(context) );
			ingestThread.start();									
			
			// Set up for continuous listening from the microphone
			// - create microphone, this can live for lifetime of app
			// - create GnMusicIdStream instance, this can live for lifetime of app
			// - configure
			// Starting and stopping continuous listening should be started and stopped
			// based on Activity life-cycle, see onPause and onResume for details
			// To show audio visualization we wrap GnMic in a visualization adapter
			gnMicrophone = new GnMic();
			gnMusicIdStream = new GnMusicId(gnUser,new MusicIDEvents());

			gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataContent, true);
			gnMusicIdStream.options().lookupData(GnLookupData.kLookupDataSonicData, true);
			gnMusicIdStream.options().resultSingle( true );
			
			// Retain GnMusicIdStream object so we can cancel an active identification if requested
		//	streamIdObjects.add( gnMusicIdStream );
			
		} catch ( GnException e ) {
			
			Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			showError( e.errorAPI() + ": " + e.errorDescription() );
			return;
			
		} catch ( Exception e ) {
			if(e.getMessage() != null){
				Log.e(appString, e.getMessage());
				showError( e.getMessage() );
			}
			else{
				e.printStackTrace();
			}
			return;
			
		}

        // Create NodeListener that enables buttons when a node is connected and disables buttons when a node is disconnected
        nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                remoteNodeId = node.getId();
            }

            @Override
            public void onPeerDisconnected(Node node) {
                remoteNodeId= "";
            }
        };
        // Create MessageListener that receives messages sent from mobile
        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getPath().equals(MESSAGE_PATH)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            Long tsLong = System.currentTimeMillis() / 1000;
                            String ts = tsLong.toString();

                            Log.v(TAG, "\n" + getString(R.string.received_message) + " " + ts);

                        }
                    });
                }
            }
        };
        // Create GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Wearable.NodeApi.addListener(mGoogleApiClient, nodeListener);
                Wearable.MessageApi.addListener(mGoogleApiClient, messageListener);
                // If there is a connected node, get it's id that is used when sending messages
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                            List<Node> foundNodes = getConnectedNodesResult.getNodes();
                            Log.d(TAG, "foundNodes: " + getConnectedNodesResult.getNodes());
                            for (int i=0; i<foundNodes.size(); i++) {
                                if( foundNodes.get(i).getId() != "cloud" && foundNodes.get(i).isNearby() == true ) {
                                    Log.d(TAG, "Node: " + foundNodes.get(i));
                                    remoteNodeId = foundNodes.get(i).getId();
                                    Log.d(TAG, "Id: " + remoteNodeId);
                                }
                            }
                        }
                    }
                });
            }

            @Override
            public void onConnectionSuspended(int i) {
                remoteNodeId = "";
            }
        }).addApi(Wearable.API).build();

        final Handler handler = new Handler();
        Timer timer = new Timer();
        TimerTask doAsynchronousTask = new TimerTask() {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @SuppressWarnings("unchecked")
                    public void run() {
                        try {
                            identifyIntervall();
                        }
                        catch (Exception e) {
                            Log.d(appString, "identifyIntervall not called" + e.getMessage());
                        }
                    }
                });
            }
        };
        timer.schedule(doAsynchronousTask, 0, 10000);
	}



    @Override
	protected void onResume() {
		super.onResume();
		
		if ( gnMusicIdStream != null ) {
			
			// restart itnerval
			
		}
		
		// tmp - work around temporary behavior where
		// calling audioProcessStop stops all events, including
		// cancelled notification, from a pending identification
		if ( gnManager != null ) {
		}

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

        Wearable.NodeApi.removeListener(mGoogleApiClient, nodeListener);
        Wearable.MessageApi.removeListener(mGoogleApiClient, messageListener);
        mGoogleApiClient.disconnect();

		super.onPause();
		
		if ( gnMusicIdStream != null ) {

		}
	}

    private class MusicIDEvents implements IGnStatusEvents {
        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {
            Log.v(appString, "MusicIDEvents: " + gnStatus.toString());
        }
    }

    /**
     * Record every 30 sec and try to find a match
     */
    public void identifyIntervall() {

        try {

            gnMusicIdStream.fingerprintFromSource(gnMicrophone, GnFingerprintType.kFingerprintTypeStream6);
            String fingerprint = gnMusicIdStream.fingerprintDataGet();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, remoteNodeId, MESSAGE_PATH, fingerprint.getBytes(Charset.forName("UTF-8"))).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    Log.d(TAG, "sendMessageResult " + sendMessageResult);
                    Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                    if (sendMessageResult.getStatus().isSuccess()) {
                        Log.d(TAG, "sendMessageResult status (success): " + sendMessageResult.getStatus().isSuccess());
//                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
//                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.message_sent));
                    } else {
                        Log.d(TAG, "sendMessageResult status (failure): " + sendMessageResult.getStatus().isSuccess());
//                        intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
//                        intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.error_message));
                    }
//                        startActivity(intent);
                }
            });
            Log.v(appString, "result" + fingerprint);
            lastLookup_startTime = SystemClock.elapsedRealtime();

        } catch (GnException e) {

            Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
            showError( e.errorAPI() + ": " +  e.errorDescription() );

        }

    }


	/**
	 * Loads a locale
	 */
	class LocaleLoadRunnable implements Runnable {
		GnLocaleGroup	group;
		GnLanguage		language;
		GnRegion		region;
		GnDescriptor	descriptor;
		GnUser			user;


		LocaleLoadRunnable(
				GnLocaleGroup group,
				GnLanguage		language,
				GnRegion		region,
				GnDescriptor	descriptor,
				GnUser			user) {
			this.group 		= group;
			this.language 	= language;
			this.region 	= region;
			this.descriptor = descriptor;
			this.user 		= user;
		}

		@Override
		public void run() {
			try {

				GnLocale locale = new GnLocale(group,language,region,descriptor,gnUser);
				locale.setGroupDefault();

			} catch (GnException e) {
				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
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

				// our bundle is delivered as a package asset
				// to ingest the bundle access it as a stream and write the bytes to
				// the bundle ingester
				// bundles should not be delivered with the package as this, rather they
				// should be downloaded from your own online service

				InputStream bundleInputStream 	= null;
				int				ingestBufferSize	= 1024;
				byte[] 			ingestBuffer 		= new byte[ingestBufferSize];
				int				bytesRead			= 0;

				GnLookupLocalStreamIngest ingester = new GnLookupLocalStreamIngest(new BundleIngestEvents());

				try {

					bundleInputStream = context.getAssets().open("1557.b");

					do {

						bytesRead = bundleInputStream.read(ingestBuffer, 0, ingestBufferSize);
						if ( bytesRead == -1 )
							bytesRead = 0;

						ingester.write( ingestBuffer, bytesRead );

					} while( bytesRead != 0 );

				} catch (IOException e) {
					e.printStackTrace();
				}

				ingester.flush();

			} catch (GnException e) {
				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			}

		}
	}


	/**
	 * Receives system events from GNSDK
	 */
	class SystemEvents implements IGnSystemEvents {
		@Override
		public void localeUpdateNeeded( GnLocale locale ){

			// Locale update is detected
			try {
				locale.update( gnUser );
			} catch (GnException e) {
				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			}
		}

		@Override
		public void listUpdateNeeded( GnList list ) {
			// List update is detected
			try {
				list.update( gnUser );
			} catch (GnException e) {
				Log.e(appString, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
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
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {

		}

	};

	/**
	 * GNSDK bundle ingest status event delegate
	 */
	private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{

		@Override
		public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {

		}
	}


	/**
	 * Helpers to read license file from assets as string
	 */
	private String getAssetAsString( String assetName ){

		String assetString = null;
		InputStream assetStream;

		try {

			assetStream = this.getApplicationContext().getAssets().open(assetName);
			if(assetStream != null){

				java.util.Scanner s = new java.util.Scanner(assetStream).useDelimiter("\\A");

				assetString = s.hasNext() ? s.next() : "";
				assetStream.close();

			}else{
				Log.e(appString, "Asset not found:" + assetName);
			}

		} catch (IOException e) {

			Log.e(appString, "Error getting asset as string: " + e.getMessage());

		}

		return assetString;
	}


	/**
	 * Helper to show and error
	 */
	private void showError( String errorMessage ) {

	}

	
	
}
