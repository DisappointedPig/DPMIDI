package com.disappointedpig.dpmidi;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;


public class MIDIService extends Service implements DPMIDIForeground.Listener {

    final String TAG = "MIDIService";
    int mStartMode;
    private Boolean serviceIsRunning;
    WifiManager.WifiLock wifiLock;
    boolean mAllowRebind = true;

    SharedPreferences sharedpreferences;
    SharedPreferences.OnSharedPreferenceChangeListener preferencelistener;

    private final IBinder mBinder = new MIDIServiceBinder();

//    private MIDISession midiSession;

    public MIDIService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        serviceIsRunning = false;
//        EventBus.getDefault().register(this);
    }

    public int onStartCommand(Intent intent, final int flags, int startId) {
        mStartMode = START_STICKY;
        if(serviceIsRunning)
            Toast.makeText(this, "Still Running", Toast.LENGTH_LONG).show();
        else {
            Toast.makeText(this, "Started", Toast.LENGTH_LONG).show();
            DPMIDIForeground.get().addListener(this);
        }
        serviceIsRunning = true;

        sharedpreferences = getSharedPreferences("DPMIDIPreferences", Context.MODE_PRIVATE);
        preferencelistener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                int flag = 1;
                if(key.equals("MIDIState")) {
                    if(prefs.getBoolean("MIDIState",false)) {
//                        startupMIDI();
                        ConnectionManager.GetInstance().startMIDI();
                    } else {
//                        shutdownMIDI();
                        ConnectionManager.GetInstance().stopMIDI();
                    }
                }
            }
        };
        sharedpreferences.registerOnSharedPreferenceChangeListener(preferencelistener);

        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

        return mStartMode;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return mAllowRebind;
    }

    @Override
    public void onRebind(Intent intent) {
        Toast.makeText(this, "CMS Rebind", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "CMS Destroyed", Toast.LENGTH_LONG).show();
        Log.d(TAG,"destroyed");
        ConnectionManager.GetInstance().stopMIDI();
//        shutdownMIDI();
//        EventBus.getDefault().unregister(this);
        wifiLock.release();
    }

    public class MIDIServiceBinder extends Binder {
        MIDIService getService() {
            return MIDIService.this;
        }
    }



//    public void startupMIDI() {
//        Log.d(TAG,"startupMIDI");
//        MIDISession.getInstance().start(this);
//
////        if (midiSession != null) {
////            midiSession.initMIDI(DPMIDIApplication.getAppContext(), 10);
////            midiSession.startListening();
////        }
//    }

//    public void shutdownMIDI() {
//        Log.d(TAG,"shutdownMIDI");
//        MIDISession.getInstance().stop();
//    }

//    @Subscribe(threadMode = ThreadMode.ASYNC)
//    public void onMIDI2MessageEvent(PayloadMessageEvent e) {
//
//    }
//    @Subscribe
//    public void onMIDIDebugEvent(final MIDIDebugEvent event) {
//        Log.e("MIDIService MIDI DEBUG",event.message);
//    }
//
//    @Subscribe
//    public void onMIDIStartEvent(final MIDIStartEvent event) {
//        Log.e("MIDIStartEvent","note:"+event.message.midi_note+" velocity:"+event.message.midi_velocity);
//    }
//
//    @Subscribe
//    public void onMIDIStopEvent(final MIDIStopEvent event) {
//        Log.e("MIDIStopEvent","note:"+event.message.midi_note+" velocity:"+event.message.midi_velocity);
//    }
//
//    @Subscribe
//    public void onMIDIUnknownEvent(final MIDIUnknownEvent event) {
//        Log.e("MIDIEvent","message:"+event.message.toString());
//    }
//
//    @Subscribe
//    public void onMIDINameChangeEvent(final MIDINameChange event) {
//        Log.e("MIDINameChange","name:"+ event.name);
//
//    }

    public void onBecameForeground() {
        Log.d(TAG, "became foreground");

    }

    public void onBecameBackground() {
        Log.d(TAG,"became background");


    }

}
