package com.disappointedpig.dpmidi;

import android.app.Activity;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Process;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.disappointedpig.midi.MIDISession;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.disappointedpig.dpmidi.ConnectionState.NOT_RUNNING;

public class ConnectionManagerService extends Service implements DPMIDIForeground.Listener {

    private static final String TAG = "CMS";

    private boolean cmsIsRunning = false;
    private WifiManager.WifiLock wifiLock;
    private PowerManager.WakeLock wakeLock;

    private final Binder binder     = new LocalBinder();
    private Map<Activity, IListenerFunctions> clients    = new ConcurrentHashMap<Activity, IListenerFunctions>();

    private ConnectionState MIDIState;
    private boolean midiRunning = false;
    private static final String DEFAULT_BONJOUR_NAME = "testing";


    public ConnectionManagerService() {
        Log.i(TAG, "--------------------------\n    init cms\n--------------------------\n");
        wifiLock = ((WifiManager) DPMIDIApplication.getAppContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "stagecallerWIFILock");
        wakeLock = ((PowerManager) DPMIDIApplication.getAppContext().getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "stagecallerWakeLock");

        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

        MIDIState = NOT_RUNNING;

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
//                EventBus.getDefault().register(this);
                cmsIsRunning = true;
                Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);
                checkMIDI();
                checkLocks();

                for (final Activity client : clients.keySet()) {

                    try {

                        // Call the setLocation in the main thread (ui thread) as it updates
                        // the ui.
                        // If we dont use the handler and just exec the code in the run() we
                        // get a CalledFromWrongThreadException
                        Handler lo = new Handler(Looper.getMainLooper());
                        lo.post(new Runnable() {

                            public void run() {
                                IListenerFunctions callback = clients.get(client);
                                callback.cmsStarted();
                            }
                        });

                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                }


            }
        } else {
            if(intent.getAction().equals(Constants.ACTION.STOPCMGR_ACTION)) {
                Log.i(TAG, "Received STOPCMGR_ACTION ");
                cmsIsRunning = false;
                DPMIDIForeground.get().removeListener(this);
                stopMIDI();
//                EventBus.getDefault().unregister(this);
                stopForeground(true);
                stopSelf();
            } else if(intent.getAction().equals(Constants.ACTION.START_MIDI_ACTION)) {
                Log.i(TAG, "Received START_MIDI_ACTION ");
                startMIDI();
            } else if(intent.getAction().equals(Constants.ACTION.STOP_MIDI_ACTION)) {
                Log.i(TAG, "Received STOP_MIDI_ACTION ");
                stopMIDI();
            } else {
                Log.i(TAG, "Received UNKNOWN ACTION");
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return binder;
    }

    private void createNotificationIntent() {
        Log.i(TAG, "createNotificationIntent ");
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK  | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

//        Intent startMIDIIntent = new Intent(this, ConnectionManagerService.class);
//        startMIDIIntent.setAction(Constants.ACTION.START_MIDI_ACTION);
//        PendingIntent pstartMIDIIntent = PendingIntent.getService(this, 0, startMIDIIntent, 0);

//        Intent stopMIDIIntent = new Intent(this, ConnectionManagerService.class);
//        stopMIDIIntent.setAction(Constants.ACTION.STOP_MIDI_ACTION);
//        PendingIntent pstopMIDIIntent = PendingIntent.getService(this, 0, stopMIDIIntent, 0);

        Intent stopCMGRIntent = new Intent(this, ConnectionManagerService.class);
        stopCMGRIntent.setAction(Constants.ACTION.STOPCMGR_ACTION);
        PendingIntent pstopCMGRSIntent = PendingIntent.getService(this, 0, stopCMGRIntent, 0);


        Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);

        Notification notification = new android.support.v4.app.NotificationCompat.Builder(this)
                .setContentTitle("StageCaller")
                .setTicker("StageCaller")
                .setContentText("stagecaller")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "Stop CMGRS", pstopCMGRSIntent)
                .build();

