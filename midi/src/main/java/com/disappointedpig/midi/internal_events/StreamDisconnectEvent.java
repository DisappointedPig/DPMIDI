package com.disappointedpig.midi.internal_events;

public class StreamDisconnectEvent {

    public int stream_ssrc = 0;
    public int initiator_token = 0;
    public StreamDisconnectEvent(int ssrc) {
        stream_ssrc = ssrc;
    }
    public StreamDisconnectEvent(int ssrc, int token) {
        stream_ssrc = ssrc;
        initiator_token = token;
    }

}
