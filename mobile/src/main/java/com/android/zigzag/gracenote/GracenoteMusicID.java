/* Gracenote Android Music SDK Sample Application
 *
 * Copyright (C) 2010 Gracenote, Inc. All Rights Reserved.
 */

package com.android.zigzag.gracenote;

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
import com.gracenote.gnsdk.*;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;


public class GracenoteMusicID extends Activity {

	static final String 				gnsdkClientId 			= "9148416";
	static final String 				gnsdkClientTag 			= "EA1C43BD1FFE51ED7ECF272A2F04DA45";
	static final String 				gnsdkLicenseFilename 	= "license.txt";
	private static final String 		TAG						= "Mubiq";
	
	private Activity 					activity;
	private Context 					context;

	protected ViewGroup 				metadataListing;
	private final int 					metadataMaxNumImages 	= 10;
//	private ArrayList<mOnClickListener> metadataRow_OnClickListeners;

	private GnManager 					gnManager;
	private GnUser 						gnUser;
	
	protected volatile boolean 			lastLookup_local		 = false;
	protected volatile long				lastLookup_matchTime 	 = 0;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		activity = this;
		context  = this.getApplicationContext();

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
			gnManager.systemEventHandler( new SystemEvents() );
			gnUser = new GnUser( new GnUserStore(context), gnsdkClientId, gnsdkClientTag, TAG );

			GnStorageSqlite.enable();

			GnLookupLocalStream.enable();

			Thread localeThread = new Thread(
									new LocaleLoadRunnable(GnLocaleGroup.kLocaleGroupMusic,
										GnLanguage.kLanguageEnglish, 
										GnRegion.kRegionGlobal,
										GnDescriptor.kDescriptorDefault,
										gnUser) 
									);
			localeThread.start();	

