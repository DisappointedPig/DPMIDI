package com.disappointedpig.midi2;


import com.disappointedpig.midi2.events.MIDI2PacketEvent;
import com.disappointedpig.midi2.utility.DataBuffer;
import com.disappointedpig.midi2.utility.DataBufferReader;
import com.disappointedpig.midi2.utility.OutDataBuffer;

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
    int[] csrcs;
    byte[] payload;
    int payload_length;

    public boolean parse(MIDI2PacketEvent packet) {

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

        this.sequenceNumber = reader.read16(rawInput); //buffer.readUInt16BE(2);
        this.timestamp = reader.readInteger(rawInput); //buffer.readUInt32BE(4);
        this.ssrc = reader.readInteger(rawInput); //buffer.readUInt32BE(8);
//        currentOffset = 12;

//        for (i = 0; i < this.csrcCount; i++) {
//            this.csrcs.push(buffer.readUInt32BE(currentOffset));
//            i++;
//        }
        int block3 = reader.read8(rawInput);
        boolean bflag = (block3 >> 7 & 1) != 0;
        boolean jflag = ((block3 >> 6 & 1) & 0x1) != 0;
        boolean zflag = ((block3 >> 5 & 1) & 0x1) != 0;
        boolean pflag = ((block3 >> 4 & 1) & 0x1) != 0;
        int command_length = block3 & 0x7;

//        if (this.hasExtension) {
//            this.extensionHeaderId = buffer.readUInt16BE(currentOffset);
//            currentOffset += 2;
//            this.extensionHeaderLength = buffer.readUInt16BE(currentOffset);
//            currentOffset += 2;
//            this.extension = buffer.slice(currentOffset, currentOffset += this.extensionHeaderLength / 32);
//        }
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
        long t = MIDI2Session.getInstance().getNow();
//        Log.e("RTPMessage","t:"+t+" t8:"+(t >>> 8)+" t16:"+(t >>>16)+" tint:"+(int)t);
        timestamp = (int)t >>> 8;
        buffer.write32(timestamp << 0);
        buffer.write32(ssrc);

        // ---------------------------------

//        for (int i = 0; i < this.csrcs && i < 15; i++) {
//            buffer.write32(this.csrcs[i], 12 + (4 * i));
//        }

//        if (this.hasExtension) {
//            length = Math.ceil(this.extension.length / 32);
//            buffer.write16(this.extensionHeaderId, 12 + (4 * i));
//            buffer.write16(length, 14 + (4 * i));
//            this.extension.copy(buffer, 16 + (4 * i));
//        }

//        buffer.write16(0x03);
//        buffer.write8(0x09);
//        buffer.write8(0x00);
//        buffer.write16(note);
//        buffer.write16(velocity);
        return buffer;
    }
}
