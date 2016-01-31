package com.disappointedpig.midi.utility;

import android.util.Log;

import com.disappointedpig.midi.MIDIControl;
import com.disappointedpig.midi.MIDIControlListener;
import com.disappointedpig.midi.MIDIMessage;
import com.disappointedpig.midi.MIDIMessageListener;
import com.disappointedpig.midi.MIDIPacket;

import java.util.ArrayList;
import java.util.Date;

public class MIDIPacketDispatcher {
//    private final Map<String, MIDIListener> selectorToListener;

    private final ArrayList<MIDIControlListener> controlListenerList;
    private final ArrayList<MIDIMessageListener> messageListenerList;

    public MIDIPacketDispatcher() {
//        this.selectorToListener = new HashMap<String, MIDIListener>();
        this.controlListenerList = new ArrayList<MIDIControlListener>();
        this.messageListenerList = new ArrayList<MIDIMessageListener>();

    }

//    public void addListener(String addressSelector, MIDIListener listener) {
//        selectorToListener.put(addressSelector, listener);
//    }

    public void addListener(MIDIControlListener listener) {
//        Log.d("MIDIPacketDispatcher","add listener");
        controlListenerList.add(listener);
    }

    public void addListener(MIDIMessageListener listener) {
        messageListenerList.add(listener);
    }

    public void dispatchPacket(MIDIPacket packet) {
        dispatchPacket(packet, null);
    }

    public void dispatchPacket(MIDIPacket packet, Date timestamp) {
        if (packet.getClass().equals(MIDIMessage.class)) {
            dispatchMessage((MIDIMessage) packet, timestamp);
        } else {
            dispatchMessage((MIDIControl) packet, timestamp);
        }

    }

    private void dispatchMessage(MIDIMessage message, Date time) {
//        for (final Map.Entry<String, MIDIListener> addrList : selectorToListener.entrySet()) {
//            if (addrList.getKey().matches("testing")) {
//                addrList.getValue().acceptMessage(time, message);
//            }
//        }
//        Log.d("MIDIPacketDispatcher", "dispatchMessage ("+listenerList.size()+")");
//        switch(message.protocol) {
//            case 0xFFFF:
//                for (MIDIControlListener listener: controlListenerList) {
//                    listener.acceptMessage(time, message);
//                }
//                break;
//            default:
//        Log.d("packetdispatch", "midimessage packet  list:" + messageListenerList.size());
        for (MIDIMessageListener listener : messageListenerList) {
            listener.acceptMessage(time, message);
        }

//        }
    }

    private void dispatchMessage(MIDIControl message, Date time) {
//        Log.d("packetdispatch", "midicontrol packet " + message.getCommand() + " list:" + controlListenerList.size());
        for (MIDIControlListener listener : controlListenerList) {
            listener.acceptMessage(time, message);
        }

    }
}