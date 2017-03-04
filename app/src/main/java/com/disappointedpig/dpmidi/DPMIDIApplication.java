package com.disappointedpig.dpmidi;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;


public class DPMIDIApplication extends Application {
    private static Context context;

//    private boolean runInBackground;

    public void onCreate() {
        super.onCreate();

        DPMIDIForeground.init(this);

        DPMIDIApplication.context = getApplicationContext();

//        runInBackground = false;
    }

    public void setRunInBackground(boolean b) {
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);
        sharedpreferences.edit().putBoolean(Constants.PREF.BACKGROUND_STATE_PREF,b).commit();
    }

    public boolean getRunInBackground() {
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);
        return sharedpreferences.getBoolean(Constants.PREF.BACKGROUND_STATE_PREF,false);
    }
    public static Context getAppContext() {
        return DPMIDIApplication.context;
    }

}
