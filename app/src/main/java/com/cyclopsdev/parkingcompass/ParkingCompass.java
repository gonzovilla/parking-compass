package com.cyclopsdev.parkingcompass;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseCrashReporting;

/**
 * Created by gonzovilla89 on 05/04/15.
 */
public class ParkingCompass extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);

        // Enable Crash Reporting
        ParseCrashReporting.enable(this);

        // Setup Parse
        Parse.initialize(this, getResources().getString(R.string.parseApplicationId), getResources().getString(R.string.parseClientKey));

    }
}