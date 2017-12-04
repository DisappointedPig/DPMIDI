package com.disappointedpig.midi;

import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

class MIDIPort implements Runnable {
    private int port;

    private Selector selector;
    private DatagramChannel channel;

    private Queue<DatagramPacket> outboundQueue;
//    private Queue<DatagramPacket> inboundQueue;

    private boolean isListening = false;

    private static final int BUFFER_SIZE = 1536;
    private static final String TAG = "MIDIPort";
//    private static final boolean DEBUG = false;

    private final Thread thread = new Thread(this);

    static MIDIPort newUsing(int port) {
        return new MIDIPort(port);
    }

    private MIDIPort(int port) {
        this.port = port;
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            outboundQueue = new ConcurrentLinkedQueue<>();
//            inboundQueue = new ConcurrentLinkedQueue<DatagramPacket>();

            InetSocketAddress address = new InetSocketAddress(this.port);
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);
            channel.socket().bind(address);

//            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new UDPBuffer());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finalize() {
        try {
            isListening = false;
            outboundQueue.clear();
//            inboundQueue.clear();
            selector.close();
            channel.close();
            thread.interrupt();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(isListening) {
            try {
                selector.select();
                Set<SelectionKey> readyKeys = selector.selectedKeys();
                if (readyKeys.isEmpty() ) {
                    break;
                } else {
                    Iterator<SelectionKey> keyIter = readyKeys.iterator();
                    while (keyIter.hasNext()) {
                        SelectionKey key = keyIter.next();
                        keyIter.remove();
                        if(!key.isValid()) {
                            continue;
                        }

                        if (key.isReadable()) {
                            handleRead(key);
                        }
                        if (key.isWritable()) {
                            handleWrite(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    int getPort() {
        return this.port;
    }

    boolean isListening() {
        return this.isListening;
    }

    int getThreadPriority() {
        return thread.getPriority();
    }

    void setThreadPriority(int priority) {
        thread.setPriority(priority);
    }

    void start() {
        isListening = true;

//        final Thread thread = new Thread(this);
//        // The JVM exits when the only threads running are all daemon threads.
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
//        Log.d(TAG,"create thread : "+thread.getId());

    }

    void stop() {
        isListening = false;
    }

    private void handleRead(SelectionKey key) {
//        Log.d("MIDIPort2","handleRead");
        DatagramChannel c = (DatagramChannel) key.channel();
        UDPBuffer b = (UDPBuffer) key.attachment();
        try {
            b.buffer.clear();
            b.socketAddress = c.receive(b.buffer);
            EventBus.getDefault().post(new PacketEvent(new DatagramPacket(b.buffer.array(),b.buffer.capacity(),b.socketAddress)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleWrite(SelectionKey key) {
        if(!outboundQueue.isEmpty()) {
//            Log.d("MIDIPort2","handleWrite "+ outboundQueue.size());
            try {
                DatagramChannel c = (DatagramChannel) key.channel();
                DatagramPacket d = outboundQueue.poll();

                c.send(ByteBuffer.wrap(d.getData()),d.getSocketAddress());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    void sendMidi(MIDIControl control, Bundle rinfo) {
//        Log.d("MIDIPort2","sendMidi(control)");
        if (!isListening) {
            Log.d(TAG,"not listening...");
            return;
        }
        addToOutboundQueue(control.generateBuffer(),rinfo);
    }

    void sendMidi(MIDIMessage message, Bundle rinfo) {
//        Log.d("MIDIPort","sendMidi(message)");
        if (!isListening) {
            Log.d(TAG,"not listening...");
            return;
        }
        addToOutboundQueue(message.generateBuffer(),rinfo);
    }

    private void addToOutboundQueue(byte[] data, Bundle rinfo) {
        try {
            outboundQueue.add(new DatagramPacket(data, data.length, InetAddress.getByName(rinfo.getString(com.disappointedpig.midi.MIDIConstants.RINFO_ADDR)), rinfo.getInt(com.disappointedpig.midi.MIDIConstants.RINFO_PORT)));
            selector.wakeup();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    private class UDPBuffer {
        DatagramPacket datagramPacket;
        SocketAddress  socketAddress;
        ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    }
}
