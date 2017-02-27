package com.disappointedpig.midi;

public enum MIDIState {
    NOOP,
    CONNECTING,
    CONNECTED,
    SYNCHRONIZING,
    SYNCHRONIZED,
    ACCEPTED,
    REJECTED,
    FAILED
}
