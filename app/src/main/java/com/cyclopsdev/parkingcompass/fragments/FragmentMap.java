package com.cyclopsdev.parkingcompass.fragments;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.cyclopsdev.parkingcompass.R;
import com.cyclopsdev.parkingcompass.activities.MainActivity;
import com.cyclopsdev.parkingcompass.support.Utils;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class FragmentMap extends Fragment implements GoogleMap.OnMyLocationChangeListener {

	public OnCarLocationSavedToParseListener mCallback;

	public interface OnCarLocationSavedToParseListener{
		void newLocationSavedToParse();
	}

	private static final String SECTION_NUMBER = "Section";

	/**
	 * Note that this may be null if the Google Play services APK is not
	 * available.
	 */
	private GoogleMap mMap;

	private Location carLocation;

	private SupportMapFragment fragment;

	private boolean firstLocationReceived = true;

	public static FragmentMap newInstance(int section) {
		FragmentMap fragment = new FragmentMap();
		Bundle args = new Bundle();
		args.putInt(SECTION_NUMBER, section);
		fragment.setArguments(args);
		return fragment;
	}

	public void updateCarLocation(Location newCarLocation) {

		carLocation = newCarLocation;

		dropCarLocationPin(carLocation, true);

	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// This makes sure that the container activity has implemented
		// the callback interface. If not, it throws an exception
		try {
			mCallback = (OnCarLocationSavedToParseListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString()
					+ " must implement OnHeadlineSelectedListener");
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);

		if (container == null) {
			return null;
		}
		View view = inflater.inflate(R.layout.map, container, false);

		setUpMapIfNeeded();

		return view;
	}

	/***** Sets up the map if it is possible to do so *****/
	private void setUpMapIfNeeded() {
		// Do a null check to confirm that we have not already instantiated the
		// map.
		if (mMap == null) {

			FragmentManager childFragmentManager;

			childFragmentManager = getChildFragmentManager();
			fragment = (SupportMapFragment) childFragmentManager.findFragmentById(R.id.map_container);
			if (fragment == null) {
				fragment = SupportMapFragment.newInstance();
				childFragmentManager.beginTransaction().replace(R.id.map_container, fragment).commit();
			}

			if (mMap != null) {
				setUpMap();
			}
		} else {
			setUpMap();
		}
	}

	/**
	 * This is where we can add markers or lines, add listeners or move the
	 * camera.
	 * <p>
	 * This should only be called once and when we are sure that {@link #mMap}
	 * is not null.
	 */
	private void setUpMap() {

		// For showing a move to my location button
		mMap.setMyLocationEnabled(true);
		mMap.setOnMyLocationChangeListener(this);

		carLocation = Utils.loadPreferences(getActivity());
		// For dropping a marker at a point on the Map
		if (Utils.isAvailable(carLocation)) {

			dropCarLocationPin(carLocation, false);

		} else {

			mMap.clear();

		}
	}

	public void dropCarLocationPin (Location carLocation, boolean saveToParse){

		MarkerOptions markerOptions = null;

		if (carLocation != null) {
			markerOptions = new MarkerOptions()
					.position(
							new LatLng(carLocation.getLatitude(), carLocation.getLongitude()))
					.title(getString(R.string.your_car))
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.parked_pin));

			if (MainActivity.isNetworkAvailable(getActivity())) {
				markerOptions.snippet(GeoLocation(carLocation, saveToParse));
			}
		}

		if (mMap != null) {
			mMap.clear();
			if (markerOptions != null) {
				mMap.addMarker(markerOptions);
			}
		} else {
			setUpMapIfNeeded();
		}
	}

	// Geocoder for the pin
	private String GeoLocation(Location location, boolean saveToParse) {

		String address = "";

		Geocoder geocoder;
		List<Address> addresses;
		geocoder = new Geocoder(getActivity(), Locale.getDefault());

		try {
			addresses = geocoder.getFromLocation(location.getLatitude(),
					location.getLongitude(), 1);
			address = addresses.get(0).getAddressLine(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (saveToParse) {
			final ParseObject carLocation = new ParseObject("Location");
			carLocation.put("user", Secure.getString(getActivity().getContentResolver(), Secure.ANDROID_ID));
			carLocation.put("latitude", location.getLatitude());
			carLocation.put("longitude", location.getLongitude());
			if (!address.equals("")) {
				carLocation.put("address", address);
			}
			carLocation.saveEventually(new SaveCallback() {
				public void done(ParseException e) {
					if (e == null) {
						mCallback.newLocationSavedToParse();
					} else {
						Log.e("Error saving", e.toString());
					}
				}
			});
		}

		return address;

	}

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	@Override
	public void onMyLocationChange(Location location) {

		if (firstLocationReceived) {
			mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(),
					location.getLongitude()), 15));
			firstLocationReceived = false;
		}

	}

}