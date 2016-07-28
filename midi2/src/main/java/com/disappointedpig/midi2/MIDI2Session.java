package com.disappointedpig.midi2;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import com.disappointedpig.midi2.events.MIDI2ListeningEvent;
import com.disappointedpig.midi2.events.MIDI2PacketEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;

import static android.content.Context.WIFI_SERVICE;

public class MIDI2Session {

    private static MIDI2Session midiSessionInstance;

    private MIDI2Session() {
        this.rate = 10000;
        this.port = 5004;
        final Random rand = new Random(System.currentTimeMillis());
        this.ssrc = (int) Math.round(rand.nextFloat() * Math.pow(2, 8 * 4));
        this.startTime = (System.currentTimeMillis() / 1000L) * (long)this.rate ;
        this.startTimeHR =  System.nanoTime();

    }

    public static MIDI2Session getInstance() {
        if(midiSessionInstance == null) {
            midiSessionInstance = new MIDI2Session();
        }
        return midiSessionInstance;
    }


    private Context appContext;
//    private final Array<MIDI2Stream> streams;
    private String localName;
    private String bonjourName;
    private int port;
    private int ssrc;
    private int readyState;
    private Boolean published;
    private int lastMessageTime;
    private int rate;
    private final long startTime;
    private final long startTimeHR;

    private MIDI2Port controlChannel;
    private MIDI2Port messageChannel;

    private NsdManager mNsdManager;
    private NsdManager.ResolveListener mResolveListener;
    private NsdManager.DiscoveryListener mDiscoveryListener;
    private NsdManager.RegistrationListener mRegistrationListener;
    private NsdServiceInfo serviceInfo;

    public void start(Context context) {
        this.appContext = context;
        EventBus.getDefault().register(this);
        this.bonjourName = Build.MODEL;

//        try {
            controlChannel = new MIDI2Port();
            controlChannel.bind(this.port);
            controlChannel.start();
            messageChannel = new MIDI2Port();
            messageChannel.bind(this.port+1);
            messageChannel.start();
//        } catch (SocketException e) {
//            e.printStackTrace();
//        }

        try {
            initializeResolveListener();
            registerService(this.port);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        controlChannel.stop();
        messageChannel.stop();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMIDI2ListeningEvent(MIDI2ListeningEvent e) {

    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onMIDI2PacketEvent(MIDI2PacketEvent e) {
        Log.d("MIDI2Session","MIDI2MessageEvent "+e.getAddress().getHostAddress()+":"+e.getPort());

        // try control first
        MIDI2Control applecontrol = new MIDI2Control();
        MIDI2Message message = new MIDI2Message();
        if(applecontrol.parse(e)) {
            // control packet
        } else {
            message.parseMessage(e);
        }


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
//        } else {
//            if(debugLevel > 0) {
//                EventBus.getDefault().post(new MIDIDebugEvent("MIDISession","api level too low for nsd"));
//            }
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

//                EventBus.getDefault().post(new MIDINameChange(bonjourName));

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

    public void shutdownNSDListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            mNsdManager.unregisterService(mRegistrationListener);
//            mNsdManager.stopServiceDiscovery(mDiscoveryListener);
        }

    }
}
