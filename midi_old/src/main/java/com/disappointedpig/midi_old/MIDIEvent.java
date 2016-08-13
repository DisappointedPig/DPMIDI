package com.disappointedpig.midi;

import java.util.Date;

public class MIDIEvent {
    public Date time;
    public MIDIMessage message;

    public MIDIEvent(MIDIMessage m) {
        message = m;
    }

}