package com.disappointedpig.midi;

import java.net.DatagramSocket;

public class Port {
    private final DatagramSocket socket;
    private final int port;

    protected Port(DatagramSocket socket, int port) {
        this.socket = socket;
        this.port = port;
    }

    protected DatagramSocket getSocket() { return socket; }

    protected int getPort() {
        return port;
    }

    public void close() {
        socket.close();
    }

}