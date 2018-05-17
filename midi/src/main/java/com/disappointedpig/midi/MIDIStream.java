package com.disappointedpig.midi;

import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.events.MIDIConnectionEndEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestReceivedEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestRejectedEvent;
import com.disappointedpig.midi.events.MIDIConnectionSentRequestEvent;
import com.disappointedpig.midi.internal_events.ConnectionEstablishedEvent;
import com.disappointedpig.midi.internal_events.ConnectionFailedEvent;
import com.disappointedpig.midi.internal_events.StreamConnectedEvent;
import com.disappointedpig.midi.internal_events.StreamDisconnectEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStartedEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStoppedEvent;

import org.greenrobot.eventbus.EventBus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import static com.disappointedpig.midi.MIDIConstants.RINFO_FAIL;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;


/* ---------------------------------------------------------
    internal events:
        connection invite received
        connection invite sent
        connection invite reject received
        connection invite accept received
        connection end received
        primary sync started
        primary sync complete
        connection failed (reason?)
            unable to connect
            connection lost, no reconnect
            connection lost, reconnect timeout
            connection lost
   --------------------------------------------------------- */
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

    private ScheduledExecutorService connectService, syncService, checkConnectionService;
    private ScheduledFuture<?> connectFuture, syncFuture, checkConnectionFuture;
    int connectTaskCount = 0, syncTaskCount=0;
    int syncFailCount = 0;

    int reconnectFailCount = 0;

    boolean receivedSyncResponse = true;

    private static final int STREAM_CONNECTED_TIMEOUT_DEFAULT = 60000;
    private static final int SYNC_PRIMARY_FREQUENCY_DEFAULT = 2000;
    private static final int SYNC_FREQUENCY_DEFAULT = 30000;
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

    public boolean connectionMatch(Bundle r) {
        boolean match = false;
        if(r != null) {
            Log.d(TAG, "connectionMatch " + r.toString() + " ? " + rinfo1.toString() + "/" + rinfo2.toString());

            if (rinfo1 == null || rinfo2 == null) {
                return false;
            }

            if (r.getString(com.disappointedpig.midi.MIDIConstants.RINFO_ADDR).equals(rinfo1.getString(com.disappointedpig.midi.MIDIConstants.RINFO_ADDR))) {
                Log.d(TAG, "addr = addr " + r.getString(com.disappointedpig.midi.MIDIConstants.RINFO_ADDR));
                if ((r.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT) == rinfo1.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT)) ||
                        ((r.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT) == rinfo2.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT)))) {
                    Log.d(TAG, "port = port " + r.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT));
                    match = true;
                } else {
                    Log.d(TAG, "port != port ");

                }
            } else {
                Log.d(TAG, "address != address ");

            }
        }
        return match;
    }

    private class MIDIConnectTask implements Runnable {

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
//                        Log.e(TAG,"connection task count hit max");
//                        rinfo.putBoolean(RINFO_FAIL,true);
                        EventBus.getDefault().post(new ConnectionFailedEvent(MIDIFailCode.UNABLE_TO_CONNECT,rinfo,initiator_token));

                        cleanupFailedConnection();

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
//            Log.d(TAG,"connection task shutdown");
            sendEnd(rinfo);
            connectFuture.cancel(false);
        }
    }

    private class SyncTask implements Runnable {

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
                    Log.d(TAG,"Sync fail count > max ");
                    EventBus.getDefault().post(new ConnectionFailedEvent(MIDIFailCode.SYNC_FAILURE,rinfo));

                    cleanupFailedConnection();
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
            syncFuture.cancel(false);
        }
    }

    private class CheckConnectionTask implements Runnable {

        public void run() {

            long timedifference = System.currentTimeMillis() - lastPacketReceivedTime;
//            Log.e(TAG,"last packet time difference is "+timedifference);
            if(timedifference > connectionTimeoutMax) {
                // stream connection has probably failed
                Log.e(TAG,"last packet time difference is "+timedifference+"   probably lost connection with "+rinfo1.toString()+" ssrc"+ssrc);
//                rinfo1.putBoolean(RINFO_FAIL,true);
                EventBus.getDefault().post(new ConnectionFailedEvent(MIDIFailCode.CONNECTION_LOST,rinfo1));
                cleanupFailedConnection();
                shutdown();
            } else {
                Log.e(TAG,"last packet time difference is "+timedifference+"   connection still up "+rinfo1.toString()+" ssrc"+ssrc);
            }
        }

        void shutdown() {
            sendEnd();
            checkConnectionFuture.cancel(false);
        }
    }



    private void sendSyncComplete() {
        receivedSyncResponse = true;
        syncFailCount = 0;
        // TODO: change this to synccomplete
        EventBus.getDefault().post(new SyncronizeStoppedEvent(this.rinfo1));
    }


    void connect(final Bundle rinfo) {
        Log.d("MIDIStream","connect "+rinfo.getString(com.disappointedpig.midi.MIDIConstants.RINFO_ADDR)+":"+rinfo.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT));
        if(isConnected) {
            // already connected, should not reconnect with same stream
            Log.e("MIDI2Stream","this stream is already connected");
            return;
        }
        if (this.initiator_token == 0) {
            this.initiator_token = generateRandomInteger(4);
            Log.e("MIDIStream","generateRandomInteger for initiator initiator_token "+this.initiator_token);
        }
        this.isInitiator = true;


        Log.d(TAG,"create connection task "+rinfo.getInt(RINFO_FAIL,0));
        MIDIConnectTask t = new MIDIConnectTask();
        t.setBundle(rinfo);
        int delay =(rinfo.getInt(RINFO_FAIL,0) * 5);
        connectFuture = connectService.scheduleAtFixedRate(t, delay, 1, SECONDS);


