/**
 *
 */
package com.lafaspot.icap.client.codec;

import java.net.URI;

import javax.annotation.Nonnull;

/**
 * Command to send RESPMOD request to AV scan server.
 *
 * @author kraman
 *
 */
public class IcapRespmod extends IcapRequest{


    /** The ICAP message. */
    private final String icapMessage;

    /** Bytes to be scanned. */
    protected final byte[] inBuffer;

    /**
     * Constructs a ICAP RESPMODE command.
     *
     * @param uri symantec server uri
     * @param inBuffer bytes to be scanned
     */
    public IcapRespmod(@Nonnull final URI uri, @Nonnull final String icapMessage, final byte[] inBuffer, final byte[] trailerBytes) {

        // buf.append("\r\n");
        super(uri);
        this.icapMessage = icapMessage;
        this.inBuffer = inBuffer;
        this.trailerBytes = trailerBytes;
    }

    /**
     * Return the ICAP headers as string.
     *
     * @return ICAP RESPMOD message
     */
    public String getIcapMessage() {
        return icapMessage;
    }

    /**
     * Return the bytes to be scanned.
     *
     * @return byte stream to be scanned
     */
    public byte[] getInStream() {
        return inBuffer;
    }

    /** Bytes denoting the end of the message. */
    private byte[] trailerBytes ;

    /**
     * Returns the trailer bytes.
     *
     * @return byte stream denoting the endof the message.
     */
    public byte[] getTrailerBytes() {
        return trailerBytes;
    }

    /** Bytes denoting the end of the message. */
    private static final byte[] END_OF_MESSAGE = { '\r', '\n' };

    /**
     * Returns the trailer bytes.
     *
     * @return byte stream denoting the endof the message.
     */
    public byte[] getEndOfMessage() {
        return END_OF_MESSAGE;
    }

}
