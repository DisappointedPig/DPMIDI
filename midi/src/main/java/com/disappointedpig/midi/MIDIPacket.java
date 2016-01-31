package com.disappointedpig.midi;

import java.nio.charset.Charset;

public interface MIDIPacket {
    /**
     * Returns the character set used by this packet.
     * @return the character set used to encode message addresses and string
     *   arguments.
     */
    Charset getCharset();

    /**
     * Sets the character set used by this packet.
     * @param charset used to encode message addresses and string arguments.
     */
    void setCharset(Charset charset);

    /**
     * Return the OSC byte stream for this packet.
     * @return byte[]
     */
    byte[] getByteArray();

}