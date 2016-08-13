package com.disappointedpig.midi.utility;

import java.math.BigInteger;


public class DataBufferReader {

    public int read16(final DataBuffer rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));

        return (firstByte << 8
                | secondByte)
                & 0xFFFF;
    }

    /**
     * Get the length of the string currently in the byte stream.
     */
    public int lengthOfCurrentString(final DataBuffer rawInput) {
        int len = 0;
        while (rawInput.getBytes()[rawInput.getStreamPosition() + len] != 0) {
            len++;
        }
        return len;
    }
    /**
     * Move to the next byte with an index in the byte array
     * which is dividable by four.
     */
    public void moveToFourByteBoundry(final DataBuffer rawInput) {
        // If i am already at a 4 byte boundry, I need to move to the next one
        final int mod = rawInput.getStreamPosition() % 4;
        rawInput.addToStreamPosition(4 - mod);
    }

    public String readString(final DataBuffer rawInput) {
        final int strLen = lengthOfCurrentString(rawInput);
//        final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen, charset);
        final String res = new String(rawInput.getBytes(), rawInput.getStreamPosition(), strLen);
        rawInput.addToStreamPosition(strLen);
        moveToFourByteBoundry(rawInput);
        return res;
    }

    /**
     * Reads an Integer (32 bit integer) from the byte stream.
     * @return an {@link Integer}
     */
    public Integer readInteger(final DataBuffer rawInput) {
        final BigInteger intBits = readBigInteger(rawInput, 4);
        return intBits.intValue();
    }

    public BigInteger readBigInteger(final DataBuffer rawInput, final int numBytes) {
        final byte[] myBytes = new byte[numBytes];
        System.arraycopy(rawInput.getBytes(), rawInput.getStreamPosition(), myBytes, 0, numBytes);
        rawInput.addToStreamPosition(numBytes);
        return  new BigInteger(myBytes);
    }

    public int read8(final DataBuffer rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return (firstByte & 0xFF);
    }

    public int read24(final DataBuffer rawInput) {
        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return ((int) (firstByte << 16
                | secondByte << 8
                | thirdByte))
                & 0xFFFFFF;
    }

    public Long readUnsignedInteger(final DataBuffer rawInput) {

        final int firstByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int secondByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int thirdByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        final int fourthByte = (0x000000FF & ((int) rawInput.getBytes()[rawInput.getAndIncreaseStreamPositionByOne()]));
        return ((long) (firstByte << 24
                | secondByte << 16
                | thirdByte << 8
                | fourthByte))
                & 0xFFFFFFFFL;
    }

    public long readUnsignedInteger64(final DataBuffer rawInput) {

        long highword = readUnsignedInteger(rawInput);
        long lowword = readUnsignedInteger(rawInput);

        long x = highword;
        x <<= 32;
        x += lowword;
        x &= 0xFFFFFFFFFFFFFFFFL;
        return x;
    }

}
