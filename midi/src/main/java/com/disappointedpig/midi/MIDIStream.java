package com.disappointedpig.midi;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.net.InetAddress;

import de.greenrobot.event.EventBus;


public class MIDIStream {

    private int debugLevel;
    private int initiator_token;
    private int initiator_ssrc;
    private int ssrc;
    private int source_port;
    private boolean synchronization_complete;
    private int syncnumber;

    private InetAddress destination_ip;
    private int destination_port;

    private long latency;
    private long timeDifference;

    public Boolean isValidStream;

    //    private InetAddress source_ip;
    public MIDIStream() {
        ssrc = MIDISession.getInstance().getSSRC();
        debugLevel = MIDISession.getInstance().getDebugLevel();
        latency = 0L;
        timeDifference = 0L;
        this.isValidStream = false;
        this.synchronization_complete = false;
        syncnumber = 0;
    };

    public MIDIStream(int it, int s, int port, int src) {
        this.isValidStream = false;
        this.synchronization_complete = false;
        initiator_token = it;
        initiator_ssrc = s;
        source_port = port;
        ssrc = src;
        debugLevel = MIDISession.getInstance().getDebugLevel();
        latency = 0L;
        timeDifference = 0L;
    }

    //    public MIDIStream(MIDIMessage message, int port, int src) {
    public MIDIStream(MIDIControl message) {
        this.isValidStream = false;
        this.synchronization_complete = false;

        if(true) {
            switch(message.getCommand()) {
                case 0x494E:// invite
                case 0x4F4B:// invite_accept
                    this.initiator_token = message.getInitiatorToken();
                    this.initiator_ssrc = message.getSenderSSRC();
                    this.destination_ip = message.source_ip;
                    this.destination_port = message.source_port;

                    this.source_port = message.destination_port;
                    this.ssrc = MIDISession.getInstance().getSSRC();
                    this.debugLevel = MIDISession.getInstance().getDebugLevel();
                    this.latency = 0L;
                    this.timeDifference = 0L;
                    syncnumber = 0;
                    this.isValidStream = true;
                    if(this.debugLevel > 0) {
                        EventBus.getDefault().post(new MIDIDebugEvent("MIDIStream", "source port:" + this.source_port + ":" + String.format("%02x", this.ssrc) + " " + this.destination_ip + ":" + this.destination_port + ":" + String.format("%02x", this.initiator_token) + ":" + String.format("%02x", this.initiator_ssrc)));
                    }
//                    Log.d("MIDIStream", "source port:" + this.source_port + ":" + String.format("%02x", this.ssrc) + " " + this.destination_ip + ":" + this.destination_port + ":" + String.format("%02x", this.initiator_token) + ":" + String.format("%02x", this.initiator_ssrc));
                    break;
            }

        }
    }


