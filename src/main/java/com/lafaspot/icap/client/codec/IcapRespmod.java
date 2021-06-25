/**
 *
 */
package com.lafaspot.icap.client.codec;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * Command to send RESPMOD request to AV scan server.
 *
 * @author kraman
 * @author nimmyr
 *
 */
public class IcapRespmod extends IcapRequest {

    /** Bytes denoting the end of the message. */
    private static final byte[] END_OF_MESSAGE = { '\r', '\n' };
    /** Bytes to be scanned. */
    protected final byte[] inBuffer;
    /** The ICAP message. */
    private final String respModString;
    /** Bytes denoting the end of the message. */
    private final byte[] trailerBytes;

    /**
     * Constructs a ICAP RESPMODE command.
     *
     * @param uri symantec server uri
     * @param inBuffer bytes to be scanned
     * @param respModString  resp mod message string
     * @param trailerBytes trailer bytes
     */
    public IcapRespmod(@Nonnull final URI uri, @Nonnull final String respModString, final byte[] inBuffer, final byte[] trailerBytes) {

        // buf.append("\r\n");
        super(uri);
        this.respModString = respModString;
        this.inBuffer = inBuffer;
        this.trailerBytes = trailerBytes;
    }

    /**
     * Return the ICAP headers as string.
     *
     * @return ICAP RESPMOD message
     */
    public String getRespModString() {
        return respModString;
    }

    /**
     * Return the bytes to be scanned.
     *
     * @return byte stream to be scanned
     */
    public byte[] getInStream() {
        return inBuffer;
    }

    /**
     * Returns the trailer bytes.
     *
     * @return byte stream denoting the endof the message.
     */
    public byte[] getTrailerBytes() {
        return trailerBytes;
    }

    /**
     * Returns the trailer bytes.
     *
     * @return byte stream denoting the endof the message.
     */
    public byte[] getEndOfMessage() {
        return END_OF_MESSAGE;
    }

}
