package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

public class SyncronizeStoppedEvent {
    public final Bundle rinfo;

    public SyncronizeStoppedEvent(final Bundle r) {
        rinfo = r;
    }

}
