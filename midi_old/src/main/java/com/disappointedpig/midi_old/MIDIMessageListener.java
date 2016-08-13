package com.disappointedpig.midi;

import java.util.Date;

public interface MIDIMessageListener {
    void acceptMessage(Date time, MIDIMessage message);
}