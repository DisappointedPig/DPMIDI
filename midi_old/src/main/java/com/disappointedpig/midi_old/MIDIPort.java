package com.disappointedpig.midi;

import android.os.AsyncTask;
import android.util.Log;

import com.disappointedpig.midi.utility.MIDIByteArrayToJavaConverter;
import com.disappointedpig.midi.utility.MIDIPacketDispatcher;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;

public class MIDIPort extends Port implements Runnable {

    private static final int BUFFER_SIZE = 1536;

    private boolean listening;

    private final MIDIByteArrayToJavaConverter converter;
    private final MIDIPacketDispatcher dispatcher;

    public MIDIPort() {
        super();
        this.converter = new MIDIByteArrayToJavaConverter();
        this.dispatcher = new MIDIPacketDispatcher();
    }

//    public MIDIPort(DatagramSocket socket) {
////        super(socket, socket.getLocalPort());
//        super();
//
//        this.converter = new MIDIByteArrayToJavaConverter();
//        this.dispatcher = new MIDIPacketDispatcher();
//    }

    public void initMIDIPort(int port) throws SocketException {
//        this(new DatagramSocket(port));
        this.port = port;
        try {
            Log.d("inport","socket bound? "+(socket.isBound() ? "YES" : "NO"));
            Log.d("inport","socket closed? "+(socket.isClosed() ? "YES" : "NO"));
            InetSocketAddress a = new InetSocketAddress(port);
            Log.d("inport","about to bind to socket - "+a.toString());
            socket.bind(a);
            Log.d("portIn","socket is bound "+(socket.isBound() ? "YES" : "NO"));
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

//    public MIDIPort(int port, Charset charset) throws SocketException {
//        this(port);
//        this.converter.setCharset(charset);
//    }


    /**
     * Run the loop that listens for OSC on a socket until
     * {@link #isListening()} becomes false.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        final byte[] buffer = new byte[BUFFER_SIZE];
        final DatagramPacket packet = new DatagramPacket(buffer, BUFFER_SIZE);
        final DatagramSocket socket = getSocket();
        while (listening) {
            try {
                try {
                    socket.receive(packet);
                } catch (SocketException ex) {
                    if (listening) {
                        throw ex;
                    } else {
                        // if we closed the socket while receiving data,
                        // the exception is expected/normal, so we hide it
                        continue;
                    }
                }
//                Log.d("MIDIPort","packet from "+packet.getAddress()+":"+packet.getPort());
                //
//                String input = "";
//                for(byte a:packet.getData()) {
//                    input += (String.format("%02x", a) + " ");
//                }
//                Log.e("MIDIPort:in ",input);
                long a = System.nanoTime();
                final MIDIPacket midiPacket = converter.convert(packet);
                long b = System.nanoTime();
//                Log.e("MIDIPort", "packet processing time: " + (b - a));

//                final MIDIPacket midiPacket = converter.convert(buffer, packet.getLength(), packet.getAddress(), packet.getPort());
                dispatcher.dispatchPacket(midiPacket);
            } catch (IOException ex) {
                ex.printStackTrace(); // XXX This may not be a good idea, as this could easily lead to a never ending series of exceptions thrown (due to the non-exited while loop), and because the user of the lib may want to handle this case himself
            }
        }
    }

    public void startListening() {
        listening = true;
        final Thread thread = new Thread(this);
        // The JVM exits when the only threads running are all daemon threads.
        thread.setDaemon(true);
        thread.start();
    }

    public void stopListening() {
        listening = false;
    }

    public void destroyPort() {
    }

    public boolean isListening() {
        return listening;
    }


    public void addListener(MIDIControlListener listener) {
        dispatcher.addListener(listener);
    }

    public void addListener(MIDIMessageListener listener) {
        dispatcher.addListener(listener);
    }

    public void sendMIDI(MIDIMessage message, Boolean async) {
        if (async) {
            new SendMidiMessageTask().execute(message);
        } else {
            doSendMIDI(message);
        }
    }

    public void sendMIDI(MIDIControl message) {
        new SendMidiControllTask().execute(message);
    }


    private void doSendMIDI(MIDIMessage message) {
        if (!isListening()) return;

        byte[] r = message.getByteArray();
        String output = "";
        for (byte a : r) {
            output += (String.format("%02x", a) + " ");
        }
        Log.e("MIDIPort:out", output);

        DatagramPacket response = new DatagramPacket(r, r.length, message.destination_ip, message.destination_port);
        try {
            DatagramSocket socket = getSocket();
            if (socket != null) {
                socket.send(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void doSendMIDI(MIDIControl message) {
        if (!isListening()) return;

        byte[] r = message.getByteArray();

//        String output = "";
//        for(byte a:r) {
//            output += (String.format("%02x", a) + " ");
//        }
//        Log.e("MIDIPort:out",output);


        DatagramPacket response = new DatagramPacket(r, r.length, message.destination_ip, message.destination_port);
        try {
            DatagramSocket socket = getSocket();
            if (socket != null) {
                socket.send(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class SendMidiMessageTask extends AsyncTask<MIDIMessage, Void, Void> {
        @Override
        protected Void doInBackground(MIDIMessage... params) {

            try {
                doSendMIDI(params[0]);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
                ex.printStackTrace();
            }

            return null;
        }
    }

    private class SendMidiControllTask extends AsyncTask<MIDIControl, Void, Void> {
        @Override
        protected Void doInBackground(MIDIControl... params) {

            try {
                doSendMIDI(params[0]);
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
                ex.printStackTrace();
            }

            return null;
        }
    }
}