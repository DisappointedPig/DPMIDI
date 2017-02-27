package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIConnectionEstablishedEvent {
    public final Bundle rinfo;

    public MIDIConnectionEstablishedEvent(final Bundle r) {
        rinfo = r;
    }

}
