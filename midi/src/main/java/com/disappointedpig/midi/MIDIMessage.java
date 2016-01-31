package com.disappointedpig.midi;

import com.disappointedpig.midi.utility.MIDIJavaToByteArrayConverter;

import java.net.InetAddress;

public class MIDIMessage extends AbstractMIDIPacket {

    public int source_port;
    public InetAddress source_ip;
    public int destination_port;
    public InetAddress destination_ip;

    // rttp section
    public int rttp_version;
    public Boolean rttp_padding;
    public Boolean rttp_extension;
    public int rttp_contributing_source_identifiers_count;
    public Boolean rttp_marker;
    public int rttp_payload_type;
    public int rttp_sequence_number;
    public int rttp_timestamp;
    public int sender_ssrc;

    // rtp-midi
    public boolean bflag; // short header (1 octet)
    public boolean jflag; // journal present
    public boolean zflag; // delta-time for first midi command
    public boolean pflag; // status byte present in original midi command
    public int command_length;

    // command section
    public int midi_channel_status;
    public int midi_channel;
    public int midi_note;
    public int midi_velocity;

    /**
     * Creates an empty OSC Message.
     * In order to send this OSC message,
     * you need to set the address and optionally some arguments.
     */
    public MIDIMessage() { }

    public MIDIMessage(int sequence, int timestamp, int ssrc, int status, int channel, int note, int velocity) {
        this.rttp_version = 2;
        this.rttp_padding = false;
        this.rttp_extension = false;
        this.rttp_contributing_source_identifiers_count = 0;
        this.rttp_marker = false;
        this.rttp_payload_type = 97; // (0x61)
        this.rttp_sequence_number = sequence & 0xffff;
        this.rttp_timestamp = timestamp; // & 0xffffffff;
        this.sender_ssrc = ssrc; // & 0xffffffff;

        this.bflag = false;
        this.jflag = false;
        this.zflag = false;
        this.pflag = false;
        this.command_length = 3;

        this.midi_channel_status = status & 0x000f;
        this.midi_channel = channel & 0x000f;
        this.midi_note = note & 0x007f;
        this.midi_velocity = velocity & 0x007f;
    }

    @Override
    protected byte[] computeByteArray(MIDIJavaToByteArrayConverter stream) {

        return stream.toByteArray();
    }

    public int getSenderSSRC() {
        return this.sender_ssrc;
    }

//    public void dumpToLog() {
//        switch(command) {
//            case 0x494E:
//                Log.d("MIDIMessage:Invitation", "start: " + String.format("%02x", protocol));
//                Log.d("MIDIMessage:Invitation", "command: "+ String.format("%02x", command));
//                Log.d("MIDIMessage:Invitation", "version: "+ String.format("%02x", protocol_version));
//                Log.d("MIDIMessage:Invitation", "token: "+ String.format("%02x", initiator_token));
//                Log.d("MIDIMessage:Invitation", "ssrc: " + String.format("%02x", ssrc));
//                Log.d("MIDIMessage:Invitation", "name: " + name);
//                break;
//            case 0x4F4B:
//                Log.d("MIDIMessage:IAccept", "start: " + String.format("%02x", protocol));
//                Log.d("MIDIMessage:IAccept", "command: "+ String.format("%02x", command));
//                Log.d("MIDIMessage:IAccept", "version: "+ String.format("%02x", protocol_version));
//                Log.d("MIDIMessage:IAccept", "token: "+ String.format("%02x", initiator_token));
//                Log.d("MIDIMessage:IAccept", "ssrc: " + String.format("%02x", ssrc));
//                Log.d("MIDIMessage:IAccept", "name: " + name);
//                Log.d("MIDIMessage:IAccept", "destination  "+destination_ip+":"+destination_port);
//                break;
//            case 0x434B:
//                Log.d("MIDIMessage:Syncronize", "start: " + String.format("%02x", protocol));
//                Log.d("MIDIMessage:Syncronize", "command: "+ String.format("%02x", command));
//                Log.d("MIDIMessage:Syncronize", "ssrc: " + String.format("%02x", ssrc));
//                Log.d("MIDIMessage:Syncronize", "count: "+ String.format("%02x", sync_count));
//                Log.d("MIDIMessage:Syncronize", "padding: "+ String.format("%02x", sync_padding));
//                Log.d("MIDIMessage:Syncronize", "timestamp1: " + String.format("%02x", sync_timestamp1));
//                Log.d("MIDIMessage:Syncronize", "timestamp2: " + String.format("%02x", sync_timestamp2));
//                Log.d("MIDIMessage:Syncronize", "timestamp3: " + String.format("%02x", sync_timestamp3));
//                Log.d("MIDIMessage:Syncronize", "destination  "+destination_ip+":"+destination_port);
//                break;
//            case 0x4259:
//                Log.d("MIDIMessage:End", "start: " + String.format("%02x", protocol));
//                Log.d("MIDIMessage:End", "command: "+ String.format("%02x", command));
//                Log.d("MIDIMessage:End", "version: "+ String.format("%02x", protocol_version));
//                Log.d("MIDIMessage:End", "token: "+ String.format("%02x", initiator_token));
//                Log.d("MIDIMessage:End", "ssrc: " + String.format("%02x", ssrc));
//                Log.d("MIDIMessage:End", "name: " + name);
//                Log.d("MIDIMessage:End", "destination  "+destination_ip+":"+destination_port);
//                break;
//            default:
//                Log.d("MIDIMessage:dumpToLog", "unhandled "+String.format("%02x", command));
//        }
//    }
}