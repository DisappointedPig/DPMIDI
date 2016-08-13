package com.disappointedpig.midi.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;


public class OutDataBuffer {
    private final ByteArrayOutputStream stream;
    private Charset charset;

    private final byte[] charByte;
    private final byte[] shortBytes;
    private final byte[] mediumBytes;
    private final byte[] intBytes;
    private final byte[] longintBytes;

    public OutDataBuffer() {
        this.stream = new ByteArrayOutputStream();
        this.charset = Charset.defaultCharset();
        this.charByte = new byte[1];
        this.shortBytes = new byte[2];
        this.mediumBytes = new byte[3];
        this.intBytes = new byte[4];
        this.longintBytes = new byte[8];
    }

    private void alignStream() {
        final int alignmentOverlap = stream.size() % 4;
        final int padLen = (4 - alignmentOverlap) % 4;
        for (int pci = 0; pci < padLen; pci++) {
            stream.write(0);
        }
    }

    public void write(int anInt) {
        writeInteger32ToByteArray(anInt);
    }

    public void write(Float aFloat) {
        writeInteger32ToByteArray(Float.floatToIntBits(aFloat));
    }

    public void write(Double aDouble) {
        writeInteger64ToByteArray(Double.doubleToRawLongBits(aDouble));
    }

    public void write(Integer anInt) {
        writeInteger32ToByteArray(anInt);
    }

    public void write16(Integer anInt) {
        writeInteger16ToByteArray(anInt);
    }

    public void write8(Integer anInt) {
        writeInteger8ToByteArray(anInt);
    }

    public void write24(Integer anInt) {
        writeInteger24ToByteArray(anInt);
    }

    public void write32(Integer anInt) {
        writeInteger32ToByteArray(anInt);
    }

    public void write64(Long aLong) {
        writeInteger64ToByteArray(aLong);
    }

    public void write(String aString) {
//        final byte[] stringBytes = aString.getBytes(charset);
        final byte[] stringBytes = aString.getBytes();
        writeUnderHandler(stringBytes);
        stream.write(0);
        alignStream();
    }


    private void writeUnderHandler(byte[] bytes) {

        try {
            stream.write(bytes);
        } catch (IOException ex) {
            throw new RuntimeException("You're screwed:"
                    + " IOException writing to a ByteArrayOutputStream", ex);
        }
    }

    /**
     * Write a 32 bit integer to the byte array without allocating memory.
     *
     * @param value a 32 bit integer.
     */
    private void writeInteger32ToByteArray(int value) {
        //byte[] intBytes = new byte[4];
        //I allocated the this buffer globally so the GC has less work

        intBytes[3] = (byte) value;
        value >>>= 8;
        intBytes[2] = (byte) value;
        value >>>= 8;
        intBytes[1] = (byte) value;
        value >>>= 8;
        intBytes[0] = (byte) value;

        writeUnderHandler(intBytes);
    }

    private void writeInteger16ToByteArray(int value) {
//        Log.d("converter","("+Integer.toBinaryString(value)+") "+ String.format("%02x",value & 0x000000FF) +" "+String.format("%02x",(value>>>8 & 0x000000FF)));
        shortBytes[1] = (byte) ((value & 0x000000FF));
//        Log.d("-----1","("+Integer.toBinaryString(shortBytes[1])+") "+ String.format("%02x", shortBytes[1]));

        shortBytes[0] = (byte) (value >>> 8 & 0x000000FF);
//        Log.d("-----0","("+Integer.toBinaryString(shortBytes[0])+") "+ String.format("%02x", shortBytes[0]));
//        Log.d("-----:"," "+ (shortBytes.length));
        writeUnderHandler(shortBytes);
    }


    private void writeInteger8ToByteArray(int value) {
        charByte[0] = (byte) ((value & 0x000000FF));
        writeUnderHandler(charByte);
    }

    private void writeInteger24ToByteArray(int value) {
        mediumBytes[2] = (byte) ((value & 0x000000FF));
        mediumBytes[1] = (byte) (value >>> 8 & 0x000000FF);
        mediumBytes[0] = (byte) (value >>> 8 & 0x000000FF);
        writeUnderHandler(mediumBytes);
    }

    /**
     * Write a 64 bit integer to the byte array without allocating memory.
     *
     * @param value a 64 bit integer.
     */
    private void writeInteger64ToByteArray(long value) {
        longintBytes[7] = (byte) value;
        value >>>= 8;
        longintBytes[6] = (byte) value;
        value >>>= 8;
        longintBytes[5] = (byte) value;
        value >>>= 8;
        longintBytes[4] = (byte) value;
        value >>>= 8;
        longintBytes[3] = (byte) value;
        value >>>= 8;
        longintBytes[2] = (byte) value;
        value >>>= 8;
        longintBytes[1] = (byte) value;
        value >>>= 8;
        longintBytes[0] = (byte) value;

        writeUnderHandler(longintBytes);
    }

    public byte[] toByteArray() {
        return stream.toByteArray();
    }

}
