package com.disappointedpig.midi2;

import android.os.Bundle;
import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MIDI2Stream {

    MIDI2Session session;
    int token = 0;
    int ssrc = 0;
    Bundle rinfo1 = null;
    Bundle rinfo2 = null;
    String name = "";
    long lastSentSequenceNr =  Math.round(Math.random() * 0xffff);
    long firstReceivedSequenceNumber = -1L;
    long lastReceivedSequenceNumber = -1L;
    long latency = 0L;
    boolean isConnected = false;
    long receiverFeedbackTimeout = 0L;
    long lastMessageTime = 0L;
    long timeDifference = 0L;
    boolean isInitiator = false;

//    long ssrc;

    int connectCounter;

    public MIDI2Stream(MIDI2Session s) {
        this.session = session;
    }

    public void connect(final Bundle rinfo) {
        this.isConnected = true;
        connectCounter = 0;

//        ScheduledExecutorService scheduler =
//                Executors.newSingleThreadScheduledExecutor();
//
//        scheduler.scheduleAtFixedRate
//                (new Runnable() {
//                    public void run() {
//                        // call service
//                    }
//                }, 0, 10, TimeUnit.MINUTES);

        final ScheduledExecutorService connectService = Executors.newSingleThreadScheduledExecutor();

        connectService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                if(connectCounter < 40 && ssrc == 0) {
                    sendInvitation(rinfo);
                    connectCounter++;
                } else {
                    connectService.shutdown();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);


    }

    public void sendInvitation(Bundle rinfo) {
        if (this.token == 0) {
            this.token = generateRandomInteger(4);
            MIDI2Control invite = new MIDI2Control();
            invite.createInvitation(token,session.ssrc,session.bonjourName);
            this.session.sendControlMessage(invite,rinfo);
        }
    }

    public int generateRandomInteger(int octets) {
        return (int) Math.round(Math.random() * Math.pow(2, 8 * octets));
    }

    public void handleControlMessage(MIDI2Control control, Bundle rinfo) {

    }

    public void handleInvitation(MIDI2Control control, Bundle rinfo) {
        if(rinfo1 == null) {
            rinfo1 = rinfo;
            this.token = control.initiator_token;
            this.name = control.name;
            this.ssrc = control.ssrc;
            Log.d("MIDI2Stream", "Got an invitation from " + control.name + " on channel 1");
        } else if(rinfo2 == null) {
            rinfo2 = rinfo;
            Log.d("MIDI2Stream", "Got an invitation from " + control.name + " on channel 2");
            this.isConnected = true;
        }
        this.sendInvitationAccepted(rinfo);
    }

    public void sendInvitationAccepted(Bundle rinfo) {

        MIDI2Control message = new MIDI2Control();
        message.createInvitationAccepted(this.token,this.session.ssrc,this.name);
        this.session.sendControlMessage(message, rinfo);

    }
}
