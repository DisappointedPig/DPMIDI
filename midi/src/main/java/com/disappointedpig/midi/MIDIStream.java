package com.disappointedpig.midi;

import android.os.Bundle;
import android.os.Debug;
import android.util.Log;

import com.disappointedpig.midi.events.MIDIConnectionEndEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestRejectedEvent;
import com.disappointedpig.midi.internal_events.ConnectionEstablishedEvent;
import com.disappointedpig.midi.internal_events.StreamConnectedEvent;
import com.disappointedpig.midi.internal_events.StreamDisconnectEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStartedEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStoppedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

class MIDIStream {

    int initiator_token = 0;
    int ssrc = 0;
    private Bundle rinfo1 = null;
    private Bundle rinfo2 = null;
    private String name = "";
    int lastSentSequenceNr =  (int)Math.round(Math.random() * 0xffff);
    int firstReceivedSequenceNumber = -1;
    int lastReceivedSequenceNumber = -1;
    long latency = 0L;
    private boolean isConnected = false;
    long receiverFeedbackTimeout = 0L;
    long lastMessageTime = 0L;
    private long timeDifference = 0L;
    private boolean isInitiator = false;
    private boolean syncStarted = false;
    private boolean primarySyncComplete = false;
    private int syncCount = 0;

    private long lastPacketReceivedTime = 0L;
//    long ssrc;

    private ScheduledExecutorService connectService, syncService, checkConnectionService;
    private ScheduledFuture<?> connectFuture, syncFuture, checkConnectionFuture;
    int connectTaskCount = 0, syncTaskCount=0;
    int syncFailCount = 0;
    boolean receivedSyncResponse = true;

    private static final int STREAM_CONNECTED_TIMEOUT_DEFAULT = 60000;
    private static final int SYNC_PRIMARY_FREQUENCY_DEFAULT = 2000;
    private static final int SYNC_FREQUENCY_DEFAULT = 100000;
    private static final int CONNECT_COUNT_DEFAULT = 12;
    private static final int PRIMARY_SYNC_COUNT_DEFAULT = 10;
    private static final int SYNC_FAIL_COUNT_DEFAULT = 10;
    private static final String TAG = "MIDIStream";
    private static final boolean DEBUG = false;

    private int connectionTimeoutMax = STREAM_CONNECTED_TIMEOUT_DEFAULT;
    private int connectCountMax = CONNECT_COUNT_DEFAULT;
    private int primarySyncCountMax = PRIMARY_SYNC_COUNT_DEFAULT;
    private int syncFailCountMax = SYNC_FAIL_COUNT_DEFAULT;
    private int syncServicePrimaryFrequency = SYNC_PRIMARY_FREQUENCY_DEFAULT;
    private int syncServiceFrequency = SYNC_FREQUENCY_DEFAULT;

    MIDIStream() {
        this.isConnected = false;
        this.isInitiator = false;
        connectService = Executors.newSingleThreadScheduledExecutor();
        syncService = Executors.newSingleThreadScheduledExecutor();
        checkConnectionService = Executors.newSingleThreadScheduledExecutor();
        connectFuture = null;
        primarySyncComplete = false;
    }

