package com.disappointedpig.midi2.events;

public class MIDI2StreamDisconnectEvent {

    public int stream_ssrc;

    public MIDI2StreamDisconnectEvent(int ssrc) {
        stream_ssrc = ssrc;
    }
}
