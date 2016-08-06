package com.disappointedpig.midi2.events;

/**
 * Created by jay on 7/31/16.
 */

public class MIDI2StreamConnected {

    public int initiator_token;

    public MIDI2StreamConnected(int token) {
        initiator_token = token;
    }
}
