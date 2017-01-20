package com.disappointedpig.midi.internal_events;


public class StreamConnectedEvent {

    public int initiator_token;

    public StreamConnectedEvent(int token) {
        initiator_token = token;
    }
}