    Handler handler = new Handler(Looper.getMainLooper());


    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            // Do something here on the main thread
//            sendSynchronization(null);
            new SendSyncronizationTask().execute("testing");
            handler.postDelayed(runnableCode, 10000);

        }
    };


    public boolean handleMessage(MIDIControl message) {
        MIDIControl response;
        //long now = MIDISession.getInstance().getNow();
        switch(message.getCommand()) {
            case 0x494E:
                // received invitation, need to respond
                if(this.debugLevel > 0) {
                    EventBus.getDefault().post(new MIDIDebugEvent("MIDIStream", "received invite"));
                }
                initiator_ssrc = message.getSenderSSRC();
                initiator_token = message.getInitiatorToken();
                this.destination_ip = message.source_ip;
                this.destination_port = message.source_port;
                response = new MIDIControl(0x4F4B, this.initiator_token, MIDISession.getInstance().getSSRC(), MIDISession.getInstance().getName());
//                response.protocol = 0xFFFF;
//                response.protocol_version = 2;
//                response.command = 0x4F4B;
//                response.initiator_token = initiator_token;
//                response.ssrc = ssrc;
//                response.name = MIDISession.getInstance().getName();
                response.destination_port = destination_port;
                response.destination_ip = destination_ip;

                MIDISession.getInstance().sendMIDI(response);
                break;
            case 0x434B:
                // received sync, respond in kind
                sendSynchronization(message);
                break;
            case 0x4259:
                if(this.debugLevel > 0) {
                    EventBus.getDefault().post(new MIDIDebugEvent("MIDIStream","end session"));
                }
//                Log.d("MIDIStream", "end session");
                shutdownStream();
                break;
        }
        return true;
    }

    public void shutdownStream() {
        handler.removeCallbacks(runnableCode);
//        MIDISession.getInstance().closeStream(this.ssrc);
    }

    public MIDIControl getEndMessage() {
        MIDIControl outboundEndMessage = new MIDIControl(0x4259,this.initiator_token,MIDISession.getInstance().getSSRC(),MIDISession.getInstance().getName());
        outboundEndMessage.destination_ip = this.destination_ip;
        outboundEndMessage.destination_port = this.destination_port;

        return outboundEndMessage;
    }

    public void sendSynchronization(MIDIControl inboundSyncMessage) {

        long now = MIDISession.getInstance().getNow();
        MIDIControl outboundSyncMessage;
        int count = (inboundSyncMessage != null) ? inboundSyncMessage.sync_count : -1;
        if(count == 3) { count = -1; }

//        int padding = (inboundSyncMessage != null) ? inboundSyncMessage.sync_padding : 0;


        outboundSyncMessage = new MIDIControl(  MIDISession.getInstance().getSSRC(),
                count+1,
                (count != -1) ? inboundSyncMessage.sync_timestamp1 : 0,
                (count != -1) ? inboundSyncMessage.sync_timestamp2 : 0,
                (count != -1) ? inboundSyncMessage.sync_timestamp3 : 0
        );

        outboundSyncMessage.destination_ip = this.destination_ip;
        outboundSyncMessage.destination_port = this.destination_port;

        switch(count) {
            case -1: // send count:0
                outboundSyncMessage.sync_timestamp1 = now;
                outboundSyncMessage.sync_timestamp2 = (timeDifference != 0L) ? now - timeDifference : 0L;
                outboundSyncMessage.sync_timestamp3 = (timeDifference != 0L) ? now + timeDifference : 0L;
                break;
            case 0: // received count:0, respond count:1
                outboundSyncMessage.sync_timestamp2 = now;
                outboundSyncMessage.sync_timestamp3 = now - timeDifference;
                break;
            case 1: // received count:1, respond count:2
//                outboundSyncMessage.sync_timestamp3 = now;
//                latency = inboundSyncMessage.sync_timestamp3 - inboundSyncMessage.sync_timestamp1;
//                timeDifference = Math.round(inboundSyncMessage.sync_timestamp3-inboundSyncMessage.sync_timestamp2) - latency;

                timeDifference = (inboundSyncMessage.sync_timestamp3 - inboundSyncMessage.sync_timestamp1) / 2;
                timeDifference = inboundSyncMessage.sync_timestamp3 + timeDifference - now;
                outboundSyncMessage.sync_timestamp3 = now;
                break;
            case 2:  // received count:2, sending nothing
                     /* compute media delay */
//                diff = ( command->data.sync.timestamp3 - command->data.sync.timestamp1 ) / 2;
                      /* approximate time difference between peer and self */
//                diff = command->data.sync.timestamp3 + diff - timestamp;
                timeDifference = (inboundSyncMessage.sync_timestamp3 - inboundSyncMessage.sync_timestamp1) / 2;
                timeDifference = inboundSyncMessage.sync_timestamp3 + timeDifference - now;

                if(!synchronization_complete) { ++syncnumber;
                    if(syncnumber >=3) {
                        synchronization_complete = true;
                        if(this.debugLevel > 0) {
                            EventBus.getDefault().post(new MIDIDebugEvent("MIDIStream","Syncroization complete"));
                        }

                        // do periodic sync
                        if(isValidStream) {
                            new SendSyncronizationTask().execute("testing");
                            handler.postDelayed(runnableCode, 10000); // every 10 seconds
                        }
                    }
                }

//                Log.d("MIDIStream","SyncNumber: "+syncnumber);
                return;
        }
//        Log.d("MIDIStream", "sendSync " + outboundSyncMessage.sync_count + " now: " + String.format("%02x", now) + " timeDifference:" + timeDifference);

        MIDISession.getInstance().sendMIDI(outboundSyncMessage);
    }


    private class SendSyncronizationTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {

            try {
                sendSynchronization(null);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
                ex.printStackTrace();
            }


            return null;
        }
    }


}
