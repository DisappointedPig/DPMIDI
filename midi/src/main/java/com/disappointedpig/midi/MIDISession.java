package com.disappointedpig.midi;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.util.SparseArray;

import org.greenrobot.eventbus.EventBus;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class MIDISession {
    private static MIDISession midiSessionInstance;

    private MIDISession() {
//        System.out.println("MIDISession init");
        this.rate = 10000;
        this.debugLevel = 0;
        this.streams = new SparseArray<MIDIStream>(2);
        this.externalListeners = new ArrayList<MIDIListener>(2);

        final Random rand = new Random(System.currentTimeMillis());

        this.ssrc = (int) Math.round(rand.nextFloat() * Math.pow(2,8*4)) & 0xFFFFFFFF;
    }

    public static MIDISession getInstance() {
        if(midiSessionInstance == null) {
            midiSessionInstance = new MIDISession();
        }
        return midiSessionInstance;
    }

    private int debugLevel;
    private Integer sessionToken;
    private final SparseArray<MIDIStream> streams;
    private final ArrayList<MIDIListener> externalListeners;
    private String bonjourName;
    private int port;
    private final int defaultPort = 5004;
    private int ssrc;
    private long startTime;
    private long startTimeHR;
    private int rate;
    private MIDIPort controlChannel;
    private MIDIPort messageChannel;

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdServiceInfo serviceInfo;

    private MIDIControlListener midiCommandListener;
    private MIDIMessageListener midiMessageListener;

    private Context appContext;

    public void initMIDI(Context context, String bonjourName) {
        initMIDI(context,defaultPort,bonjourName);
    }

    public void initMIDI(Context context) {
        initMIDI(context, defaultPort, Build.MODEL);
    }

    public void initMIDI(Context context, int debugLevel) {
        this.debugLevel = debugLevel;
        initMIDI(context, defaultPort, Build.MODEL);
    }
    public void initMIDI(Context context, int port, String bonjourName, int debugLevel) {
        this.debugLevel = debugLevel;
        initMIDI(context,port,bonjourName);
    }

    public void initMIDI(Context context, int port, String bonjourName) {
//        Log.d("MIDISession", "initMidi");
        if(debugLevel > 0) {
            EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","initialized"));
        }

        this.appContext = context;
        this.port = port;
//        this.localName = localName;
        this.bonjourName = bonjourName;

        try {
            initializeResolveListener();

            registerService(this.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //Math.round(Math.random() * Math.pow(2, 8 * 4));;

        this.startTime = (System.currentTimeMillis() / 1000L) * (long)this.rate ;
        this.startTimeHR =  System.nanoTime();

//        Log.i("startTime","t:"+String.format("%02x",this.startTime)+ " thr:"+String.format("%02x",this.startTimeHR));
        if(debugLevel > 5) {
            EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","t:"+String.format("%02x",this.startTime)+ " thr:"+String.format("%02x",this.startTimeHR)));
        }


//        dumpPhoneBuildInfo();

        try {
            controlChannel = new MIDIPort(this.port);
            messageChannel = new MIDIPort(this.port+1);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public long getStartTime() {
        return this.startTime;
    }

    public long getStartTimeHR() {
        return this.startTimeHR;
    }

    public Boolean isBonjourAvailable() {
        return true;
    }

    public Boolean isBonjourActive() {
        return true;
    }

    public long getNow() {
//        var hrtime = process.hrtime(this.startTimeHr);
//        return Math.round(((hrtime[0] + hrtime[1] / 1000 / 1000 / 1000)) * this.rate) % 0xffffffff;
//        this.ssrc = (int) Math.round(rand.nextFloat() * Math.pow(2,8*4)) & 0xFFFFFFFF;
        long hrtime = System.nanoTime()-this.startTimeHR;
        long result = Math.round((hrtime / 1000L / 1000L / 1000L) * this.rate) ;//% 0xffffffff;

//        Log.i("NOW", "hrtime:" + String.format("%02x", hrtime) + "now: " + String.format("%02x", result));
        return result;
    }

    public void startListening() {
//        Log.d("MIDISession","startListening");
        if(debugLevel > 0) {
            EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","start listening"));
        }

        midiCommandListener = new MIDIControlListener() {
            @Override
            public void acceptMessage(Date time, MIDIControl message) {
                MIDIStream stream = streams.get(message.getSenderSSRC());
                if(stream == null) {
                    stream = new MIDIStream(message);
                    if(stream.isValidStream) {
                        streams.put(message.getSenderSSRC(), stream);
                    }
                }
                stream.handleMessage(message);
            }

//            @Override
//            public void acceptMessage(Date time, MIDIControl message) {
//            }
        };

        midiMessageListener = new MIDIMessageListener() {
            @Override
            public void acceptMessage(Date time, MIDIMessage message) {

                EventBus.getDefault().post(new MIDIEvent(message));
//                MIDIStream stream = streams.get(message.ssrc);
//                if(stream == null) {
//                    stream = new MIDIStream(message, port+1, getSSRC());
//                    streams.put(message.ssrc,stream);
//                }
//                stream.handleMessage(message);
            }
        };

        controlChannel.addListener(midiCommandListener);
        messageChannel.addListener(midiCommandListener);

        controlChannel.addListener(midiMessageListener);
        messageChannel.addListener(midiMessageListener);

        controlChannel.startListening();
        messageChannel.startListening();


    }

    public int getDebugLevel() {return this.debugLevel; }

    public int getSSRC() {
        return this.ssrc;
    }

    public String getName() {
        return this.bonjourName;
    }

    public void stopListening() {
        new StopAllMidiTask().execute();
    }

    public void doStopListening() {
//        Log.d("MIDISession","stopListening: streams "+streams.size());
        if(debugLevel > 0) {
            EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","stop listening: streams "+streams.size()));
        }
        if(streams == null) {
            EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","error - no streams to stop"));
            return;
        }
        for(int i = 0; i < streams.size(); i++) {
            controlChannel.sendMIDI(streams.get(streams.keyAt(i)).getEndMessage());
            streams.valueAt(i).shutdownStream();
        }
        if(controlChannel != null) {
            controlChannel.stopListening();
            controlChannel.close();
            controlChannel = null;
        }

        if(messageChannel != null) {
            messageChannel.stopListening();
            messageChannel.close();
            messageChannel = null;
        }
        if(streams != null) {
            streams.clear();
        }
        shutdownNSDListener();
    }

    private class StopAllMidiTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {

            try {
                doStopListening();
            } catch (Exception ex) {
                // this is just a demo program, so this is acceptable behavior
                ex.printStackTrace();
            }


            return null;
        }
    }

    public void closeStream(int stream_ssrc) {
        streams.remove(stream_ssrc);
    }


    // --------------------------------------------
    // bonjour stuff
    //
    public void registerService(int port) throws UnknownHostException {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Create the NsdServiceInfo object, and populate it.
            serviceInfo = new NsdServiceInfo();

            // The name is subject to change based on conflicts
            // with other services advertised on the same network.

            serviceInfo.setServiceName(bonjourName);
            serviceInfo.setServiceType("_apple-midi._udp");
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
        } else {
            if(debugLevel > 0) {
                EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","api level too low for nsd"));
            }
//            System.out.println("api level too low for nsd");
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializeNSDRegistrationListener() {
        mRegistrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                bonjourName = NsdServiceInfo.getServiceName();
//                System.out.print("onServiceRegistered: ");
//                System.out.println(bonjourName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                System.out.print("onRegistrationFailed ");

            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
                System.out.print("onServiceUnregistered ");
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
                System.out.print("onUnregistrationFailed ");
            }
        };
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public void initializeResolveListener() {
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

                    if (serviceInfo.getServiceName().equals(bonjourName)) {
                        Log.d(TAG, "Same IP.");
                        //                    return;
                    }
                    //                mService = serviceInfo;
                    //                int port = mService.getPort();
                    //                InetAddress host = mService.getHost();
                    //                Log.e(TAG, "service host" + serviceInfo.getHost());
                    if (debugLevel > 0) {
                        EventBus.getDefault().post(new MIDIDebugEvent("MIDISession", "registered nsd on " + serviceInfo.getHost() + ":" + serviceInfo.getPort()));
                    }

                }
            };
//        } else {
//            if (debugLevel > 0) {
//                EventBus.getDefault().post(new MIDIDebugEvent("MIDISession", "nsd not available before JELLY_BEAN"));
//            }
        }
    }

    public void shutdownNSDListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNsdManager.unregisterService(mRegistrationListener);
//            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

    }

    public void sendMIDI(MIDIControl message) {
        controlChannel.sendMIDI(message);
    }

    public void sendMIDI(MIDIMessage message) {
//        if(message.source_port == this.port) {
//            controlChannel.sendMIDI(message);
//        } else {
//            switch (message.protocol) {
//                case 0xFFFF:
//                    controlChannel.sendMIDI(message,true);
//                    break;
//                default:
        messageChannel.sendMIDI(message,true);
//            }
//        }
    }

    public void dumpPhoneBuildInfo() {
        Log.d("MIDISession","board:"+ Build.BOARD);
        Log.d("MIDISession","bootloader:"+ Build.BOOTLOADER);
        Log.d("MIDISession","brand:"+ Build.BRAND);
        Log.d("MIDISession","cpu_abi:"+ Build.CPU_ABI);
        Log.d("MIDISession","cpu_abi2:"+ Build.CPU_ABI2);
        Log.d("MIDISession","device:"+ Build.DEVICE);
        Log.d("MIDISession","display:"+ Build.DISPLAY);
        Log.d("MIDISession","fingerprint:"+ Build.FINGERPRINT);
        Log.d("MIDISession","hardware:"+ Build.HARDWARE);
        Log.d("MIDISession","host:"+ Build.HOST);
        Log.d("MIDISession","id:"+ Build.ID);
        Log.d("MIDISession","manufacturer:"+ Build.MANUFACTURER);
        Log.d("MIDISession","model:"+ Build.MODEL);
        Log.d("MIDISession","product:"+ Build.PRODUCT);
        Log.d("MIDISession","radio:"+ Build.RADIO);
//        Log.d("MIDISession","serial:"+ Build.SERIAL);
        Log.d("MIDISession","tags:"+ Build.TAGS);
        Log.d("MIDISession","type:"+ Build.TYPE);
        Log.d("MIDISession","user:"+ Build.USER);
    }

    public void changeBonjourName(String name) {
        shutdownNSDListener();
        this.bonjourName = name;
        try {
            registerService(this.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

    }

    public void addMIDIMessageListener() {

    }

    public Boolean removeMIDIMessageListener() {
        return true;
    }
}
