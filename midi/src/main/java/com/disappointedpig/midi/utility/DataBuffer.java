package com.disappointedpig.midi.utility;

import java.util.Arrays;

public class DataBuffer {

    private final byte[] bytes;
    private final int bytesLength;
    private int streamPosition;

    public DataBuffer(final byte[] bytes, final int bytesLength) {

        this.bytes = bytes;
        this.bytesLength = bytesLength;
        this.streamPosition = 0;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getBytesLength() {
        return bytesLength;
    }

    public int getAndIncreaseStreamPositionByOne() {
        return streamPosition++;
    }

    public void addToStreamPosition(int toAdd) {
        streamPosition += toAdd;
    }

    public int getStreamPosition() {
        return streamPosition;
    }

    public byte[] slice(int position) {
        return Arrays.copyOfRange(this.bytes,position,bytesLength);
    }
}

