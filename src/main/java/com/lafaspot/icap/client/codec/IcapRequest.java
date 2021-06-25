package com.lafaspot.icap.client.codec;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * A ICAP request.
 * @author nimmyr
 */
public class IcapRequest {

    /**
     * Constructor to construct ICAP request.
     * @param uri server uri
     */
    public IcapRequest(@Nonnull final URI uri) {
        this.uri = uri;
    }

    /**
     * A server URI.
     */
    private final URI uri;
}
