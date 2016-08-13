package com.disappointedpig.midi.utility;

import android.util.Log;

import com.disappointedpig.midi.MIDIControl;
import com.disappointedpig.midi.MIDIMessage;
import com.disappointedpig.midi.MIDIPacket;

import java.math.BigInteger;
import java.net.DatagramPacket;
import java.nio.charset.Charset;

public class MIDIByteArrayToJavaConverter {

    private static class Input {

        private final byte[] bytes;
        private final int bytesLength;
        private int streamPosition;

        Input(final byte[] bytes, final int bytesLength) {

            this.bytes = bytes;
            this.bytesLength = bytesLength;
            this.streamPosition = 0;
        }

        public byte[] getBytes() {
            return bytes;
        }

        public int getBytesLength() {
            return bytesLength;
        }

        public int getAndIncreaseStreamPositionByOne() {
            return streamPosition++;
        }

        public void addToStreamPosition(int toAdd) {
            streamPosition += toAdd;
        }

        public int getStreamPosition() {
            return streamPosition;
        }
    }

    private Charset charset;

    public MIDIByteArrayToJavaConverter() {

        this.charset = Charset.defaultCharset();
    }

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public MIDIPacket convert(final DatagramPacket packet) {
        final MIDIPacket midiPacket;
        midiPacket = convertMessage(packet);
        return midiPacket;
    }

//    public MIDIPacket convert(byte[] bytes, int bytesLength, InetAddress ip, int port) {
//
//        final Input rawInput = new Input(bytes, bytesLength);
//        final MIDIPacket packet;
//
//        packet = convertMessage(rawInput, ip,port);
//
//        return packet;
//    }

