package com.disappointedpig.midi;


import android.os.Bundle;
import android.util.Log;

import com.disappointedpig.midi.internal_events.PacketEvent;
import com.disappointedpig.midi.utility.DataBuffer;
import com.disappointedpig.midi.utility.DataBufferReader;
import com.disappointedpig.midi.utility.OutDataBuffer;

public class MIDIMessage extends RTPMessage {

    private Boolean valid;
    private DataBuffer m;

    private boolean firstHasDeltaTime;

    private int channel_status;
    private int channel;
    private int note;
    private int velocity;

    public static MIDIMessage newUsing(int cs, int c, int n, int v) {
        MIDIMessage m = new MIDIMessage();
        m.createNote(cs,c,n,v);
        return m;
    }

    public static MIDIMessage newUsing(Bundle m) {
        return newUsing(   m.getInt(Consts.MSG_COMMAND,0x09),
                    m.getInt(Consts.MSG_CHANNEL,0),
                    m.getInt(Consts.MSG_NOTE,0),
                    m.getInt(Consts.MSG_VELOCITY,0));
    }


    public MIDIMessage() {
    }

    public boolean parseMessage(PacketEvent packet) {
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

        this.valid = true;

        Log.d("MIDIMessage", "cs:" + channel_status + " c:" + channel + " n:" + note + " v" + velocity);
        return true;
    }

    public Bundle toBundle() {
        Bundle midi = new Bundle();
        midi.putInt(Consts.MSG_COMMAND,this.channel_status);
        midi.putInt(Consts.MSG_CHANNEL,this.channel);
        midi.putInt(Consts.MSG_NOTE, this.note);
        midi.putInt(Consts.MSG_VELOCITY, this.velocity);
        return midi;
    }

    public void createNote(int channel_status, int channel, int note, int velocity) {
        this.channel_status = channel_status;
        this.channel = channel;
        this.note = note;
        this.velocity = velocity;
    }
    public void createNote(int note, int velocity) {
        this.note = note;
        this.velocity = velocity;
    }

    public Boolean isValid() {
        return valid;
    }

    public byte[] generateBuffer() {
        OutDataBuffer buffer = generatePayload();
// TODO : this doesn't handle channel_status or channel correctly
//        buffer.write8(0x00);
        buffer.write16(0x0390);
        buffer.write8(note);
        buffer.write8(velocity);
        return buffer.toByteArray();
    }
}