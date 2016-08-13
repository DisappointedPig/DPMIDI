package com.disappointedpig.midi.internal_events;

import android.os.Bundle;

import java.net.DatagramPacket;
import java.net.InetAddress;

public class PacketEvent {
    private InetAddress address;
    private int port;
    private byte[] data;
    private int length;
    public PacketEvent(final DatagramPacket packet) {
        address = packet.getAddress();
        port = packet.getPort();
        data = packet.getData();
        length = packet.getLength();
//        Log.d("PacketEvent"," p:"+packet.getLength()+ " d:"+data.length);
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
