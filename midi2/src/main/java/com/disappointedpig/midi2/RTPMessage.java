package com.disappointedpig.midi2;


import com.disappointedpig.midi2.events.MIDI2PacketEvent;
import com.disappointedpig.midi2.utility.DataBuffer;
import com.disappointedpig.midi2.utility.DataBufferReader;

public class RTPMessage {

    int version = 2;
    boolean padding = false;
    boolean hasExtension = false;
    int csrcCount = 0;
    boolean marker = false;
    int payloadType = 0;
    int sequenceNumber = 0;
    int timestamp = 0;
    int ssrc = 0;

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


}
