package com.disappointedpig.midi2.events;

import android.os.Bundle;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class MIDI2PacketEvent {
    private InetAddress address;
    private int port;
    private byte[] data;
    private int length;
    public MIDI2PacketEvent(final DatagramPacket packet) {
        address = packet.getAddress();
        port = packet.getPort();
        data = packet.getData();
        length = packet.getLength();
        Log.d("MIDI2PacketEvent"," p:"+packet.getLength()+ " d:"+data.length);
    }

    public Bundle getRInfo() {
        Bundle rinfo = new Bundle();
        rinfo.putString("address",address.getHostAddress());
        rinfo.putInt("port",port);
        return rinfo;
    }

    public InetAddress getAddress() {
        return address;
    }

    public int getPort() {
        return port;
    }

    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }
}
