package com.disappointedpig.dpmidi;

import android.app.Application;
import android.content.Context;


public class DPMIDIApplication extends Application {
    private static Context context;

    public void onCreate() {
        super.onCreate();

        DPMIDIForeground.init(this);

        DPMIDIApplication.context = getApplicationContext();

    }

    public static Context getAppContext() {
        return DPMIDIApplication.context;
    }

}