//        .addAction(android.R.drawable.ic_media_play, "Start MIDI", pstartMIDIIntent)
//        .addAction(android.R.drawable.ic_media_pause, "Stop MIDI", pstopMIDIIntent)

        startForeground(Constants.NOTIFICATION_ID.CONNECTIONMGR, notification);

    }

    // check if midi should be on
    private void checkMIDI() {
        // check if midi should be on
        Log.d(TAG,"check MIDI");
        SharedPreferences sharedpreferences = DPMIDIApplication.getAppContext().getSharedPreferences("SCPreferences", Context.MODE_PRIVATE);
        if(sharedpreferences != null) {
            Boolean midiPref = sharedpreferences.getBoolean(Constants.PREF.MIDI_STATE_PREF, false);
            if (midiPref && MIDIState == NOT_RUNNING) {
                startMIDI();
                Log.d(TAG,"--- start MIDI");

            } else if(!midiPref && MIDIState == ConnectionState.RUNNING) {
                stopMIDI();
                Log.d(TAG,"--- stop MIDI");
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
        Process.setThreadPriority(Process.THREAD_PRIORITY_FOREGROUND);
        Log.e(TAG,"set priority to THREAD_PRIORITY_FOREGROUND");
        checkMIDI();
    }

    public void onBecameBackground() {
        Log.d(TAG,"became background");

        checkLocks();
        // check if background is set

        if(!runInBackground()) {
            // turn off midi and osc as appropriate
            switch (MIDIState) {
                case RUNNING:
                case STARTING:
                    Log.e(TAG,"stopping midi");
                    Log.e(TAG,"set priority to THREAD_PRIORITY_DEFAULT");

                    Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);

                    stopMIDI();
                case NOT_RUNNING:
                case FAILED:
                default:
                    break;
            }
        } else {
            Log.e(TAG,"set priority to THREAD_PRIORITY_AUDIO");
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        }
    }

    private boolean runInBackground() {
        boolean bg = ((DPMIDIApplication) this.getApplicationContext()).getRunInBackground();
        Log.e(TAG,"should background? "+(bg ? "YES" : "NO"));
        return bg;
    }



    // -------------------------------------------------------------------------------

    private void setMIDIState(ConnectionState state) {
        MIDIState = state;
    }

    public void setupMIDI() {

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
        checkLocks();
        for (Activity client : clients.keySet()) {
            updateClients(client);
        }
    }

    public void stopMIDI() {
        MIDISession midi = MIDISession.getInstance();
        midiRunning = false;
        setMIDIState(NOT_RUNNING);
//        if(hbm != null) {
//            hbm.stopHeartbeat();
//        }

        if(midi != null) {
            midi.stop();
        } else {
            midiRunning = false;
            setMIDIState(ConnectionState.FAILED);
        }
        checkLocks();
        for (Activity client : clients.keySet()) {
            updateClients(client);
        }
    }


    public class LocalBinder extends Binder implements IServiceFunctions {

        // Registers a Activity to receive updates
        public void registerActivity(Activity activity, IListenerFunctions callback) {
            clients.put(activity, callback);
        }

        public void unregisterActivity(Activity activity) {
            clients.remove(activity);
        }

        public ConnectionState getMIDIState() {
            return MIDIState;
        }

        public boolean cmsIsRunning() { return cmsIsRunning; }

    }


    private void updateClients(final Activity client) {
        // Get the location
        try {

            // Call the setLocation in the main thread (ui thread) as it updates
            // the ui.
            // If we dont use the handler and just exec the code in the run() we
            // get a CalledFromWrongThreadException
            Handler lo = new Handler(Looper.getMainLooper());
            lo.post(new Runnable() {

                public void run() {
                    IListenerFunctions callback = clients.get(client);
                    callback.midiStateChanged(MIDIState);
                }
            });

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
