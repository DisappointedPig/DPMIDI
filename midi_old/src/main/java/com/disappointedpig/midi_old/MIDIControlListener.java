package com.disappointedpig.midi;

import java.util.Date;

public interface MIDIControlListener {
    void acceptMessage(Date time, MIDIControl message);
}

