package com.disappointedpig.midi;

import android.os.Bundle;

import static com.disappointedpig.midi.MIDIConstants.RINFO_ADDR;
import static com.disappointedpig.midi.MIDIConstants.RINFO_NAME;
import static com.disappointedpig.midi.MIDIConstants.RINFO_PORT;
import static com.disappointedpig.midi.MIDIConstants.RINFO_RECON;

public class MIDIAddressBookEntry {

    private String address;
    private int port;
    private String name;
    private Boolean reconnect;

    public MIDIAddressBookEntry() {
    }

    public MIDIAddressBookEntry(Bundle rinfo) {
        address = rinfo.getString(RINFO_ADDR,"");
        port = rinfo.getInt(RINFO_PORT);
        name = rinfo.getString(RINFO_NAME,"");
        reconnect = rinfo.getBoolean(RINFO_RECON,false);
    }

    public Bundle rinfo() {
        Bundle rinfo = new Bundle();

        return rinfo;
    }

    public void setAddress(String a) {
        address = a;
    }

    public String getAddress() {
        return address;
    }

    public void setPort(int p) {
        port = p;
    }

    public int getPort() {
        return port;
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setReconnect(boolean b) {
        reconnect = b;
    }

    public boolean getReconnect() {
        return reconnect;
    }

}
