package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIConnectionEndEvent {
    public final Bundle rinfo;

    public MIDIConnectionEndEvent(Bundle b) {
        rinfo = b;
    }

}
