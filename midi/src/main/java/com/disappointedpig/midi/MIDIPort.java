package com.disappointedpig.midi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MIDIPort implements Runnable {
    int port;

    Selector selector;
    DatagramChannel channel;

    private Queue<DatagramPacket> outboundQueue;
    private Queue<DatagramPacket> inboundQueue;

    boolean isListening = false;

    private static final int BUFFER_SIZE = 1536;
    private static final String TAG = "MIDIPort";
    private static final boolean DEBUG = false;


    public static MIDIPort newUsing(int port) {
        return new MIDIPort(port);
    }

    public MIDIPort(int port) {
        this.port = port;
        try {
            selector = Selector.open();
            channel = DatagramChannel.open();
            outboundQueue = new ConcurrentLinkedQueue<DatagramPacket>();
            inboundQueue = new ConcurrentLinkedQueue<DatagramPacket>();

            InetSocketAddress address = new InetSocketAddress(this.port);
            channel.socket().setReuseAddress(true);
            channel.configureBlocking(false);
            channel.socket().bind(address);

//            channel.socket().setSoTimeout(700);
//            int ops = channel.validOps();

//            SelectionKey selectKy = channel.register(selector, ops, new UDPBuffer()); // null for an attachment object
//            SelectionKey selectKy = channel.register(selector, (SelectionKey.OP_READ), new UDPBuffer());
            channel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Override
    public void run() {
        while(isListening) {
            try {
//                int noOfKeys = selector.select();
//                Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();
//                while (keyIter.hasNext()) {
//
//                    SelectionKey key = keyIter.next(); // Key is bit mask
//
//                    // Client socket channel has pending data?
//                    if (key.isReadable()) {
//                        handleRead(key);
//                    }
//                    // Client socket channel is available for writing and
//                    // key is valid (i.e., channel not closed).
////                    if (key.isValid() && key.isWritable()) {
//                    if (key.isWritable()) {
//                            handleWrite(key);
////                            key.interestOps(SelectionKey.OP_READ);
//                    }
//                    keyIter.remove();
//                }

                    selector.select();
                    Set readyKeys = selector.selectedKeys();
                    if (readyKeys.isEmpty() ) { //&& n == LIMIT) {
                        // All packets have been written and it doesn't look like any
                        // more are will arrive from the network
                        Log.d(TAG,"no readyKeys");
                        break;
                    } else {
                        Iterator<SelectionKey> keyIter = readyKeys.iterator();
                        while (keyIter.hasNext()) {
                            SelectionKey key = (SelectionKey) keyIter.next();
                            keyIter.remove();
                            if(!key.isValid()) {
                                continue;
                            }

                            if (key.isReadable()) {
                                handleRead(key);
//                                Log.d(TAG," read "+this.port);
                            }
                            if (key.isWritable()) {
                                handleWrite(key);
//                                Log.d(TAG," write "+this.port);
                            }
//                            Log.d(TAG,"key "+key.toString());
                        }
                    }
//                    Log.d(TAG,"loop");


            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        isListening = true;
        final Thread thread = new Thread(this);
        // The JVM exits when the only threads running are all daemon threads.
        thread.setDaemon(true);
        thread.setPriority(Thread.NORM_PRIORITY );
        thread.start();
    }

    public void stop() {
        isListening = false;

    }

    public void finalize() {
        try {
            selector.close();
            channel.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void handleRead(SelectionKey key) {
//        Log.d("MIDIPort2","handleRead");
//        final byte[] buffer = new byte[BUFFER_SIZE];
//        final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
        DatagramChannel c = (DatagramChannel) key.channel();
        UDPBuffer buf = (UDPBuffer) key.attachment();
        try {
//            channel.socket().receive(packet);
            UDPBuffer b = new UDPBuffer();
            b.buffer.clear();
            b.clientAddress = c.receive(b.buffer);
            DatagramPacket a = new DatagramPacket(b.buffer.array(),b.buffer.capacity(),b.clientAddress);
            EventBus.getDefault().post(new PacketEvent(a));

        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (buf.clientAddress != null) {  // Did we receive something?
//            // Register write with the selector
//            key.interestOps(SelectionKey.OP_WRITE);
//        }


    }

    private class sendPacketImmediatelyTask extends AsyncTask<Object, Void, Void> {
        @Override
        protected Void doInBackground(Object... params) {

            try {
                sendPacketImmediately((DatagramPacket) params[0]);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
//                Log.d("MIDIPort","SendMidiMessageTask");

                ex.printStackTrace();
            }

            return null;
        }
    }

    public void sendPacketImmediately(DatagramPacket packet) {
        try {
//            Log.d("MIDIPort2","sendPacketImmediately");
            channel.send(ByteBuffer.wrap(packet.getData()), packet.getSocketAddress());
        } catch (SocketException e) {
            Log.e("MIDIPort","socket exception - network is unreachable");
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void handleWrite(SelectionKey key) {
        if(!outboundQueue.isEmpty()) {
            Log.d("MIDIPort2","handleWrite "+ outboundQueue.size());
            try {
//                UDPBuffer buf = (UDPBuffer) key.attachment();
                DatagramChannel c = (DatagramChannel) key.channel();
//                c.socket().send(outboundQueue.poll());

//                Log.d("MIDIPort2","start buf size is "+buf.buffer.remaining());
                DatagramPacket d = outboundQueue.poll();
//                ByteBuffer byteBuffer = ByteBuffer.allocate(d.getData().length);
                byte[] r = d.getData();
                String output = "";
                for(byte a:r) {
                    output += (String.format("%02x", a) + " ");
                }
                Log.d("MIDIPort2","output "+output);
//                buf.buffer.put(r);
//                Log.d("MIDIPort2","data "+d.getData());
                Log.d("MIDIPort2","socketaddress "+d.getSocketAddress());

                int bytesSent = c.send(ByteBuffer.wrap(r),d.getSocketAddress());
//                if (bytesSent != 0) { // Buffer completely written?
//                    // No longer interested in writes
//                    key.interestOps(SelectionKey.OP_READ);
//                }
                Log.d("MIDIPort2","bytesent "+bytesSent);

//                channel.socket().send();
//                key.interestOps(SelectionKey.OP_READ);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void sendMidi(MIDIControl control, Bundle rinfo) {
//        Log.d("MIDIPort2","sendMidi(control)");
        if (!isListening) {
            Log.d(TAG,"not listening...");
            return;
        }
        try {
            byte[] r = control.generateBuffer();
            InetAddress destination_address = InetAddress.getByName(rinfo.getString("address"));
            int destination_port = rinfo.getInt("port");
            if(DEBUG) {
                Log.d(TAG, "control "+this.port+" -> " + destination_address + ":" + destination_port);
            }
            DatagramPacket response = new DatagramPacket(r, r.length, destination_address, destination_port);

            outboundQueue.add(response);
            selector.wakeup();
//            new sendPacketImmediatelyTask().execute(response);
//            Log.d("MIDIPort2","queued control response");

        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void sendMidi(MIDIMessage message, Bundle rinfo) {
//        Log.d("MIDIPort","sendMidi(message)");
        if (!isListening) {
            return;
        }
        try {
            byte[] r = message.generateBuffer();
            InetAddress destination_address = InetAddress.getByName(rinfo.getString("address"));
            int destination_port = rinfo.getInt("port");
            DatagramPacket response = new DatagramPacket(r, r.length, destination_address, destination_port);
//            outboundQueue.add(response);
//            channel.socket().send(response); // fails with IBME
//            new sendPacketImmediatelyTask().execute(response);
            if(DEBUG) {
                Log.d(TAG, "message "+this.port+" -> " + destination_address + ":" + destination_port);
            }
            outboundQueue.add(response);
            selector.wakeup();
        } catch (UnknownHostException e) {
            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
        }
    }


    static class UDPBuffer {
        public SocketAddress clientAddress;
        public ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    }

}