    public void finalize() {
        cancelConnectFuture();
        cancelSyncFuture();
        cancelCheckConnectionFuture();
        if(!connectService.isShutdown()) {
            connectService.shutdownNow();
        }
        if(!syncService.isShutdown()) {
            syncService.shutdownNow();
        }
        if(!checkConnectionService.isShutdown()) {
            checkConnectionService.shutdownNow();
        }
        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public class MIDIConnectTask implements Runnable {

        Bundle rinfo = null;
        public void setBundle(Bundle b) {
            rinfo = b;
        }

        public void run() {
            try {
                if(rinfo == null) {
                    shutdown();
                } else {
                    connectTaskCount++;
                    if(connectTaskCount > connectCountMax ) {
                        shutdown();
                        return;
                    }
//                    Log.d("MIDIConnectTask","connectTaskCount:"+connectTaskCount +" ssrc:"+ssrc);
                    sendInvitation(rinfo);
                }

            } catch (Exception e) {
//                Log.d("MIDIStream","exception in send midi task");
//                e.printStackTrace();
            }
        }
        void shutdown() {
            sendEnd(rinfo);
            connectFuture.cancel(true);
        }
    }

    public void sendSyncComplete() {
        receivedSyncResponse = true;
        syncFailCount = 0;
        EventBus.getDefault().post(new SyncronizeStoppedEvent());
    }

    public class SyncTask implements Runnable {

        Bundle rinfo = null;

        public void setBundle(Bundle b) {
            rinfo = b;
        }

        public void run() {
            try {
                syncTaskCount++;
//                Log.d("Sync","syncTaskCount:"+syncTaskCount+" timedifference:"+timeDifference);
                if(!receivedSyncResponse) {
                    syncFailCount++;
                }
                if(syncFailCount >= syncFailCountMax) {
                    shutdown();
                }
                if(isInitiator) {
                    receivedSyncResponse = false;
                }
                sendSynchronization(null);
                if(!primarySyncComplete && syncTaskCount > primarySyncCountMax) {
                    primarySyncComplete = true;
//                    EventBus.getDefault().post(new MIDISyncronizationCompleteEvent());
//                    EventBus.getDefault().post(new MIDIConnectionEstablishedEvent());
//                    Log.d("Sync", "primary sync complete");

                    sendSyncComplete();
                    resetSyncService(syncServiceFrequency);
                }
            } catch (Exception e) {
                shutdown();
            }
        }
        void shutdown() {
            sendEnd(rinfo);
            syncFuture.cancel(true);
        }
    }

    public class CheckConnectionTask implements Runnable {

        public void run() {

            long timedifference = System.currentTimeMillis() - lastPacketReceivedTime;
            Log.e(TAG,"last packet time difference is "+timedifference);
            if(timedifference > connectionTimeoutMax) {
                // stream connection has probably failed
                Log.e(TAG,"probably lost connection with "+rinfo1.toString()+" ssrc"+ssrc);

                shutdown();
            } else {
                Log.e(TAG,"connection still up "+rinfo1.toString()+" ssrc"+ssrc);
            }
        }

        void shutdown() {
            sendEnd();
            checkConnectionFuture.cancel(true);
        }
    }


    void connect(final Bundle rinfo) {
//        Log.d("MIDIStream","connect "+rinfo.getString(RINFO_ADDR)+":"+rinfo.getInt(RINFO_PORT));
        if(isConnected) {
            // already connected, should not reconnect with same stream
            Log.e("MIDI2Stream","already connected");
            return;
        }
        if (this.initiator_token == 0) {
            this.initiator_token = generateRandomInteger(4);
            Log.e("MIDIStream","generateRandomInteger for initiator initiator_token "+this.initiator_token);
        }
        this.isInitiator = true;
//        currentRinfo = rinfo;
//        new InvitationTask().execute(rinfo);
        MIDIConnectTask t = new MIDIConnectTask();
        t.setBundle(rinfo);

        connectFuture = connectService.scheduleAtFixedRate(t, 0, 1, SECONDS);

        connectService.schedule(new Runnable() {
            @Override
            public void run() {
                connectFuture.cancel(false);
            }
        }, 1, MINUTES);
    }

    private void cancelConnectFuture() {
        if(connectFuture != null && !connectFuture.isCancelled()) {
            connectFuture.cancel(true);
        }
    }

    private void cancelSyncFuture() {
        if(syncFuture != null && !syncFuture.isCancelled()) {
            syncFuture.cancel(true);
        }
    }

    private void cancelCheckConnectionFuture() {
        if(checkConnectionFuture != null && !checkConnectionFuture.isCancelled()) {
            checkConnectionFuture.cancel(true);
        }
    }


    public void disconnect() {
        cancelConnectFuture();
        cancelSyncFuture();
        cancelCheckConnectionFuture();
    }

    public void shutdown() {
        disconnect();
        if(!connectService.isShutdown()) {
            connectService.shutdown();
        }
        if(!syncService.isShutdown()) {
            syncService.shutdown();
        }
        if(!checkConnectionService.isShutdown()) {
            checkConnectionService.shutdown();
        }

    }

    public Bundle getRinfo1() {
        return rinfo1;
    }

    private void sendInvitation(Bundle rinfo) {
//        Log.d("MIDIStream","sendInvitation "+rinfo.getString(RINFO_ADDR)+":"+rinfo.getInt(RINFO_PORT));
        MIDIControl invite = new MIDIControl();
        invite.createInvitation(initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
//        invite.dumppacket();
        MIDISession.getInstance().sendUDPMessage(invite,rinfo);
    }

    private int generateRandomInteger(int octets) {
        return (int) Math.round(Math.random() * Math.pow(2, 8 * octets));
    }

    void handleControlMessage(MIDIControl control, Bundle rinfo) {

        lastPacketReceivedTime = System.currentTimeMillis();

        switch(control.command) {
            case INVITATION:
//                Log.d("MIDIStream","handle INVITATION");
                handleInvitation(control,rinfo);
                resetCheckConnectionService();
                break;
            case INVITATION_ACCEPTED:
//                Log.d("MIDIStream","handle INVITATION_ACCEPTED");
                handleInvitationAccepted(control, rinfo);
                break;
            case INVITATION_REJECTED:
//                Log.d("MIDIStream","handle INVITATION_REJECTED");
                handleInvitationRejected(control, rinfo);
                break;
            case END:
//                Log.d("MIDIStream","handle END");
                handleEnd();
                break;
            case SYNCHRONIZATION:
//                Log.d("MIDIStream","handle SYNCHRONIZATION");
                if(isInitiator) {
                    receivedSyncResponse = true;
                }
                sendSynchronization(control);
                break;
            case RECEIVER_FEEDBACK:
//                Log.d("MIDIStream","handle RECEIVER_FEEDBACK");
                break;
            case BITRATE_RECEIVE_LIMIT:
//                Log.d("MIDIStream","handle BITRATE_RECEIVE_LIMIT");
                break;
            default:
//                Log.d("MIDIStream", "unhandled command");
                break;
        }
    }



    private void handleInvitation(MIDIControl control, Bundle rinfo) {
        if(rinfo1 == null) {
            rinfo1 = rinfo;
            this.initiator_token = control.initiator_token;
            this.name = control.name;
            this.ssrc = control.ssrc;
            rinfo1.putString(Consts.RINFO_NAME, control.name);
        } else if(rinfo.getInt(Consts.RINFO_PORT) == rinfo1.getInt(Consts.RINFO_PORT)) {
            return;
        } else if(rinfo2 == null) {
            rinfo2 = rinfo;
            this.isConnected = true;
        } else {
            return;
        }
        // TODO : check if connection is allowed...
        this.sendInvitationAccepted(rinfo);
    }

    private void handleInvitationAccepted(MIDIControl control, Bundle rinfo) {
        if (!this.isConnected && this.rinfo1 == null) {
            connectFuture.cancel(true);
            rinfo1 = rinfo;
            rinfo1.putString(Consts.RINFO_NAME,control.name);
            rinfo.putInt(Consts.RINFO_PORT,rinfo.getInt(Consts.RINFO_PORT)+1);
            connectTaskCount = 0;
            connect(rinfo);
        } else if(!this.isConnected && rinfo2 == null) {
            connectFuture.cancel(true);
            connectTaskCount = 0;

            this.isConnected = true;
            this.name = control.name;
            this.ssrc = control.ssrc;
            rinfo2 = rinfo;
            EventBus.getDefault().post(new StreamConnectedEvent(this.initiator_token));
            resetSyncService(syncServicePrimaryFrequency);
        } else {
            Log.d("MIDI2Stream","unhandled invitation accept");
        }
    }

    private void resetCheckConnectionService() {
        if(checkConnectionFuture != null) {
            checkConnectionFuture.cancel(true);
        }

        CheckConnectionTask t = new CheckConnectionTask();
        checkConnectionFuture = checkConnectionService.scheduleAtFixedRate(t, 0, 1, MINUTES);

        checkConnectionService.schedule(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG,"huh?");
                checkConnectionFuture.cancel(false);
            }
        }, 2, MINUTES);

    }

    private void resetSyncService(int time) {
        if(!primarySyncComplete) {
            EventBus.getDefault().post(new SyncronizeStartedEvent());
        }
        if(syncFuture != null) {
            syncFuture.cancel(true);
        }

        SyncTask t = new SyncTask();
        syncFuture = syncService.scheduleAtFixedRate(t, 0, time, MILLISECONDS);

        syncService.schedule(new Runnable() {
            @Override
            public void run() {
                syncFuture.cancel(false);
            }
        }, 2, MINUTES);
    }

    private void handleInvitationRejected(MIDIControl control, Bundle rinfo) {
        connectFuture.cancel(true);
        EventBus.getDefault().post(new MIDIConnectionRequestRejectedEvent());
    }

    private void handleEnd() {
        this.isConnected = false;
        // shutdown sync
        if(connectFuture != null && !connectFuture.isCancelled()) {
            connectFuture.cancel(true);
        }
        if(syncFuture != null && !syncFuture.isCancelled()) {
            syncFuture.cancel(true);
        }
        if(checkConnectionFuture != null && !checkConnectionFuture.isCancelled()) {
            checkConnectionFuture.cancel(true);
        }
        EventBus.getDefault().post(new MIDIConnectionEndEvent());
        EventBus.getDefault().post(new StreamDisconnectEvent(ssrc));
    }

    private void sendInvitationAccepted(Bundle rinfo) {
        MIDIControl message = new MIDIControl();

        message.createInvitationAccepted(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);


        if(!syncStarted && rinfo2 != null) {
            syncStarted = true;
            EventBus.getDefault().post(new SyncronizeStartedEvent());
        }

    }

    public void sendInvitationRejected(Bundle rinfo) {
        MIDIControl message = new MIDIControl();
        message.createInvitationRejected(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

    }


    public void sendMessage(MIDIMessage m) {
        this.lastSentSequenceNr = (this.lastSentSequenceNr + 1) % 0x10000;
        m.sequenceNumber = this.lastSentSequenceNr;
        MIDISession.getInstance().sendUDPMessage(m, rinfo2);
    }

    public void sendEnd() {
        MIDIControl message = new MIDIControl();
        message.createEnd(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo1);
        EventBus.getDefault().post(new StreamDisconnectEvent(ssrc));
    }

    public void sendEnd(Bundle rinfo) {
        MIDIControl message = new MIDIControl();
        message.createEnd(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

        if(isConnected) {
            EventBus.getDefault().post(new StreamDisconnectEvent(ssrc));
        }
        else {
            EventBus.getDefault().post(new StreamDisconnectEvent(ssrc, initiator_token));
        }
        isConnected = false;

    }

    private void sendSynchronization(MIDIControl inboundSyncMessage) {
        long now = MIDISession.getInstance().getNow();
        MIDIControl outboundSyncMessage;
        int count = (inboundSyncMessage != null) ? inboundSyncMessage.count : -1;
        if(count == 3) { count = -1; }
        outboundSyncMessage = new MIDIControl();
        outboundSyncMessage.createSyncronization(
                MIDISession.getInstance().ssrc,
                count+1,
                (count != -1) ? inboundSyncMessage.timestamp1 : 0,
                (count != -1) ? inboundSyncMessage.timestamp2 : 0,
                (count != -1) ? inboundSyncMessage.timestamp3 : 0);

        switch(count) {
            case -1: // send count:0
                outboundSyncMessage.timestamp1 = now;
                outboundSyncMessage.timestamp2 = (timeDifference != 0L) ? now - timeDifference : 0L;
                outboundSyncMessage.timestamp3 = (timeDifference != 0L) ? now + timeDifference : 0L;
                break;
            case 0: // received count:0, respond count:1
                outboundSyncMessage.timestamp2 = now;
                outboundSyncMessage.timestamp3 = now - timeDifference;
                break;
            case 1: // received count:1, respond count:2
//                outboundSyncMessage.sync_timestamp3 = now;
//                latency = inboundSyncMessage.sync_timestamp3 - inboundSyncMessage.sync_timestamp1;
//                timeDifference = Math.round(inboundSyncMessage.sync_timestamp3-inboundSyncMessage.sync_timestamp2) - latency;

                timeDifference = (inboundSyncMessage.timestamp3 - inboundSyncMessage.timestamp1) / 2;
                timeDifference = inboundSyncMessage.timestamp3 + timeDifference - now;
                outboundSyncMessage.timestamp3 = now;
                break;
            case 2:  // received count:2, sending nothing
                     /* compute media delay */
//                diff = ( command->data.sync.timestamp3 - command->data.sync.timestamp1 ) / 2;
                      /* approximate time difference between peer and self */
//                diff = command->data.sync.timestamp3 + diff - timestamp;
                timeDifference = (inboundSyncMessage.timestamp3 - inboundSyncMessage.timestamp1) / 2;
                timeDifference = inboundSyncMessage.timestamp3 + timeDifference - now;

        }
        if(outboundSyncMessage.count < 3) {
            MIDISession.getInstance().sendUDPMessage(outboundSyncMessage, rinfo2);
            if(!primarySyncComplete && ++syncCount > 3) {
                primarySyncComplete = true;
                EventBus.getDefault().post(new SyncronizeStoppedEvent());
                EventBus.getDefault().post(new ConnectionEstablishedEvent(rinfo1));
            }
        }

    }

}
