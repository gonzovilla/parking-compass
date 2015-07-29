package com.cyclopsdev.parkingcompass.support;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;
import android.preference.PreferenceManager;

/**
 * Created by gonzovilla89 on 04/04/15.
 */
public class Utils {

    public static boolean isAvailable (Location carLocation){
        return !(carLocation == null || (carLocation.getLatitude() == 0 && carLocation.getLongitude() == 0));
    }

    public static void savePreferences(Location location, Context context) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        if (location != null) {
            editor.putFloat("Latitude", (float) location.getLatitude());
            editor.putFloat("Longitude", (float) location.getLongitude());
        } else {
            editor.putFloat("Latitude", (float) 0);
            editor.putFloat("Longitude", (float) 0);
        }
        editor.apply();
    }

    public static Location loadPreferences(Context context) {
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(context);
        double lat = (double) preferences.getFloat("Latitude", 0);
        double lon = (double) preferences.getFloat("Longitude", 0);
        if (lat == 0 && lon == 0) {
            return null;
        } else {
            Location carLocation = new Location("carLocation");
            carLocation.setLatitude(lat);
            carLocation.setLongitude(lon);
            return carLocation;
        }
    }

}