    private MIDIPacket convertMessage(final DatagramPacket packet) {
        final MIDIMessage message;
        final MIDIControl control;

        final Input rawInput = new Input(packet.getData(),packet.getLength());

        int protocol = read16(rawInput);


//        message.source_ip = packet.getAddress();
//        message.source_port = packet.getPort();

//        int protocol = read16(rawInput);
//        Log.d("MBA2J","process packet with protocol ("+String.format("%02x", protocol) +")");
//        message.protocol = protocol;
        if(protocol == 0xffff) {
            int command = read16(rawInput);
            switch (command) {
                case 0x494E:    // invitation
                case 0x4E4F:    // invitation rejected
                case 0x4F4B:    // invitation accepted
                case 0x4259:    // end session
//                    Log.d("MBA2J", "--- invitation");
                    // received invitation - setup message for session create
                    int protocol_version = readInteger(rawInput);
                    int initiator_token = readInteger(rawInput);
                    int ssrc = readInteger(rawInput);
                    String name = readString(rawInput);
                    control = new MIDIControl(command,initiator_token,ssrc,name);
                    control.source_ip = packet.getAddress();
                    control.source_port = packet.getPort();
                    break;
                case 0x434B:    // synchronize
                    int sync_ssrc = readInteger(rawInput);
                    int sync_count = read8(rawInput);
                    int sync_padding = read24(rawInput);
                    long sync_timestamp1 = readUnsignedInteger64(rawInput);
                    long sync_timestamp2 = readUnsignedInteger64(rawInput);
                    long sync_timestamp3 = readUnsignedInteger64(rawInput);
                    control = new MIDIControl(sync_ssrc,sync_count,sync_timestamp1,sync_timestamp2,sync_timestamp3);
                    control.source_ip = packet.getAddress();
                    control.source_port = packet.getPort();
//                    Log.d("MBA2J", "--- synchronize");
                    break;
                case 0x5253:    // receiver feedback
                    Log.d("MBA2J", "--- receiver feedback");
                case 0x524C:    // bitrate receive limit
                    Log.d("MBA2J", "--- bitrate receive limit");
                default:
                    Log.d("MBA2J", "--- unknown command");
                    control = null;
                    break;
            }
            return control;
        } else {
            // .. non apple-control message... so.. we should do something
            int highorder = protocol >> 8 & 0x0ff;  //4 & 0x000F;
            int loworder = protocol & 0x00ff;

            int version = highorder >> 6;
            boolean padding = ((highorder >> 5) & 1) != 0;
            boolean extension = ((highorder >> 4) & 1) != 0;
            int csic = highorder & 0x0f;
//            Log.d("MBA2J","highorder: ("+String.format("%02x", highorder) +")");
//            Log.d("MBA2J","loworder: ("+String.format("%02x", loworder) +")");
//            Log.d("MBA2J","version:"+version+" padding:"+(padding ? "true" : "false")+" extention:"+(extension ? "true" : "false")+ " csic:"+csic);
            int block2 = loworder; //read8(rawInput);
//            Log.d("MBA2J","block2 ("+String.format("%02x", block2) +")");
            int sequence = read16(rawInput);
            int timestamp = readInteger(rawInput);
            int ssid = readInteger(rawInput);

            boolean marker = (block2 >> 7 & 1) != 0;
            int payload_type = (block2 & 0x7f);
//            Log.d("MBA2J","marker:"+(marker ? "true" : "false")+" payload_type:"+payload_type);
//            int block2a = read8(rawInput);
//            int block2b = read16(rawInput);
//            int block2c = read16(rawInput);
//            int block2d = read8(rawInput);
//            Log.d("MBA2J","block2a ("+String.format("%02x", read8(rawInput)) +")");
//            Log.d("MBA2J","block2b ("+String.format("%02x", read8(rawInput)) +")");
//            Log.d("MBA2J","block2c ("+String.format("%02x", read8(rawInput)) +")");
//            Log.d("MBA2J","block2d ("+String.format("%02x", read8(rawInput)) +")");
//            Log.d("MBA2J","block2e ("+String.format("%02x", read8(rawInput)) +")");
//            Log.d("MBA2J","block2f ("+String.format("%02x", read8(rawInput)) +")");

            int block3 = read8(rawInput);
            boolean bflag = (block3 >> 7 & 1) != 0;
            boolean jflag = ((block3 >> 6 & 1) & 0x1) != 0;
            boolean zflag = ((block3 >> 5 & 1) & 0x1) != 0;
            boolean pflag = ((block3 >> 4 & 1) & 0x1) != 0;
            int command_length = block3 & 0x7;
//            Log.d("MBA2J","block3 ("+String.format("%02x", block3) +")");

//            Log.d("MBA2J","bflag:"+(bflag ? "true" : "false")+" jflag:"+(jflag ? "true" : "false")+" zflag:"+(zflag ? "true" : "false")+" pflag:"+(pflag ? "true" : "false")+" command_length:"+command_length);

            int block4 = read8(rawInput);
            int channel_status = block4 >> 4;
            int channel = block4 & 0xf;
            int block5 = read8(rawInput);
            int note = block5 & 0x7f;
            int block6 = read8(rawInput);
            int velocity = block6 & 0x7f;

//            Log.d("MBA2J","block4 ("+String.format("%02x", block4) +")");

//            Log.d("MBA2J","sequence:"+sequence+ " timestamp:"+timestamp+" ssid:"+ssid+" channel_status:"+channel_status+" channel:"+channel+" note:"+note+" velocity:"+velocity);
            message = new MIDIMessage(sequence,timestamp,ssid,channel_status,channel,note,velocity);

//            Log.d("MBA2J","first:"+String.format("%02x",firstbyte)+" 2A:"+String.format("%02x",secondbyteA)+" 2B"+String.format("%02x",secondbyteB));
            return message;
        }
    }

//    private MIDIMessage convertMessage(final Input rawInput, InetAddress ip, int port) {
//        final MIDIMessage message = new MIDIMessage();
//
//        //get first 2 bytes:
////        byte[] start = read16(rawInput);
//        int start = read16(rawInput);
////        Log.d("MBA2J","received uint message ("+String.format("%02x", start) +")");
//
//        if(start == 0xffff) {
//
//            // try command message
//            message.source_ip = ip;
//            message.source_port = port;
//            message.protocol = start;
////            Log.d("MBA2J","received command message ("+String.format("%02x", message.protocol) +")");
//            int cmd = read16(rawInput);
//            message.command = cmd;
//
//            switch (message.command) {
//                case 0x494E:    // invitation
////                    Log.d("MBA2J","--- invitation");
//
//                    // received invitation - setup message for session create
//                    message.protocol_version = readInteger(rawInput);
//                    message.initiator_token = readInteger(rawInput);
//                    message.ssrc = readInteger(rawInput);
//                    message.name = readString(rawInput);
////                    message.dumpToLog();
//                    break;
//                case 0x4E4F:    // invitation rejected
//                    Log.d("MBA2J","--- invitation rejected");
//                    // ignore unless invitation pending
//                    break;
//                case 0x4F4B:    // invitation accepted
////                    Log.d("MBA2J","--- invitation accepted");
//                    // ignore unless invitation pending
//                    break;
//                case 0x4259:    // end session
//                    Log.d("MBA2J","--- end session");
//                    break;
//                case 0x434B:    // synchronize
////                    Log.d("MBA2J","--- synchronize");
//                    message.ssrc = readInteger(rawInput);
//                    message.sync_count = read8(rawInput);
//                    message.sync_padding = read24(rawInput);
//                    message.sync_timestamp1 = readUnsignedInteger64(rawInput);
//                    message.sync_timestamp2 = readUnsignedInteger64(rawInput);
//                    message.sync_timestamp3 = readUnsignedInteger64(rawInput);
////                    message.dumpToLog();
//                    break;
//                case 0x5253:    // receiver feedback
//                    Log.d("MBA2J","--- receiver feedback");
//                case 0x524C:    // bitrate receive limit
//                    Log.d("MBA2J","--- bitrate receive limit");
//                default:
//                    Log.d("MBA2J","--- unknown command");
//                    break;
//
//            }
//        } else {
//            int firstbyte = start >> 8 & 0x00FF;
//            int secondbyteA = start >> 4 & 0xFF;
//            int secondbyteB = start & 0xFF;
//            Log.d("MBA2J","first:"+String.format("%02x",firstbyte)+" 2A:"+String.format("%02x",secondbyteA)+" 2B"+String.format("%02x",secondbyteB));
//
////            Log.d("MBA2J","received rtp message ("+ Arrays.toString(start) +")("+String.format("%02x", message.protocol)+")");
//            // try rtp message
//        }
//
//
//        return message;
//    }

