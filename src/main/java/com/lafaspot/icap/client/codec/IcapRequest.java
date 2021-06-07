package com.lafaspot.icap.client.codec;

import java.net.URI;

public class IcapRequest {

    public IcapRequest(final URI uri) {
        this.uri = uri;
    }

    private URI uri;
}
