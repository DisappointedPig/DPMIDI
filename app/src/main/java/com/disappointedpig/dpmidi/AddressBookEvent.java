package com.disappointedpig.dpmidi;

import com.disappointedpig.midi.MIDIAddressBookEntry;

public class AddressBookEvent {
    AddressBookEventType type;
    MIDIAddressBookEntry entry;

    public AddressBookEvent(AddressBookEventType t, MIDIAddressBookEntry e) {
        type = t;
        entry = e;
    }

    public MIDIAddressBookEntry getEntry() {
        return entry;
    }

    public AddressBookEventType getType() {
        return type;
    }
}
