package com.disappointedpig.midi.events;

import android.os.Bundle;


public class MIDIConnectionSentRequestEvent {

    public final Bundle rinfo;

    public MIDIConnectionSentRequestEvent(final Bundle r) {
        rinfo = r;
    }
}
