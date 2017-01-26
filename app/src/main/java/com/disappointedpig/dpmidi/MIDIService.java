package com.disappointedpig.dpmidi;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;


public class MIDIService extends Service implements DPMIDIForeground.Listener {

    private static final String TAG = "MIDIService";

    int mStartMode;
    private Boolean serviceIsRunning;
//    WifiManager.WifiLock wifiLock;
    boolean mAllowRebind = true;

    private WifiManager.WifiLock wifiLock = ((WifiManager) DPMIDIApplication.getAppContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "stagecallerWIFILock");
    private static PowerManager.WakeLock wakeLock= ((PowerManager) DPMIDIApplication.getAppContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stagecallerWakeLock");

//    SharedPreferences sharedpreferences;
//    SharedPreferences.OnSharedPreferenceChangeListener preferencelistener;

//    private final ConnectionManager cm = ConnectionManager.GetInstance();
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
        if (intent.getAction().equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(TAG, "Received MIDIService Intent ");
            Intent notificationIntent = new Intent(this, MainActivity.class);
            notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                    notificationIntent, 0);

//            Intent previousIntent = new Intent(this, MIDIService.class);
//            previousIntent.setAction(Constants.ACTION.PREV_ACTION);
//            PendingIntent ppreviousIntent = PendingIntent.getService(this, 0,
//                    previousIntent, 0);

            Intent playIntent = new Intent(this, MIDIService.class);
            playIntent.setAction(Constants.ACTION.START_ACTION);
            PendingIntent pplayIntent = PendingIntent.getService(this, 0,
                    playIntent, 0);

            Intent nextIntent = new Intent(this, MIDIService.class);
            nextIntent.setAction(Constants.ACTION.STOP_ACTION);
            PendingIntent pnextIntent = PendingIntent.getService(this, 0,
                    nextIntent, 0);
            Bitmap icon = BitmapFactory.decodeResource(getResources(),
                    R.mipmap.ic_launcher);
            Notification notification = new NotificationCompat.Builder(this)
                    .setContentTitle("StageCaller")
                    .setTicker("StageCaller")
                    .setContentText("stagecaller")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setLargeIcon(
                            Bitmap.createScaledBitmap(icon, 128, 128, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .addAction(android.R.drawable.ic_media_play, "Start",
                            pplayIntent)
                    .addAction(android.R.drawable.ic_media_next, "Stop",
                            pnextIntent).build();
            startForeground(Constants.NOTIFICATION_ID.MIDI_SERVICE,
                    notification);

        } else if (intent.getAction().equals(Constants.ACTION.PREV_ACTION)) {
            Log.i(TAG, "Clicked Previous");
        } else if (intent.getAction().equals(Constants.ACTION.START_ACTION)) {
            Log.i(TAG, "Clicked Play - start midi");
            wifiLock.acquire();
            wakeLock.acquire();
            ConnectionManager.GetInstance().startMIDI();
        } else if (intent.getAction().equals(Constants.ACTION.STOP_ACTION)) {
            Log.i(TAG, "Clicked Next - stop midi");
            wifiLock.release();
            wakeLock.release();
            ConnectionManager.GetInstance().stopMIDI();

        } else if (intent.getAction().equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
    return START_STICKY;

//        mStartMode = START_STICKY;
//        if(serviceIsRunning)
//            Toast.makeText(this, "Still Running", Toast.LENGTH_LONG).show();
//        else {
//            Toast.makeText(this, "Started", Toast.LENGTH_LONG).show();
//            DPMIDIForeground.get().addListener(this);
//        }
//        serviceIsRunning = true;

//        sharedpreferences = getSharedPreferences("DPMIDIPreferences", Context.MODE_PRIVATE);
//        preferencelistener = new SharedPreferences.OnSharedPreferenceChangeListener() {

//            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
//                int flag = 1;
//                if(key.equals("MIDIState")) {
//                    if(prefs.getBoolean("MIDIState",false)) {
//                        ConnectionManager.GetInstance().startMIDI();
//                    } else {
//                        ConnectionManager.GetInstance().stopMIDI();
//                    }
//                }
//            }
//        };
//        sharedpreferences.registerOnSharedPreferenceChangeListener(preferencelistener);

//        wifiLock = ((WifiManager) getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock");

//        return mStartMode;
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
