package com.disappointedpig.midi;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

public class MIDIPort implements Runnable {
    public final DatagramChannel channel;
    public final DatagramSocket socket;
    public InetAddress address;
    public int port;
    private static final int BUFFER_SIZE = 1536;
    private boolean listening;


    protected MIDIPort() {
        DatagramChannel channel1;
        DatagramSocket socket1;


        try {
            channel1 = DatagramChannel.open();
            socket1 = channel1.socket();
//            socket1.setBroadcast(true);
//            socket1.setReceiveBufferSize(BUFFER_SIZE);
            socket1.setReuseAddress(true);
            socket1.setSoTimeout(1100);
        } catch (IOException e) {

            socket1 = null;
            channel1 = null;
            e.printStackTrace();
        }
        this.channel = channel1;
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

    public Boolean bind(InetAddress address, int port) {
        this.address = address;
        this.port = port;
        try {
//            InetSocketAddress a = new InetSocketAddress(address, port);
            InetSocketAddress a = new InetSocketAddress(port);
            socket.bind(a);
//            Log.d("MIDIPort","midiport address "+a.getAddress().toString());
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public Boolean bind(int port) {
        this.port = port;
        try {
//            Log.d("inport","socket bound? "+(socket.isBound() ? "YES" : "NO"));
//            Log.d("inport","socket closed? "+(socket.isClosed() ? "YES" : "NO"));
            InetSocketAddress a = new InetSocketAddress(port);
//            Log.d("inport","about to bind to socket - "+a.toString());
            socket.bind(a);

//            Log.d("portIn","socket is bound "+(socket.isBound() ? "YES" : "NO"));

//            Log.d("MIDIPort","port address "+a.getAddress().toString());
        } catch (SocketException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public void start() {
        listening = true;
        final Thread thread = new Thread(this);
        // The JVM exits when the only threads running are all daemon threads.
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        listening = false;
    }

    @Override
    public void run() {
        final byte[] buffer = new byte[BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
        final DatagramSocket socket = getSocket();
        while (listening) {
            try {
                try {
//                    Log.d("MIDIPort","received packet on "+this.port);
                    socket.receive(packet);
                } catch (SocketException ex) {
                    if (listening) {
                        throw ex;
                    } else {
                        // if we closed the socket while receiving data,
                        // the exception is expected/normal, so we hide it
                        continue;
                    }
                } catch (SocketTimeoutException e) {
                    continue;
                }



//                Log.d("MIDI2Port","packet from "+packet.getAddress()+":"+packet.getPort());
                EventBus.getDefault().post(new PacketEvent(packet));

//
//                String input = "";
//                for(byte a:packet.getData()) {
//                    input += (String.format("%02x", a) + " ");
//                }
//                Log.e("MIDIPort:in ",input);
//                long a = System.nanoTime();
//                final MIDIPacket midiPacket = converter.convert(packet);
//
//                long b = System.nanoTime();
//                Log.e("MIDIPort", "packet processing time: " + (b - a));

//                final MIDIPacket midiPacket = converter.convert(buffer, packet.getLength(), packet.getAddress(), packet.getPort());
//                dispatcher.dispatchPacket(midiPacket);
            } catch (IOException ex) {
                ex.printStackTrace(); // XXX This may not be a good idea, as this could easily lead to a never ending series of exceptions thrown (due to the non-exited while loop), and because the user of the lib may want to handle this case himself
            }
        }
    }

    public void sendMidi(MIDIControl control, Bundle rinfo) {
//        Log.d("MIDIPort","sendMidi(control)");
        new SendMidiControlTask().execute(control, rinfo);
    }

    public void sendMidi(MIDIMessage message, Bundle rinfo) {
//        Log.d("MIDIPort","sendMidi(message)");
        new SendMidiMessageTask().execute(message, rinfo);
    }

    private class SendMidiControlTask extends AsyncTask<Object, Bundle, Void> {
        @Override
        protected Void doInBackground(Object... params) {
//            if(android.os.Debug.isDebuggerConnected())
//                android.os.Debug.waitForDebugger();

            try {
//                Log.d("MIDIPort","ControlTask sendMidi");

                doSendMIDI((MIDIControl)params[0],(Bundle)params[1]);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
//                Log.d("MIDIPort","SendMidiControlTask");
                ex.printStackTrace();
            }

            return null;
        }
    }

    private class SendMidiMessageTask extends AsyncTask<Object, Bundle, Void> {
        @Override
        protected Void doInBackground(Object... params) {

            try {
                doSendMIDI((MIDIMessage) params[0],(Bundle)params[1]);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
//                Log.d("MIDIPort","SendMidiMessageTask");

                ex.printStackTrace();
            }

            return null;
        }
    }

    public void doSendMIDI(MIDIControl message, Bundle rInfo) {
        if (!listening){
//            Log.d("MIDIPort","not listening, return without sending");
            return;
        }
        byte[] r = message.generateBuffer();

        String output = "";
        for(byte a:r) {
            output += (String.format("%02x", a) + " ");
        }
//        Log.e("MIDIPort:out",output);
        try {
            InetAddress destination_address = InetAddress.getByName(rInfo.getString("address"));
            int destination_port = rInfo.getInt("port");
            DatagramPacket response = new DatagramPacket(r, r.length, destination_address, destination_port);
//            Log.d("MIDIPort:out","response "+response.toString());

//            DatagramSocket socket = getSocket();
            DatagramSocket socket = channel.socket();
//            DatagramSocket socket = new DatagramSocket(this.port);

            if (socket != null) {
//                Log.d("MIDIPort:out","before socket.send");
                socket.send(response);
//                Log.d("MIDIPort:out","after socket.send");
            } else {
//                Log.d("MIDIPort","socket is null");
            }
        } catch (IOException e) {
//            Log.d("MIDIPort","doSendMIDI");

            e.printStackTrace();
        }

//        try {
//            DatagramSocket socket = getSocket();
//            if (socket != null) {
//                socket.send(response);
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    public void doSendMIDI(MIDIMessage message, Bundle rInfo) {
//        Log.d("MIDIPort","doSendMIDI");
        if (!listening){
//            Log.d("MIDIPort","not listening, return without sending");
            return;
        }

        byte[] r = message.generateBuffer();

        try {
            InetAddress destination_address = InetAddress.getByName(rInfo.getString("address"));
            int destination_port = rInfo.getInt("port");
            DatagramPacket response = new DatagramPacket(r, r.length, destination_address, destination_port);
            DatagramSocket socket = getSocket();
//            DatagramSocket socket = new DatagramSocket(this.port);
            if (socket != null) {
                socket.send(response);
            }
        } catch (IOException e) {
//            Log.d("MIDIPort","doSendMIDI");
            e.printStackTrace();
        }
    }
}
