package com.disappointedpig.midi.internal_events;

/**
 * Created by jay on 7/31/16.
 */

public class StreamConnectedEvent {

    public int initiator_token;

    public StreamConnectedEvent(int token) {
        initiator_token = token;
    }
}
