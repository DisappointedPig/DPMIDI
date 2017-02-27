package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIConnectionRequestReceivedEvent {
    public final Bundle rinfo;

    public MIDIConnectionRequestReceivedEvent(final Bundle r) {
        rinfo = r;
    }

}
