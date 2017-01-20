package com.disappointedpig.dpmidi;


// connection manager singleton

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import com.disappointedpig.midi.MIDISession;

public class ConnectionManager {

    private static ConnectionManager instance;

    private static final String TAG = "ConnectionManager";
    private static final int CM_DEBUG_LEVEL = 0;
    private static final String DEFAULT_BONJOUR_NAME = "testing";

    private boolean midiRunning = false, oscRunning = false;
    private HeartbeatManager hbm;

    public enum ConnectionState {
        NOT_RUNNING,
        STARTING,
        STOPPING,
        FAILED,
        RUNNING
    }

    public enum ConnectionService {
        MIDI,
        OSC
    }

    private ConnectionState MIDIState, OSCState;
    private WifiManager.WifiLock wifiLock;

    static public ConnectionManager GetInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public ConnectionManager() {
        MIDIState = ConnectionState.NOT_RUNNING;
        OSCState = ConnectionState.NOT_RUNNING;
        wifiLock = ((WifiManager) DPMIDIApplication.getAppContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "dpmidiWIFILock");
    }

    public ConnectionState getMIDIState() {
        return MIDIState;
    }

    public ConnectionState getOSCState() {
        return OSCState;
    }

    private void setMIDIState(ConnectionState state) {
        MIDIState = state;
    }

    private void setOSCState(ConnectionState state) {
        OSCState = state;
    }

    private void checkWifiLock() {
        if(wifiLock.isHeld() && !midiRunning && !oscRunning) {
            wifiLock.release();
        }
        if((midiRunning || oscRunning) && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    public void startMIDI() {

        setMIDIState(ConnectionState.STARTING);

        MIDISession midi = MIDISession.getInstance();
        if(midi != null) {
            midi.init(DPMIDIApplication.getAppContext());
            midi.setBonjourName(DEFAULT_BONJOUR_NAME);
            midi.start();
            midiRunning = true;
            setMIDIState(ConnectionState.RUNNING);
        } else {
            midiRunning = false;
            setMIDIState(ConnectionState.FAILED);
        }
        checkWifiLock();
    }

    public void stopMIDI() {
        MIDISession midi = MIDISession.getInstance();
        midiRunning = false;
        setMIDIState(ConnectionState.NOT_RUNNING);
        if(hbm != null) {
            hbm.stopHeartbeat();
        }

        if(midi != null) {
            midi.stop();
        } else {
            midiRunning = false;
            setMIDIState(ConnectionState.FAILED);
        }
        checkWifiLock();
    }

    public void testHeartbeat() {
        hbm = new HeartbeatManager();
        Bundle t = new Bundle();
        t.putInt("type",0);
        t.putInt("note",2);
        t.putInt("velocity",127);
        t.putInt("channel",0);
        t.putInt("channel_status",9);
        hbm.setHeartbeat(t);
        hbm.setDelay(5);
        hbm.startHeartbeat();
    }


}

