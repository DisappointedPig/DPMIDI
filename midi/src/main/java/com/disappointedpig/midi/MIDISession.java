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

public class MIDISession {

    private static MIDISession midiSessionInstance;
    private static String TAG = "MIDISession";
    private static String BONJOUR_TYPE = "_apple-midi._udp";
    private static String BONJOUR_SEPARATOR = ".";

    private static boolean DEBUG = false;

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
    private Context appContext = null;
    private SparseArray<MIDIStream> streams;
    private SparseArray<MIDIStream> pendingStreams;

    public String bonjourName = Build.MODEL;
    public InetAddress bonjourHost = null;
    public int bonjourPort = 0;

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
        try {
            this.bonjourHost = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        this.bonjourHost = getWifiAddress();
        this.bonjourPort = this.port;
        controlChannel = MIDIPort.newUsing(this.port);
        controlChannel.start();
        messageChannel = MIDIPort.newUsing(this.port+1);
        messageChannel.start();

        this.streams = new SparseArray<>(2);
        this.pendingStreams = new SparseArray<>(2);
        try {
            initializeResolveListener();
            registerService();
            isRunning = true;
            EventBus.getDefault().post(new MIDISessionStartEvent());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }


    public void stop() {
        for (int i = 0; i < streams.size(); i++) {
            streams.get(streams.keyAt(i)).sendEnd();
        }
        for (int i = 0; i < pendingStreams.size(); i++) {
            pendingStreams.get(pendingStreams.keyAt(i)).sendEnd();
        }

        if(controlChannel != null) {
            controlChannel.stop();
        }
        if(messageChannel != null) {
            messageChannel.stop();
        }
        isRunning = false;

        shutdownNSDListener();
        EventBus.getDefault().post(new MIDISessionStopEvent());

    }


    public void finalize() {
        stop();
        EventBus.getDefault().unregister(this);
        try {
            super.finalize();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    public void connect(Bundle rinfo) {

        if(isRunning && !isAlreadyConnected(rinfo)) {
//            Log.d("midisession","running and not connected");
            MIDIStream stream = new MIDIStream();
            stream.connect(rinfo);
            pendingStreams.put(stream.initiator_token, stream);
        } else {
//            Log.d("midisession","not running or already connected");
        }
    }

    private Boolean isAlreadyConnected(Bundle rinfo) {
        for (int i = 0; i < streams.size(); i++) {
//            streams.get(streams.keyAt(i)).sendMessage(message);
//            String key = ((MIDIStream)streams.keyAt(i));
//            Bundle b = (MIDIStream)streams. .getRinfo1();
            Bundle b = streams.get(streams.keyAt(i)).getRinfo1();
            if(b.getString(Consts.RINFO_ADDR).equals(rinfo.getString(Consts.RINFO_ADDR)) && b.getInt(Consts.RINFO_PORT) == rinfo.getInt(Consts.RINFO_PORT)) {
                return true;
            }
        }
        return false;
    }

    public void sendUDPMessage(MIDIControl control, Bundle rinfo) {
//        Log.d("MIDISession","sendUDPMessage:control");
        if(rinfo.getInt(Consts.RINFO_PORT) % 2 == 0) {
            Log.d("MIDISession","sendUDPMessage control 5004 rinfo:"+rinfo.toString());
//            controlChannel.sendMidi(control, rinfo);
            controlChannel.sendMidi(control, rinfo);
        } else {
            Log.d("MIDISession","sendUDPMessage control 5005 rinfo:"+rinfo.toString());
//            messageChannel.sendMidi(control, rinfo);
            messageChannel.sendMidi(control, rinfo);
        }
    }

    public void sendUDPMessage(MIDIMessage m, Bundle rinfo) {
//        Log.d("MIDISession","sendUDPMessage:message");
        if(m != null && rinfo != null) {
            if (rinfo.getInt(Consts.RINFO_PORT) % 2 == 0) {
                controlChannel.sendMidi(m, rinfo);
            } else {
                messageChannel.sendMidi(m, rinfo);
            }
        }
    }

    public void sendMessage(Bundle m) {
        if(published_bonjour && streams.size() > 0) {
//            Log.d("MIDISession", "sendMessage c:"+m.getInt("command",0x09)+" ch:"+m.getInt("channel",0)+" n:"+m.getInt("note",0)+" v:"+m.getInt("velocity",0));

            MIDIMessage message = new MIDIMessage();
            message.createNote(
                    m.getInt(Consts.MSG_COMMAND,0x09),
                    m.getInt(Consts.MSG_CHANNEL,0),
                    m.getInt(Consts.MSG_NOTE,0),
                    m.getInt(Consts.MSG_VELOCITY,0));
            message.ssrc = this.ssrc;

            for (int i = 0; i < streams.size(); i++) {
                streams.get(streams.keyAt(i)).sendMessage(message);
            }
        }
    }

    public void sendMessage(int note, int velocity) {
        if(published_bonjour && streams.size() > 0) {
//            Log.d("MIDISession", "note:" + note + " velocity:" + velocity);

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
//        Log.d("MIDI2Session","StreamConnectedEvent");
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
//        Log.d("MIDISession","SyncronizeStartedEvent");
        EventBus.getDefault().post(new MIDISyncronizationStartEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSyncronizeStoppedEvent(SyncronizeStoppedEvent e) {
//        Log.d("MIDISession","SyncronizeStoppedEvent");
        EventBus.getDefault().post(new MIDISyncronizationCompleteEvent());
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onConnectionEstablishedEvent(ConnectionEstablishedEvent e) {
//        Log.d("MIDISession","ConnectionEstablishedEvent");
        EventBus.getDefault().post(new MIDIConnectionEstablishedEvent(e.getRInfo()));
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onPacketEvent(PacketEvent e) {
//        Log.d("MIDISession","PacketEvent packet from "+e.getAddress().getHostAddress()+":"+e.getPort());

        // try control first
        MIDIControl applecontrol = new MIDIControl();
        MIDIMessage message = new MIDIMessage();

        if(applecontrol.parse(e)) {
//            Log.d("MIDISession","- parsed as apple control packet");
            if(applecontrol.isValid()) {
//                applecontrol.dumppacket();

                if(applecontrol.initiator_token != 0) {
                    MIDIStream pending = pendingStreams.get(applecontrol.initiator_token);
                    if (pending != null) {
                        Log.d("MIDISession", " - got pending stream by token");
                        pending.handleControlMessage(applecontrol, e.getRInfo());
                        return;
                    }
                }
                // check if this applecontrol.ssrc is known stream
                MIDIStream stream = streams.get(applecontrol.ssrc);

                if(stream == null) {
                    // else, check if this is an invitation
                    //       create stream and tell stream to handle invite
//                    Log.d("MIDISession","- create new stream");
                    stream = new MIDIStream();
                    streams.put(applecontrol.ssrc, stream);
                } else {
//                    Log.d("MIDISession", " - got existing stream by ssrc");

                }
//                Log.d("MIDISession","- pass control packet to stream");

                stream.handleControlMessage(applecontrol, e.getRInfo());
            }
            // control packet
        } else {
//            Log.d("MIDISession","message?");
            message.parseMessage(e);
            if(message.isValid()) {
                EventBus.getDefault().post(new MIDIReceivedEvent(message.toBundle()));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onStreamDisconnectEvent(StreamDisconnectEvent e) {
        if(DEBUG) {
            Log.d(TAG,"onStreamDisconnectEvent - ssrc:"+e.stream_ssrc+" it:"+e.initiator_token+" #streams:"+streams.size()+" #pendstreams:"+pendingStreams.size());
        }
        MIDIStream a = streams.get(e.stream_ssrc,null);
        if(a == null) {
            Log.d(TAG,"can't find stream with ssrc "+e.stream_ssrc);
        } else {
            a.shutdown();
            streams.delete(e.stream_ssrc);
        }
        if(e.initiator_token != 0) {
            MIDIStream p = pendingStreams.get(e.initiator_token,null);
            if(p == null) {
                Log.d(TAG,"can't find pending stream with IT "+e.initiator_token);
            } else {
                p.shutdown();
                pendingStreams.delete(e.initiator_token);
            }

        }
    }

    public InetAddress getWifiAddress() {
        try {
            if(appContext == null) {
                return InetAddress.getByName("127.0.0.1");
            }
            WifiManager wm = (WifiManager) appContext.getSystemService(WIFI_SERVICE);
            byte[] ipbytearray= BigInteger.valueOf(wm.getConnectionInfo().getIpAddress()).toByteArray();
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

    private void registerService() throws UnknownHostException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Create the NsdServiceInfo object, and populate it.
            serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.

            serviceInfo.setServiceName(this.bonjourName);
            serviceInfo.setServiceType(BONJOUR_TYPE);
            serviceInfo.setHost(this.bonjourHost);
            serviceInfo.setPort(this.bonjourPort);

            if(DEBUG) {
                Log.d(TAG,"register service: "+serviceInfo.toString());
            }
            mNsdManager = (NsdManager) appContext.getApplicationContext().getSystemService(Context.NSD_SERVICE);

            initializeNSDRegistrationListener();

            mNsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, mRegistrationListener);
//            mNsdManager.resolveService(serviceInfo, mResolveListener);
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
                Log.d(TAG,"Service Registered "+NsdServiceInfo.toString());
                if(NsdServiceInfo.getServiceName() != null && bonjourName != NsdServiceInfo.getServiceName()) {
                    bonjourName = NsdServiceInfo.getServiceName();
                    serviceInfo.setServiceName(bonjourName);

//                    mNsdManager.resolveService(serviceInfo, mResolveListener);

                }
                mNsdManager.resolveService(serviceInfo, mResolveListener);

                published_bonjour = true;
                EventBus.getDefault().post(new MIDISessionNameRegisteredEvent());
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                System.out.print("onRegistrationFailed \n"+serviceInfo.toString()+"\nerror code: "+errorCode);
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

                @Override
                public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                    // Called when the resolve fails.  Use the error code to debug.
                    Log.e(TAG, "Resolve failed" + errorCode);
                }

                @Override
                public void onServiceResolved(NsdServiceInfo serviceInfo) {
                    Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                }
            };
        }
    }

    private void shutdownNSDListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {

            if(mNsdManager != null) {
                mNsdManager.unregisterService(mRegistrationListener);
            }
//            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

    }

    public String version() {
        return BuildConfig.VERSION_NAME;
    }

}
