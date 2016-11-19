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

    MIDIStream() {
        this.isConnected = false;
        this.isInitiator = false;
        connectService = Executors.newSingleThreadScheduledExecutor();
        syncService = Executors.newSingleThreadScheduledExecutor();
        connectFuture = null;
        primarySyncComplete = false;
    }

    public class MIDITask implements Runnable {

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
//                    Log.d("MIDITask","connectTaskCount:"+connectTaskCount +" ssrc:"+ssrc);
                    if(connectTaskCount > 40 ) {
                        shutdown();
                    }
                    sendInvitation(rinfo);
                }

            } catch (Exception e) {

            }
        }
        void shutdown() {

        }
    }

    public void sendSyncComplete() {
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
                sendSynchronization(null);
                if(!primarySyncComplete && syncTaskCount > 10) {
                    primarySyncComplete = true;
//                    EventBus.getDefault().post(new MIDISyncronizationCompleteEvent());
//                    EventBus.getDefault().post(new MIDIConnectionEstablishedEvent());
//                    Log.d("Sync", "primary sync complete");

                    sendSyncComplete();
                    resetSyncService(10000);
                }
            } catch (Exception e) {

            }
        }
        void shutdown() {

        }
    }


    void connect(final Bundle rinfo) {
        Log.d("MIDIStream","connect "+rinfo.getString("address")+":"+rinfo.getInt("port"));
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
        MIDITask t = new MIDITask();
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
        Log.d("MIDIStream","sendInvitation "+rinfo.getString("address")+":"+rinfo.getInt("port"));
        MIDIControl invite = new MIDIControl();
        invite.createInvitation(initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(invite,rinfo);
    }

    private int generateRandomInteger(int octets) {
        return (int) Math.round(Math.random() * Math.pow(2, 8 * octets));
    }

    void handleControlMessage(MIDIControl control, Bundle rinfo) {
        switch(control.command) {
            case INVITATION:
                handleInvitation(control,rinfo);
                break;
            case INVITATION_ACCEPTED:
                handleInvitationAccepted(control, rinfo);
                break;
            case INVITATION_REJECTED:
                handleInvitationRejected(control, rinfo);
                break;
            case END:
                handleEnd();
                break;
            case SYNCHRONIZATION:
                sendSynchronization(control);
                break;
            case RECEIVER_FEEDBACK:
            case BITRATE_RECEIVE_LIMIT:
                Log.d("MIDI2Stream", "unhandled command");
                break;
        }
    }

    private void handleInvitation(MIDIControl control, Bundle rinfo) {
        if(rinfo1 == null) {
            rinfo1 = rinfo;
            this.initiator_token = control.initiator_token;
            this.name = control.name;
            this.ssrc = control.ssrc;
            rinfo1.putString("name",control.name);

            Log.d("MIDI2Stream", "Got an invitation from " + control.name + " on channel 1");
        } else if(rinfo2 == null) {
            rinfo2 = rinfo;
            Log.d("MIDI2Stream", "Got an invitation from " + control.name + " on channel 2");
            this.isConnected = true;
        }
        this.sendInvitationAccepted(rinfo);
    }

    private void handleInvitationAccepted(MIDIControl control, Bundle rinfo) {

        if (!this.isConnected && this.rinfo1 == null) {
            connectFuture.cancel(true);
            rinfo1 = rinfo;
            Log.d("MIDI2Stream", "invite accepted by " + control.name + " on channel 1");
            rinfo1.putString("name",control.name);
            rinfo.putInt("port",rinfo.getInt("port")+1);
            connectTaskCount = 0;
            Log.d("MIDI2Stream","shutdown connect");
            connect(rinfo);
        } else if(!this.isConnected && rinfo2 == null) {
            connectFuture.cancel(true);
            Log.d("MIDI2Stream","shutdown connect");
            connectTaskCount = 0;

            this.isConnected = true;
            this.name = control.name;
            this.ssrc = control.ssrc;
            rinfo2 = rinfo;
            Log.d("MIDI2Stream", "Data channel to " + control.name + " established");
            Log.d("MIDI2Stream", "name: "+this.name+" ssrc:"+this.ssrc);
            EventBus.getDefault().post(new StreamConnectedEvent(this.initiator_token));
//            EventBus.getDefault().post(new MIDIConnectionRequestAcceptedEvent());

            resetSyncService(1500);
        } else {
            Log.d("MIDI2Stream","unhandled invitation accept");
        }
    }

    private void resetSyncService(int time) {

        Log.d("MIDI2Stream","resetSyncService "+time);
        if(!primarySyncComplete) {
//            EventBus.getDefault().post(new MIDISyncronizationStartEvent());
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
//        connectService.shutdownNow();
//        connectFuture.cancel(true);
        connectFuture.cancel(true);
        EventBus.getDefault().post(new MIDIConnectionRequestRejectedEvent());
    }

    private void handleEnd() {
        this.isConnected = false;
        // shutdown sync
//        connectFuture.cancel(true);
//        connectService.shutdown();
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
        Log.d("MIDI2Stream","sendInvitationAccepted");
        MIDIControl message = new MIDIControl();

        message.createInvitationAccepted(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo);

//
        if(!syncStarted && rinfo2 == null) {
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
        Log.d("MIDI2Stream","sendMessage");
        this.lastSentSequenceNr = (this.lastSentSequenceNr + 1) % 0x10000;
        m.sequenceNumber = this.lastSentSequenceNr;
        MIDISession.getInstance().sendUDPMessage(m, rinfo2);
    }

    public void sendEnd() {
        Log.d("MIDI2Stream","sendEnd");
        MIDIControl message = new MIDIControl();
        message.createEnd(this.initiator_token, MIDISession.getInstance().ssrc, MIDISession.getInstance().bonjourName);
        MIDISession.getInstance().sendUDPMessage(message, rinfo1);
//        connectFuture.cancel(true);
//        connectService.shutdown();
//        syncService.shutdown();
        EventBus.getDefault().post(new StreamDisconnectEvent(ssrc));
    }


    private void sendSynchronization(MIDIControl inboundSyncMessage) {
        long now = MIDISession.getInstance().getNow();
        MIDIControl outboundSyncMessage;
        int count = (inboundSyncMessage != null) ? inboundSyncMessage.count : -1;
        if(count == 3) { count = -1; }
//        Log.d("MIDI2Stream","sendSyncronization "+count);

//        int padding = (inboundSyncMessage != null) ? inboundSyncMessage.sync_padding : 0;


        outboundSyncMessage = new MIDIControl();
        outboundSyncMessage.createSyncronization(
                MIDISession.getInstance().ssrc,
                count+1,
                (count != -1) ? inboundSyncMessage.timestamp1 : 0,
                (count != -1) ? inboundSyncMessage.timestamp2 : 0,
                (count != -1) ? inboundSyncMessage.timestamp3 : 0);

//        outboundSyncMessage.destination_ip = this.destination_ip;
//        outboundSyncMessage.destination_port = this.destination_port;

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
//        Log.d("MIDI2Stream","about to send sync");
        if(outboundSyncMessage.count < 4) {
            MIDISession.getInstance().sendUDPMessage(outboundSyncMessage, rinfo2);
            if(!primarySyncComplete && ++syncCount > 3) {
                primarySyncComplete = true;
                EventBus.getDefault().post(new SyncronizeStoppedEvent());
                EventBus.getDefault().post(new ConnectionEstablishedEvent(rinfo1));
            }
        }

    }

}
