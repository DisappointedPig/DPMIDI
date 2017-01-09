package com.disappointedpig.midi;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;

import com.disappointedpig.midi.events.MIDIConnectionEstablishedEvent;
import com.disappointedpig.midi.events.MIDIReceivedEvent;
import com.disappointedpig.midi.events.MIDISessionNameRegisteredEvent;
import com.disappointedpig.midi.events.MIDISessionStartEvent;
import com.disappointedpig.midi.events.MIDISessionStopEvent;
import com.disappointedpig.midi.events.MIDISyncronizationCompleteEvent;
import com.disappointedpig.midi.events.MIDISyncronizationStartEvent;
import com.disappointedpig.midi.internal_events.ConnectionEstablishedEvent;
import com.disappointedpig.midi.internal_events.ListeningEvent;
import com.disappointedpig.midi.internal_events.PacketEvent;
import com.disappointedpig.midi.internal_events.StreamConnectedEvent;
import com.disappointedpig.midi.internal_events.StreamDisconnectEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStartedEvent;
import com.disappointedpig.midi.internal_events.SyncronizeStoppedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import static android.content.Context.WIFI_SERVICE;

/*
    session events:
        session start
        session registered
        session name change
        session stop
        session failed?

    stream events:eeee
        stream recieved invite
        stream received invite accepted
        stream received invite rejected
        stream received end
        stream connected
        stream disconnected
        stream syncronized
 */
public class MIDISession {

    private static MIDISession midiSessionInstance;

    private MIDISession() {
        this.rate = 10000;
        this.port = 5004;
        final Random rand = new Random(System.currentTimeMillis());
        this.ssrc = (int) Math.round(rand.nextFloat() * Math.pow(2, 8 * 4));
        this.startTime = (System.currentTimeMillis() / 1000L) * (long)this.rate ;
        this.startTimeHR =  System.nanoTime();
        this.registered_eb = false;
        this.published_bonjour = false;
    }

    public static MIDISession getInstance() {
        if(midiSessionInstance == null) {
            midiSessionInstance = new MIDISession();
        }
        return midiSessionInstance;
    }

    private Boolean isRunning = false;
    private Context appContext;
    private SparseArray<MIDIStream> streams;
    private SparseArray<MIDIStream> pendingStreams;

//    public String localName = Build.MODEL;
    public String bonjourName = Build.MODEL;
    public int port;
    public int ssrc;
    private int readyState;
    private Boolean registered_eb;
    private Boolean published_bonjour;
    private int lastMessageTime;
    private int rate;
    private final long startTime;
    private final long startTimeHR;

    private MIDIPort controlChannel;
    private MIDIPort messageChannel;

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdServiceInfo serviceInfo;

    public void init(Context context) {
        this.appContext = context;
        if(!registered_eb) {
            EventBus.getDefault().register(this);
            registered_eb = true;
        }
    }

    public void start(Context context) {
        init(context);
        start();
    }

