package com.disappointedpig.midi;

import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;
import com.disappointedpig.midi.utility.DataBuffer;
import com.disappointedpig.midi.utility.DataBufferReader;
import com.disappointedpig.midi.utility.OutDataBuffer;

import java.util.HashMap;
import java.util.Map;

import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.BITRATE_RECEIVE_LIMIT;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.END;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.INVITATION;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.INVITATION_ACCEPTED;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.INVITATION_REJECTED;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.RECEIVER_FEEDBACK;
import static com.disappointedpig.midi.MIDIControl.AppleMIDICommand.SYNCHRONIZATION;

public class MIDIControl {

    private Boolean valid;
    private DataBuffer m;
    public AppleMIDICommand command;

    public int protocol_version;
    public int initiator_token;
    public int ssrc;
    public String name;
    public int count;
    public int padding;
    public long timestamp1,timestamp2,timestamp3;
    public int sequenceNumber;


    public MIDIControl() {}

    public boolean parse(PacketEvent packet) {
        this.valid = false;
        final DataBuffer rawInput = new DataBuffer(packet.getData(),packet.getLength());
        final DataBufferReader reader = new DataBufferReader();
        int protocol = reader.read16(rawInput);
        if(protocol == 0xffff) {
            command = commandMap.get(reader.read16(rawInput));
            switch (command) {
                case INVITATION:
                case INVITATION_ACCEPTED:
                case INVITATION_REJECTED:
                case END:
                    this.valid = true;
                    protocol_version = reader.readInteger(rawInput);
                    initiator_token = reader.readInteger(rawInput);
                    ssrc = reader.readInteger(rawInput);
                    name = reader.readString(rawInput);
//                    this.version = buffer.readUInt32BE(4);
//                    this.token = buffer.readUInt32BE(8);
//                    this.ssrc = buffer.readUInt32BE(12);
//                    this.name = buffer.toString('utf-8', 16);

                    break;
                case SYNCHRONIZATION:
                    this.valid = true;
                    ssrc = reader.readInteger(rawInput);
                    count = reader.read8(rawInput);
                    padding = reader.read24(rawInput);
                    timestamp1 = reader.readUnsignedInteger64(rawInput);
                    timestamp2 = reader.readUnsignedInteger64(rawInput);
                    timestamp3 = reader.readUnsignedInteger64(rawInput);
//                    this.ssrc = buffer.readUInt32BE(4, 8);
//                    this.count = buffer.readUInt8(8);
//                    this.padding = (buffer.readUInt8(9) << 0xF0) + buffer.readUInt16BE(10);
//                    this.timestamp1 = buffer.slice(12, 20); //[buffer.readUInt32BE(12), buffer.readUInt32BE(16)];
//                    this.timestamp2 = buffer.slice(20, 28); //[buffer.readUInt32BE(20), buffer.readUInt32BE(24)];
//                    this.timestamp3 = buffer.slice(28, 36); //[buffer.readUInt32BE(28), buffer.readUInt32BE(32)];
                    break;
                case RECEIVER_FEEDBACK:
                    this.valid = true;
                    ssrc = reader.readInteger(rawInput);
                    sequenceNumber = reader.read16(rawInput);
//                    this.ssrc = buffer.readUInt32BE(4, 8);
//                    this.sequenceNumber = buffer.readUInt16BE(8);
                    break;
                case BITRATE_RECEIVE_LIMIT:
                    this.valid = true;
                    break;

            }
        }
        return valid;
    }

    public Boolean isValid() {
        return valid;
    }

    public void createInvitation(int token, int ssrc, String name) {
        this.name = name;
        this.initiator_token = token;
        this.ssrc = ssrc;
        this.protocol_version = 2;
        this.command = INVITATION;
    }

    public void createInvitationAccepted(int token, int ssrc, String name) {
        this.name = name;
        this.initiator_token = token;
        this.ssrc = ssrc;
        this.protocol_version = 2;
        this.command = INVITATION_ACCEPTED;
    }

    public void createInvitationRejected(int token, int ssrc, String name) {
        this.name = name;
        this.initiator_token = token;
        this.ssrc = ssrc;
        this.protocol_version = 2;
        this.command = INVITATION_REJECTED;
    }
    public void createEnd(int token, int ssrc, String name) {
        this.name = name;
        this.initiator_token = token;
        this.ssrc = ssrc;
        this.protocol_version = 2;
        this.command = END;
    }
    public void createSyncronization(int ssrc, int count, long t1, long t2, long t3) {
        this.ssrc = ssrc;
        this.count = count;
        this.timestamp1 = t1;
        this.timestamp2 = t2;
        this.timestamp3 = t3;
        this.command = SYNCHRONIZATION;
    }

