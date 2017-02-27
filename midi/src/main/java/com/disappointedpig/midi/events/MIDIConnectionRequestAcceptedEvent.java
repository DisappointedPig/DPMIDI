package com.disappointedpig.midi.events;


import android.os.Bundle;

public class MIDIConnectionRequestAcceptedEvent {
    public final Bundle rinfo;

    public MIDIConnectionRequestAcceptedEvent(final Bundle r) {
        rinfo = r;
    }

}
