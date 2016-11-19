package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

public class ConnectionEstablishedEvent {
    private Bundle rinfo;

    public ConnectionEstablishedEvent(final Bundle r) {
        rinfo = r;
    }

    public Bundle getRInfo() {
        return rinfo;
    }
}