    public void start() {
        if(this.appContext == null) {
            return;
        }

        if(!registered_eb) {
            EventBus.getDefault().register(this);
            registered_eb = true;
        }

        this.streams = new SparseArray<MIDIStream>(2);
        this.pendingStreams = new SparseArray<MIDIStream>(2);

        controlChannel = new MIDIPort();
        controlChannel.bind(this.port);
        controlChannel.start();
        messageChannel = new MIDIPort();
        messageChannel.bind(this.port+1);
        messageChannel.start();
        try {
            initializeResolveListener();
            registerService(this.port);
            isRunning = true;
            EventBus.getDefault().post(new MIDISessionStartEvent());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void stop() {

        for(int i = 0; i < streams.size(); i++) {
            streams.get(streams.keyAt(i)).sendEnd();
        }
        isRunning = false;
        // may want to put this on a timer to check if all streams have sent 'END'
        controlChannel.stop();
        messageChannel.stop();

        shutdownNSDListener();

        EventBus.getDefault().post(new MIDISessionStopEvent());
    }

    public void finalize() {
        EventBus.getDefault().unregister(this);

        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void connect(Bundle rinfo) {

        if(isRunning && !isAlreadyConnected(rinfo)) {
            Log.d("midisession","running and not connected");
            MIDIStream stream = new MIDIStream();
            stream.connect(rinfo);
            pendingStreams.put(stream.initiator_token, stream);
        } else {
            Log.d("midisession","not running or already connected");
        }
    }

    private Boolean isAlreadyConnected(Bundle rinfo) {
        for (int i = 0; i < streams.size(); i++) {
//            streams.get(streams.keyAt(i)).sendMessage(message);
//            String key = ((MIDIStream)streams.keyAt(i));
//            Bundle b = (MIDIStream)streams. .getRinfo1();
            Bundle b = streams.get(streams.keyAt(i)).getRinfo1();
            if(b.getString("address").equals(rinfo.getString("address")) && b.getInt("port") == rinfo.getInt("port")) {
                return true;
            }
        }
        return false;
    }

    public void sendUDPMessage(MIDIControl control, Bundle rinfo) {
        if(rinfo.getInt("port") % 2 == 0) {
            controlChannel.sendMidi(control, rinfo);
        } else {
            messageChannel.sendMidi(control, rinfo);
        }
    }

    public void sendUDPMessage(MIDIMessage m, Bundle rinfo) {
        if(rinfo.getInt("port") % 2 == 0) {
            controlChannel.sendMidi(m, rinfo);
        } else {
            messageChannel.sendMidi(m, rinfo);
        }
    }

    public void sendMessage(Bundle m) {
        if(published_bonjour && streams.size() > 0) {
            Log.d("MIDISession", "sendMessage c:"+m.getInt("command",0x09)+" ch:"+m.getInt("channel",0)+" n:"+m.getInt("note",0)+" v:"+m.getInt("velocity",0));

            MIDIMessage message = new MIDIMessage();
            message.createNote(
                    m.getInt("command",0x09),
                    m.getInt("channel",0),
                    m.getInt("note",0),
                    m.getInt("velocity",0));
            message.ssrc = this.ssrc;

            for (int i = 0; i < streams.size(); i++) {
                streams.get(streams.keyAt(i)).sendMessage(message);
            }
        }
    }

    public void sendMessage(int note, int velocity) {
        if(published_bonjour && streams.size() > 0) {
            Log.d("MIDISession", "note:" + note + " velocity:" + velocity);

            MIDIMessage message = new MIDIMessage();
            message.createNote(note, velocity);
            message.ssrc = this.ssrc;

            for (int i = 0; i < streams.size(); i++) {
                streams.get(streams.keyAt(i)).sendMessage(message);
            }
        }
    }

    // getNow returns a unix (long)timestamp
    public long getNow() {
        long hrtime = System.nanoTime()-this.startTimeHR;
        long result = Math.round((hrtime / 1000L / 1000L / 1000L) * this.rate) ;
        return result;
    }


    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onStreamConnected(StreamConnectedEvent e) {
        Log.d("MIDI2Session","StreamConnectedEvent");
        MIDIStream stream = pendingStreams.get(e.initiator_token);
        if(stream != null) {
            streams.put(stream.ssrc, stream);
        }
        pendingStreams.delete(e.initiator_token);
//        EventBus.getDefault().post(new MIDIConnectionEstablishedEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMIDI2ListeningEvent(ListeningEvent e) {

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSyncronizeStartedEvent(SyncronizeStartedEvent e) {
        Log.d("MIDISession","SyncronizeStartedEvent");
        EventBus.getDefault().post(new MIDISyncronizationStartEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSyncronizeStoppedEvent(SyncronizeStoppedEvent e) {
        Log.d("MIDISession","SyncronizeStoppedEvent");
        EventBus.getDefault().post(new MIDISyncronizationCompleteEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onConnectionEstablishedEvent(ConnectionEstablishedEvent e) {
        Log.d("MIDISession","ConnectionEstablishedEvent");
        EventBus.getDefault().post(new MIDIConnectionEstablishedEvent(e.getRInfo()));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPacketEvent(PacketEvent e) {
//        Log.d("MIDI2Session","PacketEvent "+e.getAddress().getHostAddress()+":"+e.getPort());

        // try control first
        MIDIControl applecontrol = new MIDIControl();
        MIDIMessage message = new MIDIMessage();
        if(applecontrol.parse(e)) {
            if(applecontrol.isValid()) {

//                Log.d("MIDISession","initiator_token : "+applecontrol.initiator_token);
                if(applecontrol.initiator_token != 0) {
                    MIDIStream pending = pendingStreams.get(applecontrol.initiator_token);
                    if (pending != null) {
                        Log.d("MIDISession", "got pending stream by token");
                        pending.handleControlMessage(applecontrol, e.getRInfo());
                        return;
                    }
                }
                // check if this applecontrol.ssrc is known stream
                MIDIStream stream = streams.get(applecontrol.ssrc);

                if(stream == null) {
                    // else, check if this is an invitation
                    //       create stream and tell stream to handle invite
                    stream = new MIDIStream();
                    streams.put(applecontrol.ssrc, stream);
                }
                stream.handleControlMessage(applecontrol, e.getRInfo());
            }
            // control packet
        } else {
            message.parseMessage(e);
            if(message.isValid()) {
                EventBus.getDefault().post(new MIDIReceivedEvent(message.toBundle()));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMIDI2StreamDisconnectEvent(StreamDisconnectEvent e) {
        disconnectStream(e.stream_ssrc);
    }

    private void disconnectStream(int ssrc) {
        streams.delete(ssrc);
//        Log.d("MIDI2Session","streams count:"+streams.size());
    }

    public InetAddress getWifiAddress() {
//        EventBus.getDefault().post(new OSCDebugEvent("OSCSession", "getInboundAddress"));

        try {
            if(appContext == null) {
                return InetAddress.getByName("127.0.0.1");
            }
            WifiManager wm = (WifiManager) appContext.getSystemService(WIFI_SERVICE);
            int ip = wm.getConnectionInfo().getIpAddress();

            byte[] ipbytearray= BigInteger.valueOf(wm.getConnectionInfo().getIpAddress()).toByteArray();
//        String ip = String.format("%d.%d.%d.%d", (ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
            reverseByteArray(ipbytearray);
            if(ipbytearray.length != 4) {
                return InetAddress.getByName("127.0.0.1");
            }
            return InetAddress.getByAddress(ipbytearray);
        } catch (UnknownHostException e) {
            return null;
        }
    }

    private static void reverseByteArray(byte[] array) {
        if (array == null) {
            return;
        }
        int i = 0;
        int j = array.length - 1;
        byte tmp;
        while (j > i) {
            tmp = array[j];
            array[j] = array[i];
            array[i] = tmp;
            j--;
            i++;
        }
    }


    // --------------------------------------------
    // bonjour stuff
    //

    public void setBonjourName(String name) {
        this.bonjourName = name;
    }

    private void registerService(int port) throws UnknownHostException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Create the NsdServiceInfo object, and populate it.
            serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.

            serviceInfo.setServiceName(this.bonjourName);
            serviceInfo.setServiceType("_apple-midi._udp");
            serviceInfo.setPort(5004);
            serviceInfo.setHost(getWifiAddress());
//            InetAddress ip = InetAddress.getByAddress(new byte[]{(byte) 172, 16, 1, 85});
//            InetAddress ip = InetAddress.getByAddress(new byte[]{(byte) 192, (byte)168, 58, 101});
//            serviceInfo.setHost(ip);
            serviceInfo.setPort(port);
//        Context mctx = this.getApplicationContext();
            mNsdManager = (NsdManager) appContext.getApplicationContext().getSystemService(Context.NSD_SERVICE);

            initializeNSDRegistrationListener();

            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
            mNsdManager.resolveService(serviceInfo, mResolveListener);

//            Log.d("MIDISession", "registered nsd on " + serviceInfo.getHost() + ":" + serviceInfo.getPort());

//            System.out.println("registered service");
//        } else {
//            if(debugLevel > 0) {
//                EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","api level too low for nsd"));
//            }
//            System.out.println("api level too low for nsd");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeNSDRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                bonjourName = NsdServiceInfo.getServiceName();
                published_bonjour = true;
                EventBus.getDefault().post(new MIDISessionNameRegisteredEvent());
//                EventBus.getDefault().post(new MIDINameChange(bonjourName));

//                System.out.print("onServiceRegistered: ");
//                System.out.println(bonjourName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                System.out.print("onRegistrationFailed ");
                published_bonjour = false;

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                System.out.print("onServiceUnregistered ");
                published_bonjour = false;
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                System.out.print("onUnregistrationFailed ");
                published_bonjour = false;
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void initializeResolveListener() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mResolveListener = new NsdManager.ResolveListener() {
                String TAG = "resolve:";

                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    // Called when the resolve fails.  Use the error code to debug.
                    Log.e(TAG, "Resolve failed" + errorCode);
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

//                    if (serviceInfo.getServiceName().equals(bonjourName)) {
//                        Log.d(TAG, "Same IP.");
//                        //                    return;
//                    }
                    //                mService = serviceInfo;
                    //                int port = mService.getPort();
                    //                InetAddress host = mService.getHost();
                    //                Log.e(TAG, "service host" + serviceInfo.getHost());
//                    if (debugLevel > 0) {
//                        EventBus.getDefault().post(new MIDIDebugEvent("MIDISession", "registered nsd on " + serviceInfo.getHost() + ":" + serviceInfo.getPort()));
//                    }

                }
            };
//        } else {
//            if (debugLevel > 0) {
//                EventBus.getDefault().post(new MIDIDebugEvent("MIDISession", "nsd not available before JELLY_BEAN"));
//            }
        }
    }

    private void shutdownNSDListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNsdManager.unregisterService(mRegistrationListener);
//            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

    }

    public String version() {
        return BuildConfig.VERSION_NAME;
    }

}