    public byte[] generateBuffer() {
        OutDataBuffer buffer = new OutDataBuffer();

        switch(this.command) {
            case INVITATION:
            case INVITATION_ACCEPTED:
            case INVITATION_REJECTED:
            case END:
                buffer.write16(0xFFFF);
                buffer.write16(getCommandKey(this.command));
                buffer.write(this.protocol_version);
                buffer.write(this.initiator_token);
                buffer.write(this.ssrc);
                buffer.write(this.name);
                break;
            case SYNCHRONIZATION:
                buffer.write16(0xFFFF);
                buffer.write16(getCommandKey(this.command));
                buffer.write(this.ssrc);
                buffer.write8(this.count);
                buffer.write24(this.padding);
                buffer.write64(this.timestamp1);
                buffer.write64(this.timestamp2);
                buffer.write64(this.timestamp3);
                break;
            case RECEIVER_FEEDBACK:
                buffer.write16(0xFFFF);
                buffer.write16(getCommandKey(this.command));
                buffer.write(this.ssrc);
                buffer.write(this.sequenceNumber);
                break;
            default:
                return null;
        }
        return buffer.toByteArray();
    }

    private static final Map<Integer, AppleMIDICommand> commandMap = new HashMap<Integer, AppleMIDICommand>();
    static {
        commandMap.put(0x494E, INVITATION);
        commandMap.put(0x4F4B, INVITATION_ACCEPTED);
        commandMap.put(0x4E4F, INVITATION_REJECTED);
        commandMap.put(0x4259, END);
        commandMap.put(0x434B, SYNCHRONIZATION);
        commandMap.put(0x5253, RECEIVER_FEEDBACK);
        commandMap.put(0x524C, BITRATE_RECEIVE_LIMIT);
    }

    private Integer getCommandKey(AppleMIDICommand c){
        for(Integer key : commandMap.keySet()){
            if(commandMap.get(key).equals(c)){
                return key; //return the first found
            }
        }
        return null;
    }

    public enum AppleMIDICommand {
        NOOP,
        INVITATION,
        INVITATION_ACCEPTED,
        INVITATION_REJECTED,
        END,
        SYNCHRONIZATION,
        RECEIVER_FEEDBACK,
        BITRATE_RECEIVE_LIMIT
    }

    public void dumppacket() {
        Log.d("MIDIControl","------------------------------");
        Log.d("MIDIControl","command: "+this.command.toString());
        switch(this.command) {
            case INVITATION:
                Log.d("MIDIControl","protocol_version : "+this.protocol_version);
                Log.d("MIDIControl","initiator_token : "+ String.format("0x%X",this.initiator_token));
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                Log.d("MIDIControl","name : "+this.name);
                break;
            case INVITATION_ACCEPTED:
                Log.d("MIDIControl","protocol_version : "+this.protocol_version);
                Log.d("MIDIControl","initiator_token : "+ String.format("0x%X",this.initiator_token));
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                Log.d("MIDIControl","name : "+this.name);
                break;
            case INVITATION_REJECTED:
                Log.d("MIDIControl","protocol_version : "+this.protocol_version);
                Log.d("MIDIControl","initiator_token : "+ String.format("0x%X",this.initiator_token));
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                Log.d("MIDIControl","name : "+this.name);
                break;
            case RECEIVER_FEEDBACK:
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                Log.d("MIDIControl","name : "+this.sequenceNumber);
                break;
            case BITRATE_RECEIVE_LIMIT:
                break;
            case END:
                Log.d("MIDIControl","protocol_version : "+this.protocol_version);
                Log.d("MIDIControl","initiator_token : "+ String.format("0x%X",this.initiator_token));
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                break;
            case SYNCHRONIZATION:
                Log.d("MIDIControl","ssrc : "+ String.format("0x%X",this.ssrc));
                Log.d("MIDIControl","count : "+this.count);
                Log.d("MIDIControl","padding : "+ String.format("0x%X",this.padding));
                Log.d("MIDIControl","ts1 : "+ String.format("0x%X",this.timestamp1));
                Log.d("MIDIControl","ts2 : "+ String.format("0x%X",this.timestamp2));
                Log.d("MIDIControl","ts3 : "+ String.format("0x%X",this.timestamp3));

                break;
            default:
                Log.d("MIDIControl","unknown packet");
                break;
        }
        Log.d("MIDIControl","------------------------------");
    }
}



