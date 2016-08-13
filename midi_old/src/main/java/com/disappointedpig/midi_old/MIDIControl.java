package com.disappointedpig.midi;

import com.disappointedpig.midi.utility.MIDIJavaToByteArrayConverter;

import java.net.InetAddress;

public class MIDIControl extends AbstractMIDIPacket {

    public int source_port;
    public InetAddress source_ip;
    public int destination_port;
    public InetAddress destination_ip;

    private int command_protocol;
    private int control_command;
    private int control_protocol_version;

    private int initiator_token;
    private int sender_ssrc;
    private String sender_name;

    public int sync_count;
    private int sync_padding;
    public long sync_timestamp1;
    public long sync_timestamp2;
    public long sync_timestamp3;

    private int sequence_number;

    public MIDIControl() {
        this.command_protocol = 0xFFFF;
        this.control_protocol_version = 0x00000002;
    }

    public MIDIControl(int command, int token, int ssrc, String name) {
        this.command_protocol = 0xFFFF;
        this.control_protocol_version = 0x00000002;
        setControlCommand(command);
        setInitiatorToken(token);
        setSenderSSRC(ssrc);
        setSenderName(name);
    }

    public MIDIControl(int ssrc, int count, long timestamp1, long timestamp2, long timestamp3) {
        this.command_protocol = 0xFFFF;
        setControlCommand(0x434B);
        setSenderSSRC(ssrc);
        setSyncCount(count);
        setSyncPadding(0xFFFFFF);
        setSyncTimestamp1(timestamp1);
        setSyncTimestamp2(timestamp2);
        setSyncTimestamp3(timestamp3);
    }

    public MIDIControl(int ssrc, int sequence) {
        this.command_protocol = 0xffff;
        setControlCommand(0x5253);
        setSenderSSRC(ssrc);
        setSequenceNumber(sequence);
    }

    protected byte[] computeByteArray(MIDIJavaToByteArrayConverter stream) {

        switch(this.control_command) {
            case 0x494E:    // invitation
            case 0x4F4B:    // invitation accepted
            case 0x4E4F:    // invitation rejected
                stream.write16(this.command_protocol);
                stream.write16(this.control_command);
                stream.write(this.control_protocol_version);
                stream.write(this.initiator_token);
                stream.write(this.sender_ssrc);
                stream.write(this.sender_name);
                break;
            case 0x434B:    // synchronization
                stream.write16(this.command_protocol);
                stream.write16(this.control_command);
                stream.write(this.sender_ssrc);
                stream.write8(this.sync_count);
                stream.write24(this.sync_padding);
                stream.write64(this.sync_timestamp1);
                stream.write64(this.sync_timestamp2);
                stream.write64(this.sync_timestamp3);
                break;
            case 0x4259:    // end session
                stream.write16(this.command_protocol);
                stream.write16(this.control_command);
                stream.write(this.control_protocol_version);
                stream.write(this.initiator_token);
                stream.write(this.sender_ssrc);
                break;
            case 0x5253:    // receiver feedback
                stream.write16(this.command_protocol);
                stream.write16(this.control_command);
                stream.write(this.sender_ssrc);
                stream.write(this.sequence_number);
            case 0x524C:    // bitrate receive limit
                // I have no idea what this is supposed to do
                break;
        }
        return stream.toByteArray();
    }

    public void setControlCommand(int command) { this.control_command = command & 0xffff; }
    public void setControlProtocolVersion(int version) { this.control_protocol_version = version & 0xffff; }
    public void setInitiatorToken(int token) { this.initiator_token = token; }
    public void setSenderSSRC(int ssrc) { this.sender_ssrc = ssrc; }
    public void setSenderName(String name) { this.sender_name = name; }
    public void setSyncCount(int count) { this.sync_count = count & 0xff; }
    public void setSyncPadding(int padding) { this.sync_padding = padding & 0xffffff; }
    public void setSyncTimestamp1(long timestamp1) { this.sync_timestamp1 = timestamp1; }
    public void setSyncTimestamp2(long timestamp2) { this.sync_timestamp2 = timestamp2; }
    public void setSyncTimestamp3(long timestamp3) { this.sync_timestamp3 = timestamp3; }
    public void setSequenceNumber(int sequence) { this.sequence_number = sequence; }

    public int getCommand() {
        return this.control_command;
    }
    public int getSenderSSRC() {
        return this.sender_ssrc;
    }

    public int getInitiatorToken() {
        return this.initiator_token;
    }


}