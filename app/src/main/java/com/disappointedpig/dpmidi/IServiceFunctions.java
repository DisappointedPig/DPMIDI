package com.disappointedpig.dpmidi;

import android.app.Activity;

public interface IServiceFunctions {
    void registerActivity(Activity activity, IListenerFunctions callback);

    void unregisterActivity(Activity activity);

//    Location getLocation();
    ConnectionState getMIDIState();
    boolean cmsIsRunning();

}

