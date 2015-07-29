package com.cyclopsdev.parkingcompass.activities;

import android.app.ActionBar;
import android.app.Dialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteConstraintException;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.cyclopsdev.parkingcompass.R;
import com.cyclopsdev.parkingcompass.fragments.CompassFragment;
import com.cyclopsdev.parkingcompass.fragments.FragmentMap;
import com.cyclopsdev.parkingcompass.fragments.HistoryFragment;
import com.cyclopsdev.parkingcompass.support.Utils;
import com.cyclopsdev.parkingcompass.support.ZoomOutPageTransformer;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.ParseAnalytics;

import java.util.Locale;

public class MainActivity extends FragmentActivity implements GoogleApiClient.ConnectionCallbacks,
		GoogleApiClient.OnConnectionFailedListener, ActionBar.TabListener,
		LocationListener, SensorEventListener, FragmentMap.OnCarLocationSavedToParseListener {

	// Request code to use when launching the resolution activity
	private static final int REQUEST_RESOLVE_ERROR = 1001;
	// Unique tag for the error dialog fragment
	private static final String DIALOG_ERROR = "dialog_error";
	// Bool to track whether the app is already resolving an error
	private boolean mResolvingError = false;

	private FragmentManager fragmentManager;

	private SectionsPagerAdapter mSectionsPagerAdapter;

	private Menu mOtionsMenu;

	public GoogleApiClient mGoogleApiClient;
	public Location userLocation;
	LocationRequest mLocationRequest;

	private SensorManager mSensorManager;
	private Sensor accelerometer;
	private Sensor magnetometer;

	/**
	 * The {@link ViewPager} that will host the section contents.
	 */
	private ViewPager mViewPager;
	private static boolean mSoundActive = false;
	private boolean mThreadFlag = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Set portrait orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

		// Set up the action bar.
		final ActionBar actionBar = getActionBar();
		if (actionBar != null) {
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			// actionBar.setDisplayShowHomeEnabled(false);
			// actionBar.setDisplayShowTitleEnabled(false);
			// actionBar.setDisplayUseLogoEnabled(false);
		}

		fragmentManager = getSupportFragmentManager();

		// Create the adapter that will return a fragment for each of the three
		// primary sections of the app.
		mSectionsPagerAdapter = new SectionsPagerAdapter(fragmentManager);

		// Set up the ViewPager with the sections adapter.
		mViewPager = (ViewPager) findViewById(R.id.pager);
		mViewPager.setAdapter(mSectionsPagerAdapter);

		mViewPager.setBackgroundColor(Color.WHITE);
		mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());

		// Prevent fragments from being removed from the FragmentManager when they go offscreen
		mViewPager.setOffscreenPageLimit(2);

		// When swiping between different sections, select the corresponding
		// tab. We can also use ActionBar.Tab#select() to do this if we have
		// a reference to the Tab.
		mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				if (actionBar != null) {
					actionBar.setSelectedNavigationItem(position);
				}
			}
		});

		// For each of the sections in the app, add a tab to the action bar.
		for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
			// Create a tab with text corresponding to the page title defined by
			// the adapter. Also specify this Activity object, which implements
			// the TabListener interface, as the callback (listener) for when
			// this tab is selected.
			if (actionBar != null) {
				actionBar.addTab(actionBar.newTab()
						.setText(mSectionsPagerAdapter.getPageTitle(i))
						.setTabListener(this));
			}
		}

		mViewPager.setCurrentItem(1);

		createSensorManager();
		createLocationRequest();
		buildGoogleApiClient();

		ParseAnalytics.trackAppOpenedInBackground(getIntent());

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mResolvingError) {
			mGoogleApiClient.connect();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		if (mGoogleApiClient.isConnected()) {
			mGoogleApiClient.disconnect();
		}
		mSoundActive = false;
	}

	@Override
	protected void onResume() {
		super.onResume();

		int SENSOR_DELAY = SensorManager.SENSOR_DELAY_FASTEST;

		if (isNetworkAvailable(this)) {
			checkPlayServices();
		}

		if (mGoogleApiClient.isConnected()) {
			startLocationUpdates();
		}

		mSensorManager.registerListener(this, accelerometer, SENSOR_DELAY);
		mSensorManager.registerListener(this, magnetometer, SENSOR_DELAY);

	}

	protected void createSensorManager() {
		mSensorManager = (SensorManager) this.getSystemService(
				MainActivity.SENSOR_SERVICE);

		accelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		magnetometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
	}

	protected void createLocationRequest() {
		mLocationRequest = new LocationRequest();
		mLocationRequest.setInterval(5000);
		mLocationRequest.setFastestInterval(2000);
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	}

	protected synchronized void buildGoogleApiClient() {
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addConnectionCallbacks(this)
				.addOnConnectionFailedListener(this)
				.addApi(LocationServices.API)
				.build();
	}

	protected void startLocationUpdates() {
		LocationServices.FusedLocationApi.requestLocationUpdates(
				mGoogleApiClient, mLocationRequest, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		mOtionsMenu = menu;
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		if(Utils.isAvailable(Utils.loadPreferences(this))){
			mOtionsMenu.findItem(R.id.action_add_car).setVisible(false);
			mOtionsMenu.findItem(R.id.action_delete_car).setVisible(true);
			mOtionsMenu.findItem(R.id.action_volume_on).setVisible(true);
			mOtionsMenu.findItem(R.id.action_volume_off).setVisible(false);

		} else {
			mOtionsMenu.findItem(R.id.action_add_car).setVisible(true);
			mOtionsMenu.findItem(R.id.action_delete_car).setVisible(false);
			mOtionsMenu.findItem(R.id.action_volume_on).setVisible(false);
			mOtionsMenu.findItem(R.id.action_volume_off).setVisible(false);
		}
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
			case (R.id.action_volume_on):

					mSoundActive = true;
				playBeep(this);

					mOtionsMenu.findItem(R.id.action_add_car).setVisible(false);
					mOtionsMenu.findItem(R.id.action_delete_car).setVisible(true);
					mOtionsMenu.findItem(R.id.action_volume_on).setVisible(false);
					mOtionsMenu.findItem(R.id.action_volume_off).setVisible(true);

				return true;
			case (R.id.action_volume_off):

				mSoundActive = false;

				mOtionsMenu.findItem(R.id.action_add_car).setVisible(false);
				mOtionsMenu.findItem(R.id.action_delete_car).setVisible(true);
				mOtionsMenu.findItem(R.id.action_volume_on).setVisible(true);
				mOtionsMenu.findItem(R.id.action_volume_off).setVisible(false);
				return true;
			case (R.id.action_add_car):

				Location newCarLocation = null;

				if (canGetLocation()){
					newCarLocation = getLocation();
				}

				if (newCarLocation != null) {

					Toast toast = Toast.makeText(this, R.string.location_saved,
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.TOP, 0, 150);
					toast.show();
					newCarPosition(newCarLocation);

					mOtionsMenu.findItem(R.id.action_add_car).setVisible(false);
					mOtionsMenu.findItem(R.id.action_delete_car).setVisible(true);
					mOtionsMenu.findItem(R.id.action_volume_on).setVisible(true);
					mOtionsMenu.findItem(R.id.action_volume_off).setVisible(false);

				} else {
					Toast toast = Toast.makeText(this, R.string.location_not_saved,
							Toast.LENGTH_SHORT);
					toast.setGravity(Gravity.TOP, 0, 150);
					toast.show();				}
				return true;
			case (R.id.action_delete_car):

				Toast toast = Toast.makeText(this, R.string.location_removed,
						Toast.LENGTH_SHORT);
				toast.setGravity(Gravity.TOP, 0, 150);
				toast.show();

				newCarPosition(null);

				mOtionsMenu.findItem(R.id.action_add_car).setVisible(true);
				mOtionsMenu.findItem(R.id.action_delete_car).setVisible(false);
				mOtionsMenu.findItem(R.id.action_volume_on).setVisible(false);
				mOtionsMenu.findItem(R.id.action_volume_off).setVisible(false);

				return true;
			default:
				return super.onMenuItemSelected(featureId, item);
		}
	}

	public static boolean isSoundActive() {
		return mSoundActive;
	}

	@Override
	public void onTabSelected(ActionBar.Tab tab,
							  FragmentTransaction fragmentTransaction) {
		// When the given tab is selected, switch to the corresponding page in
		// the ViewPager.
		mViewPager.setCurrentItem(tab.getPosition());


	}

	@Override
	public void onTabUnselected(ActionBar.Tab tab,
								FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onTabReselected(ActionBar.Tab tab,
								FragmentTransaction fragmentTransaction) {
	}

	@Override
	public void onConnected(Bundle bundle) {
		Log.d("GoogleApiClient", "CONNECTED");
		userLocation = LocationServices.FusedLocationApi.getLastLocation(
				mGoogleApiClient);
		startLocationUpdates();
	}

	@Override
	public void onConnectionSuspended(int i) {
		mGoogleApiClient.connect();
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		if (mResolvingError) {
			// Already attempting to resolve an error.
			return;
		} else if (result.hasResolution()) {
			try {
				mResolvingError = true;
				result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
			} catch (IntentSender.SendIntentException e) {
				// There was an error with the resolution intent. Try again.
				mGoogleApiClient.connect();
			}
		} else {
			// Show dialog using GooglePlayServicesUtil.getErrorDialog()
			showErrorDialog(result.getErrorCode());
			mResolvingError = true;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_RESOLVE_ERROR) {
			mResolvingError = false;
			if (resultCode == RESULT_OK) {
				// Make sure the app is not already connected or attempting to connect
				if (!mGoogleApiClient.isConnecting() &&
						!mGoogleApiClient.isConnected()) {
					mGoogleApiClient.connect();
				}
			}
		}
	}

	/* Creates a dialog for an error message */
	private void showErrorDialog(int errorCode) {
		// Create a fragment for the error dialog
		ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
		// Pass the error that should be displayed
		Bundle args = new Bundle();
		args.putInt(DIALOG_ERROR, errorCode);
		dialogFragment.setArguments(args);
		dialogFragment.show(fragmentManager, "errordialog");
	}

	/* Called from ErrorDialogFragment when the dialog is dismissed. */
	public void onDialogDismissed() {
		mResolvingError = false;
	}

	@Override
	public void newLocationSavedToParse() {
		HistoryFragment historyFragment = (HistoryFragment) mSectionsPagerAdapter.findFragment(2);
		historyFragment.historyModified();
	}

	/* A fragment to display an error dialog */
	public static class ErrorDialogFragment extends DialogFragment {
		public ErrorDialogFragment() { }

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			// Get the error code and retrieve the appropriate dialog
			int errorCode = this.getArguments().getInt(DIALOG_ERROR);
			return GooglePlayServicesUtil.getErrorDialog(errorCode,
					this.getActivity(), REQUEST_RESOLVE_ERROR);
		}

		@Override
		public void onDismiss(DialogInterface dialog) {
			((MainActivity)getActivity()).onDialogDismissed();
		}
	}

	@Override
	public void onLocationChanged(Location location) {
		userLocation = location;
		updateUI();
	}

	private void updateUI() {

		CompassFragment compassFragment = (CompassFragment) mSectionsPagerAdapter.findFragment(1);
		compassFragment.updateUserLocation(userLocation);

	}

	/**
	 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
	 * one of the sections/tabs/pages.
	 */
	public class SectionsPagerAdapter extends FragmentPagerAdapter {

		public Fragment findFragment(int position) {
			String name = "android:switcher:" + mViewPager.getId() + ":"
					+ position;
			Fragment fragment = fragmentManager.findFragmentByTag(name);
			if (fragment == null) {
				fragment = getItem(position);
			}
			return fragment;
		}

		public SectionsPagerAdapter(FragmentManager fm) {
			super(fragmentManager);
		}

		@Override
		public Fragment getItem(int position) {

			Fragment fragment;

			if(position == 0){
				fragment = FragmentMap.newInstance(position+1);
			}else if (position == 1){
				fragment = CompassFragment.newInstance(position + 1);
			}else {
				fragment = HistoryFragment.newInstance(position + 1);
			}
			return fragment;
		}

		@Override
		public int getCount() {
			// Show 3 total pages.
			return 3;
		}

		@Override
		public CharSequence getPageTitle(int position) {
			Locale l = Locale.getDefault();
			switch (position) {
				case 0:
					return getString(R.string.title_section1).toUpperCase(l);
				case 1:
					return getString(R.string.title_section2).toUpperCase(l);
				case 2:
					return getString(R.string.title_section3).toUpperCase(l);
			}
			return null;
		}
	}

	private void newCarPosition(Location newCarLocation) {

		Utils.savePreferences(newCarLocation, this);

		mViewPager.setCurrentItem(1);

		FragmentMap fragmentMap = (FragmentMap) mSectionsPagerAdapter.findFragment(0);
		fragmentMap.updateCarLocation(newCarLocation);

		CompassFragment compassFragment = (CompassFragment) mSectionsPagerAdapter.findFragment(1);
		compassFragment.updateCarLocation(newCarLocation);

	}

	public static boolean isNetworkAvailable(Context context) {
		ConnectivityManager CManager =
				(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo NInfo = CManager.getActiveNetworkInfo();
		return (NInfo != null && NInfo.isConnectedOrConnecting());
	}

	private void checkPlayServices() {

		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());

		if (status != ConnectionResult.SUCCESS) {
			int requestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					requestCode);
			dialog.show();
		}
	}

	public Location getLocation() {
		return userLocation;
	}

	public boolean canGetLocation() {
		return userLocation != null;
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLocationUpdates();
		mSensorManager.unregisterListener(this);
	}

	protected void stopLocationUpdates() {
		if (mGoogleApiClient.isConnected()) {
			LocationServices.FusedLocationApi.removeLocationUpdates(
					mGoogleApiClient, this);
		}
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		CompassFragment compassFragment = (CompassFragment) mSectionsPagerAdapter.findFragment(1);
		compassFragment.updateSensorData(event);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	public void playBeep(Context context) {
		if (!mThreadFlag) {

			mThreadFlag = true;

			Thread thread = new Thread() {

				public void run() {

					CompassFragment compassFragment = (CompassFragment) mSectionsPagerAdapter.findFragment(1);
					boolean soundActive = true;

					while (soundActive) {

						try {
							ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM, 100);
							toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 200);
						} catch (SQLiteConstraintException e) {
							e.printStackTrace();
						}
						try {
							Log.w("SPEED", Integer.toString(compassFragment.getSpeed()));
							Thread.sleep(compassFragment.getSpeed());
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						soundActive = isSoundActive();
					}
					mThreadFlag = false;
				}
			};
			thread.setContextClassLoader(context.getClass().getClassLoader());
			thread.start();
		}
	}

}