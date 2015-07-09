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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
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
import java.util.concurrent.TimeUnit;

public class GracenoteMusicID extends Activity implements DataApi.DataListener {

	static final String                     gnsdkClientId 			= "9148416";
	static final String                     gnsdkClientTag 			= "EA1C43BD1FFE51ED7ECF272A2F04DA45";
	static final String                     gnsdkLicenseFilename 	= "license.txt";
	//private static final String             TAG				        = "Mubiq";
	
	private Activity                        activity;
	private Context                         context;

    private Timer                           timer;
    protected volatile long				    lastLookup_startTime;

	protected ViewGroup                     metadataListing;

	private GnManager 					    gnManager;
	private GnUser 						    gnUser;
	private GnMusicId        			    gnMusicId;
	private IGnAudioSource				    gnMicrophone;

    private GoogleApiClient                 mGoogleApiClient;
    private NodeApi.NodeListener            nodeListener;
    private String                          remoteNodeId 			= "";
    private MessageApi.MessageListener      messageListener;
    private final String                    MESSAGE_PATH            = "/message";
    private Handler                         handler, handlerTimer;

	ImageView coverImg;
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;
		context  = this.getApplicationContext();
        handler = new Handler();
		handlerTimer = new Handler();

		setContentView(R.layout.main);
		coverImg = (ImageView) findViewById(R.id.coverImg);

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

			gnManager = new GnManager( context, gnsdkLicense, GnLicenseInputMode.kLicenseInputModeString );

			gnManager.systemEventHandler(new SystemEvents());
			
			// get a user, if no user stored persistently a new user is registered and stored
			gnUser = new GnUser( new GnUserStore(context), gnsdkClientId, gnsdkClientTag, TAG );

			// enable storage provider allowing GNSDK to use its persistent stores
			GnStorageSqlite.enable();
			
			// enable local MusicID-Stream recognition (GNSDK storage provider must be enabled as pre-requisite)
			// GnLookupLocalStream.enable();

			// Download and write to persistent storage can be lengthy so perform in another thread
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

			gnMicrophone = new GnMic();
			gnMusicId = new GnMusicId(gnUser,new MusicIDEvents());

