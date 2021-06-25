package com.lafaspot.icap.client.impl;

import com.lafaspot.icap.client.AbstractIcapRequestProducer;
import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * @author nimmyr
 * A Default ICAP Request producer for any anitvirus engine
 */
public class DefaultIcapRequestProducer extends AbstractIcapRequestProducer {

    private static final String SERVICE_NAME = "SYMCScanResp-AV";

    /**
     * Constructor.
     * @param uri server uri
     * @param fileName name of the file to be scanned
     * @param dataToScan file data to be scanned
     */
    public DefaultIcapRequestProducer(@Nonnull final URI uri, @Nonnull final String fileName, @Nonnull final byte[] dataToScan) {
        super(uri, fileName, dataToScan);
    }

    @Override
    public IcapOptions generateOptions() {
        return new IcapOptions(uri, SERVICE_NAME);
    }

    @Override
    public IcapRespmod generateRespMod(final boolean keepAlive) {
        byte[] copiedStream = new byte[dataToScan.length + TRAILER_BYTES.length];
        System.arraycopy(dataToScan, 0, copiedStream, 0, dataToScan.length);
        System.arraycopy(TRAILER_BYTES, 0, copiedStream, dataToScan.length, TRAILER_BYTES.length);
        return new IcapRespmod(uri, constructIcapRespMod(SERVICE_NAME, keepAlive), copiedStream, TRAILER_BYTES);
    }

}
