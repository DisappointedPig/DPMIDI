package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

public class ConnectionEstablishedEvent {
    public final Bundle rinfo;

    public ConnectionEstablishedEvent(final Bundle r) {
        rinfo = r;
    }
}
