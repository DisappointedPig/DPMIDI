package com.disappointedpig.midi;

import java.util.Date;

public interface MIDIListener {
    void acceptMessage(Date time, MIDIMessage message);

    void acceptMessage(Date time, MIDIControl message);

}
