package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

public class StreamDisconnectEvent {

    public final int stream_ssrc;
    public final int initiator_token;
    public final Bundle rinfo;

    public StreamDisconnectEvent(int ssrc) {
        stream_ssrc = ssrc;
        initiator_token = 0;
        rinfo = null;
    }
    public StreamDisconnectEvent(int ssrc, int token) {
        stream_ssrc = ssrc;
        initiator_token = token;
        rinfo = null;
    }
    public StreamDisconnectEvent(int ssrc, Bundle b) {
        stream_ssrc = ssrc;
        initiator_token = 0;
        rinfo = b;
    }
    public StreamDisconnectEvent(int ssrc, int token, Bundle b) {
        stream_ssrc = ssrc;
        initiator_token = token;
        rinfo = b;
    }

}
