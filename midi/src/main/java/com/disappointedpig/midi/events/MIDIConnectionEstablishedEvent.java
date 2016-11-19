package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIConnectionEstablishedEvent {
    private Bundle rinfo;

    public MIDIConnectionEstablishedEvent(final Bundle r) {
        rinfo = r;
    }

    public Bundle getRInfo() {
        return rinfo;
    }
}
