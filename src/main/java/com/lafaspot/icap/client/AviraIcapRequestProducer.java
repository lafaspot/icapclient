package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapRespmod;

import java.net.URI;

public class AviraIcapRequestProducer extends AbstractIcapRequestProducer {

    public AviraIcapRequestProducer(URI uri, String fileName, byte[] dataToScan, boolean reuseSession) {
        super(uri, fileName, dataToScan, reuseSession);
    }

    @Override
    public IcapRespmod generateRespMod() {
        byte[] copiedStream = new byte[dataToScan.length];
        System.arraycopy(dataToScan, 0, copiedStream, 0, dataToScan.length);
        return new IcapRespmod(uri, constructIcapRespMod(uri,fileName, dataToScan, reuseSession), copiedStream, TRAILER_BYTES);
    }

}
