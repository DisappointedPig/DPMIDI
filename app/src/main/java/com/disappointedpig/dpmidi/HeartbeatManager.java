package com.disappointedpig.dpmidi;

import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.MIDISession;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

public class HeartbeatManager {

    private ScheduledExecutorService heartbeatService;
    private ScheduledFuture<?> heartbeatFuture;

    private int delay = 5;

    private boolean isRunning = false;
    private Bundle currentHeartbeat = null;

    /*
        heartbeat bundle
            "type" int  MIDI|OSC
            "note" int
            "velocity" int
            "address"   string
            "message"   string

     */

    public class HeartbeatTask implements Runnable {
        Bundle heartbeat = null;

        public void setHeartbeat(Bundle b) {
            heartbeat = b;
        }

        public void run() {
            try {
                if(heartbeat == null) {
                    shutdown();
                } else {
                    switch(heartbeat.getInt("type",0)) {
                        case 0:
                            MIDISession.getInstance().sendMessage(heartbeat.getInt("note"),heartbeat.getInt("velocity"));
                            break;
                        case 1:
                            // send osc heartbeat
                            break;
                    }
                }

            } catch (Exception e) {

            }
        }

        void shutdown() {

        }
    }

    public HeartbeatManager() {
        heartbeatService = Executors.newSingleThreadScheduledExecutor();
    }

    public void setDelay(int d) {
        if(d > 0) {
            this.delay = d;
            if(isRunning) {
                stopHeartbeat();
                startHeartbeat();
            }
        }
    }

    public void setHeartbeat(final Bundle h) {
        Log.d("Heartbeat","setHeartbeat");
        currentHeartbeat = h;
        if(isRunning) {
            stopHeartbeat();
            startHeartbeat();
        }
    }

    public void startHeartbeat() {

        if(currentHeartbeat == null || isRunning) {
            Log.e("Heartbeat","problem starting");
            return;
        }

        HeartbeatTask t = new HeartbeatTask();
        t.setHeartbeat(currentHeartbeat);

        heartbeatFuture = heartbeatService.scheduleAtFixedRate(t, 0, delay, SECONDS);

//        heartbeatService.schedule(new Runnable() {
//            @Override
//            public void run() {
//                heartbeatFuture.cancel(false);
//            }
//        }, 1, MINUTES);
    }

    public void stopHeartbeat() {
        if(isRunning) {
            isRunning = false;
            heartbeatService.schedule(new Runnable() {
                @Override
                public void run() {
                    heartbeatFuture.cancel(false);
                }
            }, 10, MILLISECONDS);
        }
    }
}
