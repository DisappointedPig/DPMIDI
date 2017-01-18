package com.disappointedpig.midi;

import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.events.MIDIConnectionEndEvent;
import com.disappointedpig.midi.events.MIDIConnectionRequestRejectedEvent;
import com.disappointedpig.midi.internal_events.ConnectionEstablishedEvent;
import com.disappointedpig.midi.internal_events.StreamConnectedEvent;
import com.disappointedpig.midi.internal_events.StreamDisconnectEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStartedEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStoppedEvent;

import org.greenrobot.eventbus.EventBus;

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
//    long ssrc;

    private ScheduledExecutorService connectService, syncService;
    private ScheduledFuture<?> connectFuture, syncFuture;
    int connectTaskCount = 0, syncTaskCount=0;
    int syncFailCount = 0;
    boolean receivedSyncResponse = true;
    MIDIStream() {
        this.isConnected = false;
        this.isInitiator = false;
        connectService = Executors.newSingleThreadScheduledExecutor();
        syncService = Executors.newSingleThreadScheduledExecutor();
        connectFuture = null;
        primarySyncComplete = false;
    }

    public void finalize() {
        if(connectFuture != null) {
            connectFuture.cancel(true);
        }
        if(syncFuture != null) {
            syncFuture.cancel(true);
        }
        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public class MIDIConnectTask implements Runnable {

        Bundle rinfo = null;
        private boolean success = false;
        public void setBundle(Bundle b) {
            rinfo = b;
        }

        public void run() {
            try {
                if(rinfo == null) {
                    shutdown();
                } else {
                    connectTaskCount++;
                    if(connectTaskCount > 12 ) {
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
                if(syncFailCount >= 10) {
                    shutdown();
                }
                if(isInitiator) {
                    receivedSyncResponse = false;
                }
                sendSynchronization(null);
                if(!primarySyncComplete && syncTaskCount > 10) {
                    primarySyncComplete = true;
//                    EventBus.getDefault().post(new MIDISyncronizationCompleteEvent());
//                    EventBus.getDefault().post(new MIDIConnectionEstablishedEvent());
//                    Log.d("Sync", "primary sync complete");

                    sendSyncComplete();
                    resetSyncService(50000);
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


    void connect(final Bundle rinfo) {
//        Log.d("MIDIStream","connect "+rinfo.getString("address")+":"+rinfo.getInt("port"));
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

    public Bundle getRinfo1() {
        return rinfo1;
    }

    private void sendInvitation(Bundle rinfo) {
//        Log.d("MIDIStream","sendInvitation "+rinfo.getString("address")+":"+rinfo.getInt("port"));
        MIDIControl invite = new MIDIControl();
        invite.createInvitation(initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
//        invite.dumppacket();
        MIDISession.getInstance().sendUDPMessage(invite,rinfo);
    }

    private int generateRandomInteger(int octets) {
        return (int) Math.round(Math.random() * Math.pow(2, 8 * octets));
    }

    void handleControlMessage(MIDIControl control, Bundle rinfo) {

        switch(control.command) {
            case INVITATION:
//                Log.d("MIDIStream","handle INVITATION");
                handleInvitation(control,rinfo);
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
            rinfo1.putString("name", control.name);
        } else if(rinfo.getInt("port") == rinfo1.getInt("port")) {
            return;
        } else if(rinfo2 == null) {
            rinfo2 = rinfo;
            this.isConnected = true;
        } else {
            return;
        }
        this.sendInvitationAccepted(rinfo);
    }

    private void handleInvitationAccepted(MIDIControl control, Bundle rinfo) {
        if (!this.isConnected && this.rinfo1 == null) {
            connectFuture.cancel(true);
            rinfo1 = rinfo;
            rinfo1.putString("name",control.name);
            rinfo.putInt("port",rinfo.getInt("port")+1);
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
            resetSyncService(2000);
        } else {
            Log.d("MIDI2Stream","unhandled invitation accept");
        }
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
                connectFuture.cancel(false);
            }
        }, 1, MINUTES);
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
