package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapRespmod;

import java.net.URI;

public class SymantecIcapRequestProducer extends AbstractIcapRequestProducer {

    public SymantecIcapRequestProducer(final URI uri, final String fileName, final byte[] dataToScan, final boolean reuseSession) {
        super(uri, fileName, dataToScan, reuseSession);
    }

    @Override
    public IcapRespmod generateRespMod() {
        byte[] copiedStream = new byte[dataToScan.length + TRAILER_BYTES.length];
        System.arraycopy(dataToScan, 0, copiedStream, 0, dataToScan.length);
        System.arraycopy(TRAILER_BYTES, 0, copiedStream, dataToScan.length, TRAILER_BYTES.length);
        return new IcapRespmod(uri, constructIcapRespMod(uri, fileName, dataToScan, reuseSession), copiedStream, TRAILER_BYTES);
    }

}
