package com.disappointedpig.midi.events;

import android.os.Bundle;

public class MIDIReceivedEvent {

    public Bundle midi;

    public MIDIReceivedEvent(Bundle m) {
        this.midi = m;
    }
}
