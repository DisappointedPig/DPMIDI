package com.disappointedpig.midi2;


import com.disappointedpig.midi2.events.MIDI2PacketEvent;
import com.disappointedpig.midi2.utility.DataBuffer;
import com.disappointedpig.midi2.utility.DataBufferReader;
import com.disappointedpig.midi2.utility.OutDataBuffer;

public class MIDI2Message extends RTPMessage {

    private Boolean valid;
    private DataBuffer m;

    private boolean firstHasDeltaTime;

    private int channel_status;
    private int channel;
    private int note;
    private int velocity;

    public MIDI2Message() {
    }

    public boolean parseMessage(MIDI2PacketEvent packet) {
        this.valid = false;
        parse(packet);
        final DataBufferReader reader = new DataBufferReader();
        final DataBuffer rawPayload = new DataBuffer(payload, payload_length);

        // payload should contain command + journal
        int block4 = reader.read8(rawPayload);
        channel_status = block4 >> 4;
        channel = block4 & 0xf;
        int block5 = reader.read8(rawPayload);
        note = block5 & 0x7f;
        int block6 = reader.read8(rawPayload);
        velocity = block6 & 0x7f;

//        Log.d("MIDI2Message", "cs:" + channel_status + " c:" + channel + " n:" + note + " v" + velocity);
        return true;
    }

    public void createNote(int note, int velocity) {
        this.note = note;
        this.velocity = velocity;
    }

    public byte[] generateBuffer() {
        OutDataBuffer buffer = generatePayload();

//        buffer.write8(0x00);
        buffer.write16(0x0390);
        buffer.write8(note);
        buffer.write8(velocity);
        return buffer.toByteArray();
    }
}