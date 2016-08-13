package com.disappointedpig.midi;


import com.disappointedpig.midi.internal_events.PacketEvent;
import com.disappointedpig.midi.utility.DataBuffer;
import com.disappointedpig.midi.utility.DataBufferReader;
import com.disappointedpig.midi.utility.OutDataBuffer;

public class RTPMessage {

    int version = 2;
    boolean padding = false;
    boolean hasExtension = false;
    int csrcCount = 0;
    boolean marker = false;
    int payloadType = 0x61;
    int sequenceNumber = 0;
    int timestamp = 0;
    int ssrc = 0;
    byte[] payload;
    int payload_length;

    public boolean parse(PacketEvent packet) {

        final DataBuffer rawInput = new DataBuffer(packet.getData(),packet.getLength());
        final DataBufferReader reader = new DataBufferReader();

        int firstByte = reader.read8(rawInput);

        this.version = firstByte >>> 6;
        this.padding = ((firstByte >>> 5 & 1) != 0 );
        this.hasExtension = ((firstByte >>> 4 & 1) != 0);
        this.csrcCount = firstByte & 0xF;

        int secondByte = reader.read8(rawInput);

        this.marker = (secondByte & 0x80) == 0x80;
        this.payloadType = secondByte & 0x7f;

        this.sequenceNumber = reader.read16(rawInput);
        this.timestamp = reader.readInteger(rawInput);
        this.ssrc = reader.readInteger(rawInput);

        int block3 = reader.read8(rawInput);
        boolean bflag = (block3 >> 7 & 1) != 0;
        boolean jflag = ((block3 >> 6 & 1) & 0x1) != 0;
        boolean zflag = ((block3 >> 5 & 1) & 0x1) != 0;
        boolean pflag = ((block3 >> 4 & 1) & 0x1) != 0;
        int command_length = block3 & 0x7;

        this.payload = rawInput.slice(rawInput.getStreamPosition());
        this.payload_length = rawInput.getBytesLength() - rawInput.getStreamPosition();
        return true;
    }

    public OutDataBuffer generatePayload() {
        OutDataBuffer buffer = new OutDataBuffer();

        int firstByte = 0;
        firstByte |= this.version << 6;
        firstByte |= this.padding ? 0x20 : 0;
        firstByte |= this.hasExtension ? 0x10 : 0;
        // csrcs = 0... so just skip this
//        firstByte |= (this.csrcs.length > 15 ? 15 : this.csrcs.length);

        int secondByte = this.payloadType | (this.marker ? 0x80 : 0);
        buffer.write8(firstByte);
        buffer.write8(secondByte);
        buffer.write16(sequenceNumber);
        long t = MIDISession.getInstance().getNow();
//        Log.e("RTPMessage","t:"+t+" t8:"+(t >>> 8)+" t16:"+(t >>>16)+" tint:"+(int)t);
//        timestamp = (int)t >>> 8;
        timestamp = (int)t;
        buffer.write32(timestamp << 0);
        buffer.write32(ssrc);


        return buffer;
    }
}
