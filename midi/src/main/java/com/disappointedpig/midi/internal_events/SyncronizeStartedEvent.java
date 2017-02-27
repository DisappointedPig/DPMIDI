package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

/**
 * Created by jay on 11/10/16.
 */

public class SyncronizeStartedEvent {

    public final Bundle rinfo;

    public SyncronizeStartedEvent(final Bundle r) {
        rinfo = r;
    }

}