            gnMusicId.options().lookupData(GnLookupData.kLookupDataContent, true);
            gnMusicId.options().lookupData(GnLookupData.kLookupDataSonicData, true);
            gnMusicId.options().resultSingle( true);

        } catch ( GnException e ) {
			
			Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			showError( e.errorAPI() + ": " + e.errorDescription() );
			return;
			
		} catch ( Exception e ) {
			if(e.getMessage() != null){
				Log.e(TAG, e.getMessage());
				showError( e.getMessage() );
			}
			else{
				e.printStackTrace();
			}
			return;
			
		}


        nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
				Log.v(TAG, "WEAR onPeerConnected");
                remoteNodeId = node.getId();
            }

            @Override
            public void onPeerDisconnected(Node node) {
				Log.v(TAG, "WEAR onPeerDisconnected");
                //remoteNodeId= "";
            }
        };


        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getPath().equals(MESSAGE_PATH)) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            // receiver for fingerprint result from mobile
                        }
                    });
                }
            }
        };


        mGoogleApiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                Wearable.NodeApi.addListener(mGoogleApiClient, nodeListener);
                Wearable.MessageApi.addListener(mGoogleApiClient, messageListener);
				Wearable.DataApi.addListener(mGoogleApiClient, GracenoteMusicID.this);
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

            }
        }).addApi(Wearable.API).build();


        timer = new Timer();
       // timer.schedule(doAsynchronousTask, 0, 10000);
	}

	TimerTask doAsynchronousTask = new TimerTask() {
		@Override
		public void run() {
			handlerTimer.post(new Runnable() {
				@SuppressWarnings("unchecked")
				public void run() {
					try {
						fingerprinting();
					}
					catch (Exception e) {
						Log.d( TAG, "fingerprint not called: " + e.getMessage());
					}
				}
			});
		}
	};

    @Override
	protected void onResume() {
		super.onResume();

		timer.schedule(doAsynchronousTask, 0, 10000);

        int connectionResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (connectionResult != ConnectionResult.SUCCESS) {
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

		timer.cancel();
        Wearable.NodeApi.removeListener(mGoogleApiClient, nodeListener);
        Wearable.MessageApi.removeListener(mGoogleApiClient, messageListener);
        mGoogleApiClient.disconnect();

		super.onPause();
		
		if ( gnMusicId != null ) {
            timer.cancel();
		}
	}

	private String TAG = "Mubiq";

	private static final String WEARABLE_DATA_PATH = "/albumDetails";


	@Override
	public void onDataChanged(DataEventBuffer dataEvents) {

		Log.v(TAG, "onDataChanged");

		DataMap dataMap;
		for (DataEvent event : dataEvents) {

			// Check the data type
			if (event.getType() == DataEvent.TYPE_CHANGED) {
				// Check the data path
				String path = event.getDataItem().getUri().getPath();
				if (path.equals(WEARABLE_DATA_PATH)) {}
				dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
				Log.v(TAG, "DataMap received on watch: " + dataMap);

				DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
				Asset profileAsset = dataMapItem.getDataMap().getAsset("coverImg");
				final Bitmap bitmap = loadBitmapFromAsset(profileAsset);
				Log.d("asd", bitmap.getByteCount() + "");

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						coverImg.setImageBitmap(bitmap);
					}
				});

			}
		}
	}

	public Bitmap loadBitmapFromAsset(Asset asset) {
		if (asset == null) {
			throw new IllegalArgumentException("Asset must be non-null");
		}
		ConnectionResult result =
				mGoogleApiClient.blockingConnect(1000, TimeUnit.MILLISECONDS);
		if (!result.isSuccess()) {
			return null;
		}
		// convert asset into a file descriptor and block until it's ready
		InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
				mGoogleApiClient, asset).await().getInputStream();
		mGoogleApiClient.disconnect();

		if (assetInputStream == null) {
			Log.w(TAG, "Requested an unknown Asset.");
			return null;
		}
		// decode the stream into a bitmap
		return BitmapFactory.decodeStream(assetInputStream);
	}

    private class MusicIDEvents implements IGnStatusEvents {
        @Override
        public void statusEvent(GnStatus gnStatus, long l, long l1, long l2, IGnCancellable iGnCancellable) {
            Log.v(TAG, "MusicIDEvents: " + gnStatus.toString());
        }
    }

    /**
     * Record audio and make fingerprint to send to phone
     */
    public void fingerprinting() {

        try {
			Log.d(TAG, "fingerprinting to remoteNodeId:" + remoteNodeId);

            gnMusicId.fingerprintFromSource(gnMicrophone, GnFingerprintType.kFingerprintTypeStream6);
            String fingerprint = gnMusicId.fingerprintDataGet();

            Wearable.MessageApi.sendMessage(mGoogleApiClient, remoteNodeId, MESSAGE_PATH, fingerprint.getBytes(Charset.forName("UTF-8"))).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                @Override
                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    if (sendMessageResult.getStatus().isSuccess()) {
                        Log.d(TAG, "sendMessageResult (success): " + sendMessageResult);
                    } else {
                        Log.d(TAG, "sendMessageResult (fail): " + sendMessageResult);
                    }
				}
			});

			Log.v(TAG, "result: " + fingerprint);

        } catch (GnException e) {

            Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
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

				InputStream bundleInputStream 	    = null;
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
				Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
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
				Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			}
		}

		@Override
		public void listUpdateNeeded( GnList list ) {
			// List update is detected
			try {
				list.update( gnUser );
			} catch (GnException e) {
				Log.e(TAG, e.errorCode() + ", " + e.errorDescription() + ", " + e.errorModule());
			}
		}

		@Override
		public void systemMemoryWarning(long currentMemorySize, long warningMemorySize) {
			Log.v(TAG, "memory warning limit (only if configured)");
		}
	}


	/**
	 * GNSDK status event delegate
	 */
	private class StatusEvents implements IGnStatusEvents {

		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {
            Log.v(TAG, "IGnStatusEvents statusEvent (status): " + status);
		}

	};

	/**
	 * GNSDK bundle ingest status event delegate
	 */
	private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{

		@Override
		public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
            Log.v(TAG, "IGnLookupLocalStreamIngestEvents statusEvent (status): " + status);
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
	private void showError( String errorMessage ) {
        Log.v(TAG, "showError (errorMessage): " + errorMessage);
	}


	
}
