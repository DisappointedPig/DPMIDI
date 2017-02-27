package com.disappointedpig.midi.internal_events;


import android.os.Bundle;

import com.disappointedpig.midi.MIDIFailCode;

public class ConnectionFailedEvent {
    public final Bundle rinfo;
    public final MIDIFailCode code;
    public final int initiator_code;

    public ConnectionFailedEvent(final MIDIFailCode c, final Bundle r) {
        code = c;
        rinfo = r;
        initiator_code = 0;
    }

    public ConnectionFailedEvent(final MIDIFailCode c, final Bundle r, final int i) {
        code = c;
        rinfo = r;
        initiator_code = i;
    }
}