//        connectService.schedule(new Runnable() {
//            @Override
//            public void run() {
//                Log.d(TAG,"timeout - cancel connection task ");
//                if(connectFuture != null) {
//                    connectFuture.cancel(false);
//                }
//            }
//        }, 1, MINUTES);
    }

    void cleanupFailedConnection() {
        Log.e(TAG,"cleanupFailedConnection");
        isConnected = false;
        isInitiator = false;
        primarySyncComplete = false;
        rinfo2 = rinfo1 = null;
        cancelCheckConnectionFuture();
        cancelConnectFuture();
        cancelSyncFuture();
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

    void disconnect() {
        cancelConnectFuture();
        cancelSyncFuture();
        cancelCheckConnectionFuture();
    }

    void shutdown() {
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

    Bundle getRinfo1() {
        return rinfo1;
    }

    private void sendInvitation(Bundle rinfo) {
//        Log.d("MIDIStream","sendInvitation "+rinfo.getString(RINFO_ADDR)+":"+rinfo.getInt(RINFO_PORT));
        MIDIControl invite = new MIDIControl();
        invite.createInvitation(initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
//        invite.dumppacket();
        MIDISession.getInstance().sendUDPMessage(invite,rinfo);
        EventBus.getDefault().post(new MIDIConnectionSentRequestEvent(rinfo));
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
            rinfo1 = (Bundle) rinfo.clone();
            this.initiator_token = control.initiator_token;
            this.name = control.name;
            this.ssrc = control.ssrc;
            rinfo1.putString(com.disappointedpig.midi.MIDIConstants.RINFO_NAME, control.name);
            EventBus.getDefault().post(new MIDIConnectionRequestReceivedEvent(rinfo1));
        } else if(rinfo.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT) == rinfo1.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT)) {
            return;
        } else if(rinfo2 == null) {
            rinfo2 = (Bundle) rinfo.clone();
            this.isConnected = true;

        } else {
            return;
        }

// this needs work...
        if(MIDISession.getInstance().isHostConnectionAllowed(rinfo)) {
            this.sendInvitationAccepted(rinfo);
        } else {
            this.sendInvitationRejected(rinfo);
        }
    }

    private void handleInvitationAccepted(MIDIControl control, Bundle rinfo) {
        Log.d(TAG,"handleInvitationAccepted "+rinfo.toString());
        if (!this.isConnected && this.rinfo1 == null) {
            Log.d(TAG,"cancel connectFuture");
            connectFuture.cancel(true);
            Log.d(TAG,"set rinfo1 "+rinfo.toString());
            rinfo1 = (Bundle) rinfo.clone();
            rinfo1.putString(com.disappointedpig.midi.MIDIConstants.RINFO_NAME,control.name);
            rinfo.putInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT,rinfo.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT)+1);
            connectTaskCount = 0;
            connect(rinfo);
        } else if(!this.isConnected && rinfo2 == null) {
            Log.d(TAG,"cancel connectFuture");
            connectFuture.cancel(true);
            connectTaskCount = 0;

            this.isConnected = true;
            this.name = control.name;
            this.ssrc = control.ssrc;
            Log.d(TAG,"set rinfo2 "+rinfo.toString());
            rinfo2 = (Bundle) rinfo.clone();
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
        checkConnectionFuture = checkConnectionService.scheduleAtFixedRate(t, 0, 30000, MILLISECONDS);

    }

    private void resetSyncService(int time) {
        if(!primarySyncComplete) {
            EventBus.getDefault().post(new SyncronizeStartedEvent(this.rinfo1));
        }
        if(syncFuture != null) {
            syncFuture.cancel(true);
        }

        SyncTask t = new SyncTask();
        syncFuture = syncService.scheduleAtFixedRate(t, 0, time, MILLISECONDS);

    }

    private void handleInvitationRejected(MIDIControl control, Bundle rinfo) {
        connectFuture.cancel(true);


        EventBus.getDefault().post(new ConnectionFailedEvent(MIDIFailCode.REJECTED_INVITATION,rinfo,control.initiator_token));
        EventBus.getDefault().post(new MIDIConnectionRequestRejectedEvent(rinfo));
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
//        if(MIDISession.getInstance().getAutoReconnect()) {
//            Bundle rinfo = (Bundle) rinfo1.clone();
//            rinfo1 = rinfo2 = null;
//            sendInvitation(rinfo);
//            syncStarted = false;
//            syncFailCount = 0;
//            syncCount = 0;
//            this.isInitiator = false;
//            connectFuture = null;
//            checkConnectionFuture = null;
//            syncFuture = null;
//            primarySyncComplete = false;
//            return;
//        }
//        EventBus.getDefault().post(new MIDIConnectionEndEvent(this.rinfo1));
        EventBus.getDefault().post(new StreamDisconnectEvent(ssrc,(Bundle)this.rinfo1.clone()));
    }

    private void sendInvitationAccepted(Bundle rinfo) {
        MIDIControl message = new MIDIControl();

        message.createInvitationAccepted(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

        if(!syncStarted && rinfo2 != null) {
            syncStarted = true;
            EventBus.getDefault().post(new SyncronizeStartedEvent(this.rinfo1));
        }

    }

    private void sendInvitationRejected(Bundle rinfo) {
        MIDIControl message = new MIDIControl();
        message.createInvitationRejected(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

    }


    void sendMessage(MIDIMessage m) {
        this.lastSentSequenceNr = (this.lastSentSequenceNr + 1) % 0x10000;
        m.sequenceNumber = this.lastSentSequenceNr;
        MIDISession.getInstance().sendUDPMessage(m, rinfo2);
    }

    void sendEnd() {
//        MIDIControl message = new MIDIControl();
//        message.createEnd(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
//        MIDISession.getInstance().sendUDPMessage(message, rinfo1);
//        EventBus.getDefault().post(new StreamDisconnectEvent(ssrc));
        if(rinfo1 != null) {
            sendEnd(rinfo1);
        }
    }

    private void sendEnd(Bundle rinfo) {
        Log.d(TAG,"send end "+rinfo.toString());
        MIDIControl message = new MIDIControl();
        message.createEnd(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

        if(isConnected) {
            EventBus.getDefault().post(new StreamDisconnectEvent(ssrc, (Bundle)this.rinfo1.clone()));
        }
        else {
            EventBus.getDefault().post(new StreamDisconnectEvent(ssrc, initiator_token, (Bundle)this.rinfo1.clone()));
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
                EventBus.getDefault().post(new SyncronizeStoppedEvent(this.rinfo1));
                EventBus.getDefault().post(new ConnectionEstablishedEvent(rinfo1));
            }
        }

    }

}
