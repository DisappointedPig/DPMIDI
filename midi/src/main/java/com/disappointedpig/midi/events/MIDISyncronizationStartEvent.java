package com.disappointedpig.midi.events;


import android.os.Bundle;

public class MIDISyncronizationStartEvent {

    public final Bundle rinfo;

    public MIDISyncronizationStartEvent(final Bundle r) {
        rinfo = r;
    }
}
