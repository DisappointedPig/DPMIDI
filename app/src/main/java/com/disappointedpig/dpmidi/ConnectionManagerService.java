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
import android.os.IBinder;
import android.os.PowerManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import static com.disappointedpig.dpmidi.ConnectionManager.ConnectionState.NOT_RUNNING;

public class ConnectionManagerService extends Service implements DPMIDIForeground.Listener {

    private static final String TAG = "CMS";

    private boolean cmsIsRunning = false;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    public ConnectionManagerService() {
        Log.i(TAG, "init ");
        wifiLock = ((WifiManager) DPMIDIApplication.getAppContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "stagecallerWIFILock");
//        wifiLock = ((WifiManager) DPMIDIApplication.getAppContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "stagecallerWIFILock");
        wakeLock = ((PowerManager) DPMIDIApplication.getAppContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stagecallerWakeLock");
    }

    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate ");
    }
    @Override
    public int onStartCommand(Intent intent, final int flags, int startId) {
        Log.i(TAG, "onStartCommand ");
        processIntentAction(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    private void processIntentAction(Intent intent) {
        Log.i(TAG, "processIntentAction ");

        if(!cmsIsRunning) {
            if (intent.getAction().equals(Constants.ACTION.STARTCMGR_ACTION)) {
                Log.i(TAG, "Received STARTCMGR_ACTION ");
                createNotificationIntent();
                DPMIDIForeground.get().addListener(this);
                cmsIsRunning = true;
                checkMIDI();
                checkLocks();

            }
        } else {
            if(intent.getAction().equals(Constants.ACTION.STOPCMGR_ACTION)) {
                Log.i(TAG, "Received STOPCMGR_ACTION ");
                stopForeground(true);
                cmsIsRunning = false;
            } else if(intent.getAction().equals(Constants.ACTION.START_MIDI_ACTION)) {
                Log.i(TAG, "Received START_MIDI_ACTION ");
                ConnectionManager.GetInstance().startMIDI();
            } else if(intent.getAction().equals(Constants.ACTION.STOP_MIDI_ACTION)) {
                Log.i(TAG, "Received STOP_MIDI_ACTION ");
                ConnectionManager.GetInstance().stopMIDI();
            } else {
                Log.i(TAG, "Received UNKNOWN ACTION");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void createNotificationIntent() {
        Log.i(TAG, "createNotificationIntent ");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Intent startMIDIIntent = new Intent(this, ConnectionManagerService.class);
        startMIDIIntent.setAction(Constants.ACTION.START_MIDI_ACTION);
        PendingIntent pstartMIDIIntent = PendingIntent.getService(this, 0, startMIDIIntent, 0);

        Intent stopMIDIIntent = new Intent(this, ConnectionManagerService.class);
        stopMIDIIntent.setAction(Constants.ACTION.STOP_MIDI_ACTION);
        PendingIntent pstopMIDIIntent = PendingIntent.getService(this, 0, stopMIDIIntent, 0);



        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Notification notification = new android.support.v4.app.NotificationCompat.Builder(this)
                .setContentTitle("StageCaller")
                .setTicker("StageCaller")
                .setContentText("stagecaller")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "Start MIDI", pstartMIDIIntent)
                .addAction(android.R.drawable.ic_media_pause, "Stop MIDI", pstopMIDIIntent)
                .build();

        startForeground(Constants.NOTIFICATION_ID.CONNECTIONMGR, notification);

    }

    // check if midi should be on
    private void checkMIDI() {
        // check if midi should be on
        ConnectionManager instance = ConnectionManager.GetInstance();
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);
        if(sharedpreferences != null) {
            Boolean midiPref = sharedpreferences.getBoolean(Constants.PREF.MIDI_STATE_PREF, false);
            if (midiPref && instance.getMIDIState() == NOT_RUNNING) {
                instance.startMIDI();
            } else if(!midiPref && instance.getMIDIState() == ConnectionManager.ConnectionState.RUNNING) {
                instance.stopMIDI();
            }
        }
    }

    private void checkLocks() {
        Log.d(TAG,"start checkLocks wifi:"+(wifiLock.isHeld() ? "YES" : "NO") +" wake:"+(wakeLock.isHeld() ? "YES" : "NO"));
        if(!runInBackground()) {
            if(wifiLock.isHeld()) {
                wifiLock.release();
            }
            if(wakeLock.isHeld()) {
                wakeLock.release();
            }
        } else {
            if(!wifiLock.isHeld()) {
                wifiLock.acquire();
            }
            if(!wakeLock.isHeld()) {
                wakeLock.acquire();
            }
        }

        Log.d(TAG,"end checkLocks wifi:"+(wifiLock.isHeld() ? "YES" : "NO") +" wake:"+(wakeLock.isHeld() ? "YES" : "NO"));
    }

    public void onBecameForeground() {
        Log.d(TAG,"became foreground");
        checkMIDI();
    }

    public void onBecameBackground() {
        Log.d(TAG,"became background");

        ConnectionManager instance = ConnectionManager.GetInstance();

        checkLocks();
        // check if background is set

        if(!runInBackground()) {
            // turn off midi and osc as appropriate
            switch (instance.getMIDIState()) {
                case RUNNING:
                case STARTING:
                    Log.e(TAG,"stopping midi");
                    instance.stopMIDI();
                case NOT_RUNNING:
                case FAILED:
                default:
                    break;
            }

        }
    }

    private boolean runInBackground() {
//        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences(Constants.PREF.SHAREDPREFERENCES_KEY, Context.MODE_PRIVATE);
//        if(sharedpreferences == null) {
//            return false;
//        }
//        boolean bg = sharedpreferences.getBoolean(Constants.PREF.BACKGROUND_STATE_PREF, false);
//        Log.e(TAG,"background? "+(bg ? "YES" : "NO"));
//        return(bg);
        boolean bg = ((DPMIDIApplication) this.getApplicationContext()).getRunInBackground();
        Log.e(TAG,"background? "+(bg ? "YES" : "NO"));
        return bg;
    }


}