			Thread ingestThread = new Thread( new LocalBundleIngestRunnable(context) );
			ingestThread.start();									

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

	}
	
    @Override
	protected void onResume() {
		super.onResume();
		// tbd
    }
    

	@Override
	protected void onPause() {
		super.onPause();
		// tbd
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
		public void systemMemoryWarning(long l, long l1) {
			// tbd
		}
	}


	/**
	 * GNSDK status event delegate
	 */
	private class StatusEvents implements IGnStatusEvents {

		@Override
		public void statusEvent( GnStatus status, long percentComplete, long bytesTotalSent, long bytesTotalReceived, IGnCancellable cancellable ) {
			// tbd
		}

	};


	/**
	 * GNSDK bundle ingest status event delegate
	 */
	private class BundleIngestEvents implements IGnLookupLocalStreamIngestEvents{

		@Override
		public void statusEvent(GnLookupLocalStreamIngestStatus status, String bundleId, IGnCancellable canceller) {
				Log.v(TAG, "Bundle ingest progress: " + status.toString());
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






































	void loadAndDisplayCoverArt(String coverArtUrl, ImageView imageView ){
		Thread runThread = new Thread( new CoverArtLoaderRunnable( coverArtUrl, imageView ) );
		runThread.start();
	}

	class CoverArtLoaderRunnable implements Runnable {

		String coverArtUrl;
		ImageView imageView;

		CoverArtLoaderRunnable( String coverArtUrl, ImageView imageView){
			this.coverArtUrl = coverArtUrl;
			this.imageView = imageView;
		}

		@Override
		public void run() {

			Drawable coverArt = null;

			if (coverArtUrl != null && !coverArtUrl.isEmpty()) {
				URL url;
				try {
					url = new URL("http://" + coverArtUrl);
					InputStream input = new BufferedInputStream(url.openStream());
					coverArt = Drawable.createFromStream(input, "src");

				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			if (coverArt != null) {
				setCoverArt(coverArt, imageView);
			} else {
				setCoverArt(getResources().getDrawable(R.drawable.no_image),imageView);
			}

		}

	}

	private void setCoverArt( Drawable coverArt, ImageView coverArtImage ){
		activity.runOnUiThread(new SetCoverArtRunnable(coverArt, coverArtImage));
	}

	class SetCoverArtRunnable implements Runnable {

		Drawable coverArt;
		ImageView coverArtImage;

		SetCoverArtRunnable( Drawable locCoverArt, ImageView locCoverArtImage) {
			coverArt = locCoverArt;
			coverArtImage = locCoverArtImage;
		}

		@Override
		public void run() {
			coverArtImage.setImageDrawable(coverArt);
		}
	}


	/**
	 * Adds album results to UI via Runnable interface
	 */
	class UpdateResultsRunnable implements Runnable {

		GnResponseAlbums albumsResult;

		UpdateResultsRunnable(GnResponseAlbums albumsResult) {
			this.albumsResult = albumsResult;
		}

		@Override
		public void run() {
			try {
				if (albumsResult.resultCount() == 0) {

					Log.v(TAG, "albumsResult: No match");

				} else {

					Log.v(TAG, "albumsResult: Match found");
					GnAlbumIterator iter = albumsResult.albums().getIterator();
					while (iter.hasNext()) {
						updateMetaDataFields(iter.next(), true, false);

					}
					trackChanges(albumsResult);

				}
			} catch (GnException e) {
				Log.v(TAG, "albumsResult error: " + e.errorDescription());
				return;
			}

		}
	}

	/**
	 * Adds the provided album as a new row on the application display
	 * @throws GnException
	 */
	private void updateMetaDataFields(final GnAlbum album, boolean displayNoCoverArtAvailable, boolean fromTxtOrLyricSearch) throws GnException {

		// Load metadata layout from resource .xml
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View metadataView = inflater.inflate(R.layout.file_meta_data, null);

		metadataListing.addView(metadataView);

		final ImageView coverArtImage = (ImageView) metadataView.findViewById(R.id.coverArtImage);

		TextView albumText = (TextView) metadataView.findViewById( R.id.albumName );
		TextView trackText = (TextView) metadataView.findViewById( R.id.trackTitle );
		TextView artistText = (TextView) metadataView.findViewById( R.id.artistName );

		// enable pressing row to get track listing
		 metadataView.setClickable(true);
//		 mOnClickListener onClickListener = new mOnClickListener(album, coverArtImage);
//		 if(metadataRow_OnClickListeners.add(onClickListener)){
//			 metadataView.setOnClickListener(onClickListener);
//		 }

		if (album == null) {

			coverArtImage.setVisibility(View.GONE);
			albumText.setVisibility(View.GONE);
			trackText.setVisibility(View.GONE);
			// Use the artist text field to display the error message
			//artistText.setText("Music Not Identified");
		} else {

			// populate the display tow with metadata and cover art

			albumText.setText( album.title().display() );
			String artist = album.trackMatched().artist().name().display();

			//use album artist if track artist not available
			if(artist.isEmpty()){
				artist = album.artist().name().display();
			}
			artistText.setText( artist );

			if ( album.trackMatched() != null ) {
				trackText.setText( album.trackMatched().title().display() );
			} else {
				trackText.setText("");
			}

			// limit the number of images added to display so we don't run out of memory,
			// a real app would page the results
			if ( metadataListing.getChildCount() <= metadataMaxNumImages ){
				String coverArtUrl = album.coverArt().asset(GnImageSize.kImageSizeMedium).url();
				loadAndDisplayCoverArt( coverArtUrl, coverArtImage );
			} else {
				coverArtImage.setVisibility(View.GONE);
			}

		}
	}






















	/**
	 * Helper to show and error
	 */
	private void showError( String errorMessage ) {
		Log.v(TAG, "showError (errorMessage): " + errorMessage);
	}


	/**
	 * History Tracking:
	 * initiate the process to insert values into database.
	 * 
	 * @param albums
	 *            - contains all the information to be inserted into DB,
	 *            except location.
	 */
	private synchronized void trackChanges(GnResponseAlbums albums) {		
		Thread thread = new Thread(new InsertChangesRunnable(albums));
		thread.start();
				
	}
	
	class InsertChangesRunnable implements Runnable {
		GnResponseAlbums row;

		InsertChangesRunnable(GnResponseAlbums row) {
			this.row = row;
		}

		@Override
		public void run() {
			try {
				DatabaseAdapter db = new DatabaseAdapter(GracenoteMusicID.this);
				db.open();
				db.insertChanges(row);
				db.close();
			} catch (GnException e) {
				// ignore
			}
		}
	}
	
	
}
