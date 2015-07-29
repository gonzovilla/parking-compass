package com.cyclopsdev.parkingcompass.fragments;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.cyclopsdev.parkingcompass.R;
import com.cyclopsdev.parkingcompass.support.Utils;

public class CompassFragment extends Fragment {

	private static final String SECTION_NUMBER = "Section";

	private static final int SPEED_SLOW = 1200;
	private static final int SPEED_NORMAL = 800;
	private static final int SPEED_FAST = 400;

	private float[] mGravity = null;
	private float[] mGeomagnetic = null;

	private static TextView textView;
	private ImageView compass;
	private Location userLocation;
	private Location carLocation;

	private float previousDistance = 0;
	private int speed = SPEED_NORMAL;

	public CompassFragment() {
	}

	public static CompassFragment newInstance(int section) {
		CompassFragment fragment = new CompassFragment();
		Bundle args = new Bundle();
		args.putInt(SECTION_NUMBER, section);
		fragment.setArguments(args);
		return fragment;
	}

	public void updateCarLocation(Location newCarLocation) {

		carLocation = newCarLocation;
		if (!Utils.isAvailable(carLocation)) {
			textView.setText(getString(R.string.no_car));
			compass.setRotation(0);
		}
		moveCompass();
	}

	public void updateUserLocation(Location newUserLocation) {

		userLocation = newUserLocation;

		moveCompass();

	}

	public void updateSensorData (SensorEvent event){

		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			mGravity = event.values;
		}
		if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
			mGeomagnetic = event.values;
		}

		moveCompass();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {

		View rootView = inflater.inflate(R.layout.compass, container, false);

		textView = (TextView) rootView.findViewById(R.id.textView1);
		textView.setText(getString(R.string.move));

		compass = (ImageView) rootView.findViewById(R.id.imageView1);
		compass.setImageResource(R.drawable.car_compass);

		return rootView;
	}

	@Override
	public void onResume() {

		carLocation = Utils.loadPreferences(getActivity());

		if (userLocation != null && !Utils.isAvailable(carLocation)) {
			textView.setText(getString(R.string.no_car));
			compass.setRotation(0);
		}

		super.onResume();
	}


	public void moveCompass() {

		if (userLocation != null && Utils.isAvailable(carLocation) && mGravity != null && mGeomagnetic != null) {

			float distance;
			float bearing;
			float direction;
			float azimuthRad, azimuth;

			float Rg[] = new float[9];
			float I[] = new float[9];
			boolean success = SensorManager.getRotationMatrix(Rg, I, mGravity, mGeomagnetic);
			if (success) {
				float orientation[] = new float[3];
				SensorManager.getOrientation(Rg, orientation);
				azimuthRad = orientation[0]; // orientation contains:
				// azimut, pitch
				// and roll
				mGravity = null;
				mGeomagnetic = null;

				if (azimuthRad <= Math.PI / 2) {
					azimuth = ((azimuthRad * -180) / 3.14159f) + 90;
				} else {
					azimuth = 450 + ((azimuthRad * -180) / 3.14159f);
				}

				bearing = userLocation.bearingTo(carLocation);

				if (bearing < 0){
					bearing = 360 + bearing;
				}

				direction = (azimuth + bearing + 270) % 360;

				distance = userLocation.distanceTo(carLocation);

				if (distance < 4) {

					textView.setText(getString(R.string.here));
					compass.setRotation(0);

				} else {

					textView.setText(getString(R.string.distance) + " "
							+ String.valueOf(Math.round(distance)) + " "
							+ getString(R.string.meters));

					compass.setRotation(direction);

				}

				if (previousDistance == 0 || previousDistance == distance){
					speed = SPEED_NORMAL;
				} else if (previousDistance > distance) {
					speed = SPEED_FAST;
				} else {
					speed = SPEED_SLOW;
				}
				previousDistance = distance;

			}
		}
	}

	public int getSpeed(){
		return speed;
	}
}
