package com.disappointedpig.midi;

import java.util.Date;

public class MIDIDebugEvent {
    public long unixtime;
    public String module;
    public String message;

    public MIDIDebugEvent(String m) {
        message = m;
        module = "";
        unixtime = System.currentTimeMillis() / 1000L;
    }

    public MIDIDebugEvent(String mod, String m) {
        message = m;
        module = mod;
        unixtime = System.currentTimeMillis() / 1000L;
    }

}
