package com.disappointedpig.dpmidi;

import android.app.Application;
import android.content.Context;


public class DPMIDIApplication extends Application {
    private static Context context;

    private boolean runInBackground;

    public void onCreate() {
        super.onCreate();

        DPMIDIForeground.init(this);

        DPMIDIApplication.context = getApplicationContext();

        runInBackground = false;
    }

    public void setRunInBackground(boolean b) {
        runInBackground = b;
    }

    public boolean getRunInBackground() {
        return runInBackground;
    }
    public static Context getAppContext() {
        return DPMIDIApplication.context;
    }

}
