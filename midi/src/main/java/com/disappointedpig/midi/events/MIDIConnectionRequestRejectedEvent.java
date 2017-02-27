package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIConnectionRequestRejectedEvent {
    public final Bundle rinfo;

    public MIDIConnectionRequestRejectedEvent(final Bundle r) {
        rinfo = r;
    }

}
