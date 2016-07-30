package com.disappointedpig.midi2;

import com.disappointedpig.midi2.events.MIDI2PacketEvent;
import com.disappointedpig.midi2.utility.DataBuffer;
import com.disappointedpig.midi2.utility.DataBufferReader;
import com.disappointedpig.midi2.utility.OutDataBuffer;

import java.util.HashMap;
import java.util.Map;

import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.BITRATE_RECEIVE_LIMIT;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.END;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.INVITATION;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.INVITATION_ACCEPTED;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.INVITATION_REJECTED;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.RECEIVER_FEEDBACK;
import static com.disappointedpig.midi2.MIDI2Control.AppleMIDICommand.SYNCHRONIZATION;

public class MIDI2Control {

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


    public MIDI2Control() {}

    public boolean parse(MIDI2PacketEvent packet) {
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
        commandMap.put(0x4E4F, INVITATION_ACCEPTED);
        commandMap.put(0x4F4B, INVITATION_REJECTED);
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


}



