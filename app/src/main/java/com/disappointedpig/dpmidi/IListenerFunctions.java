package com.disappointedpig.dpmidi;

public interface IListenerFunctions {

//    void setLocation(double lat, double lon);
    void midiStateChanged(ConnectionState state);

    void cmsStarted();

}