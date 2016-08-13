package com.disappointedpig.midi;

import java.io.IOException;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;

public class Port {
    public final DatagramSocket socket;
    public int port;

//    protected Port(DatagramSocket socket, int port) {
//        this.socket = socket;
//        this.port = port;
//    }
    protected Port() {
        DatagramSocket socket1;
        try {
//            DatagramChannel channel = DatagramChannel.open();
//            DatagramSocket socket = channel.socket();
            DatagramChannel channel = DatagramChannel.open();
            socket1 = channel.socket();
//            socket1.setBroadcast(true);
            socket1.setReuseAddress(true);
        } catch (IOException e) {
            socket1 = null;
            e.printStackTrace();
        }

        this.socket = socket1;
        this.port = 0;
    }

    protected DatagramSocket getSocket() { return socket; }

    protected int getPort() {
        return port;
    }

    public void close() {
        socket.close();
    }

}