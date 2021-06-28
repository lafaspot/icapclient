/**
 *
 */
package com.lafaspot.icap.client.codec;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * Defines the ICAP OPTIONS command.
 *
 * @author kraman
 * @author nimmyr
 *
 */
public class IcapOptions extends IcapRequest {

    /** The actual ICAP OPTIONS message. */
    private final String message;

    /**
     * Constructor to build an ICAP OPTIONS command.
     *
     * @param uri Symantec server uri
     * @param serviceName service name
     */
    public IcapOptions(@Nonnull final URI uri, @Nonnull final String serviceName) {
        super(uri);
        final StringBuffer buf = new StringBuffer();
        buf.append("OPTIONS icap://");
        buf.append(uri.getHost());
        buf.append("/").append(serviceName).append(" ICAP/1.0\r\n");
        buf.append("Host:");
        buf.append(uri.getHost());
        buf.append("\r\n");
        buf.append("User-Agent: JEDI ");
        buf.append("ICAP Client/1.1.\r\n");
        buf.append("Encapuslated: null-body=0\r\n");
        buf.append("\r\n");

        message = buf.toString();
    }

    /**
     * Returns the String representation of the message.
     *
     * @return ICAP OPTIONS message as string
     */
    public String getMessage() {
        return message;
    }

}
