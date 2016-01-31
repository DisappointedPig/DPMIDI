package com.disappointedpig.midi;

import com.disappointedpig.midi.utility.MIDIJavaToByteArrayConverter;

import java.nio.charset.Charset;

abstract class AbstractMIDIPacket implements  MIDIPacket {
    private Charset charset;
    private byte[] byteArray;

    public AbstractMIDIPacket() {
        this.charset = Charset.defaultCharset();
        this.byteArray = null;
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    @Override
    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    private byte[] computeByteArray() {
        final MIDIJavaToByteArrayConverter stream = new MIDIJavaToByteArrayConverter();
        stream.setCharset(charset);
        return computeByteArray(stream);
    }

    protected abstract byte[] computeByteArray(MIDIJavaToByteArrayConverter stream);

    @Override
    public byte[] getByteArray() {
        if (byteArray == null) {
            byteArray = computeByteArray();
        }
        return byteArray;
    }

    protected void contentChanged() {
        byteArray = null;
    }

}

