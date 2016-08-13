package com.disappointedpig.midi.internal_events;

public class StreamDisconnectEvent {

    public int stream_ssrc;

    public StreamDisconnectEvent(int ssrc) {
        stream_ssrc = ssrc;
    }
}