    private byte readByte(final Input rawInput) {
        final byte[] intBit = new byte[1];

        System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), intBit, 0, 1);
        rawInput.addToStreamPosition(1);
        return intBit[0];
    }

    private String readString(final Input rawInput) {
        final int strLen = lengthOfCurrentString(rawInput);
//        final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen, charset);
        final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen);
        rawInput.addToStreamPosition(strLen);
        moveToFourByteBoundry(rawInput);
        return res;
    }

    /**
     * Reads an Integer (32 bit integer) from the byte stream.
     * @return an {@link Integer}
     */
    private Integer readInteger(final Input rawInput) {
        final BigInteger intBits = readBigInteger(rawInput, 4);
        return intBits.intValue();
    }

    private int read16(final Input rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));

        return (firstByte << 8
                | secondByte)
                & 0xFFFF;
    }

    private int read8(final Input rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return (firstByte & 0xFF);
    }

    private int read24(final Input rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return ((int) (firstByte << 16
                | secondByte << 8
                | thirdByte))
                & 0xFFFFFF;
    }
    private BigInteger readBigInteger(final Input rawInput, final int numBytes) {
        final byte[] myBytes = new byte[numBytes];
        System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), myBytes, 0, numBytes);
        rawInput.addToStreamPosition(numBytes);
        return  new BigInteger(myBytes);
    }

    private Long readUnsignedInteger(final Input rawInput) {

        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int fourthByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return ((long) (firstByte << 24
                | secondByte << 16
                | thirdByte << 8
                | fourthByte))
                & 0xFFFFFFFFL;
    }

    private long readUnsignedInteger64(final Input rawInput) {

        long highword = readUnsignedInteger(rawInput);
        long lowword = readUnsignedInteger(rawInput);

        long x = highword;
        x <<= 32;
        x += lowword;
        x &= 0xFFFFFFFFFFFFFFFFL;
        return x;
    }

    /**
     * Get the length of the string currently in the byte stream.
     */
    private int lengthOfCurrentString(final Input rawInput) {
        int len = 0;
        while (rawInput.getBytes()[rawInput.getStreamPosition() + len] != 0) {
            len++;
        }
        return len;
    }

    /**
     * Move to the next byte with an index in the byte array
     * which is dividable by four.
     */
    private void moveToFourByteBoundry(final Input rawInput) {
        // If i am already at a 4 byte boundry, I need to move to the next one
        final int mod = rawInput.getStreamPosition() % 4;
        rawInput.addToStreamPosition(4 - mod);
    }

    public static int byteArrayToInt(byte[] b)
    {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i] & 0x000000FF) << shift;
        }
        return value;
    }
}

