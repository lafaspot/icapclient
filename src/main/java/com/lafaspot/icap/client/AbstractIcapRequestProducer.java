package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapOptions;

import java.net.URI;

public abstract class AbstractIcapRequestProducer implements IcapRequestProducer{

    public AbstractIcapRequestProducer(URI uri, String fileName, byte[] dataToScan, boolean reuseSession) {
        this.uri = uri;
        this.fileName = fileName;
        this.dataToScan = dataToScan;
        this.reuseSession = reuseSession;
    }

    @Override
    public IcapOptions generateOptions() {
        return new IcapOptions(uri);
    }

    String constructIcapRespMod(URI uri, String fileName, byte[] dataToScan, boolean reuseSession) {
        final StringBuffer buf = new StringBuffer();
        buf.append("RESPMOD icap://");
        buf.append(uri.getHost());
        buf.append(":");
        buf.append(uri.getPort());
        buf.append("/").append(uri.getPath()).append(" ICAP/1.0\r\n");
        buf.append("Host: ");
        buf.append(uri.getHost());
        buf.append("\r\n");
        buf.append("Connection: ");
        if (reuseSession) {
            buf.append("keep-alive\r\n");
        } else {
            buf.append("close\r\n");
        }

        //req header
        final StringBuffer resHdr = new StringBuffer();
        resHdr.append("GET ");
        resHdr.append("/" + fileName);
        resHdr.append(" HTTP/1.1\r\n");
        resHdr.append("Host: ");
        resHdr.append(uri.getHost());
        resHdr.append(":");
        resHdr.append(uri.getPort());
        resHdr.append("\r\n");
        resHdr.append("\r\n");

        // res hdr
        final StringBuffer resBody = new StringBuffer();
        resBody.append("HTTP/1.1 200 OK");
        resBody.append("\r\n");
        resBody.append("\r\n");

        int resHdrLen = resHdr.length();
        int resBodyLen = resHdrLen + resBody.length();

        //encapsulated header
        buf.append("Encapsulated: req-hdr=0, res-hdr=");
        buf.append(resHdrLen);
        buf.append(", res-body=");
        buf.append(resBodyLen);
        buf.append("\r\n");
        buf.append("\r\n");

        buf.append(resHdr);

        buf.append(resBody);

        buf.append(Integer.toHexString(dataToScan.length));
        buf.append("\r\n");
        return buf.toString();
    }

    /** Bytes denoting the end of the message. */
    protected static final byte[] TRAILER_BYTES = { '\r', '\n', '0', '\r', '\n' };

    protected URI uri;

    protected String fileName;

    protected byte[] dataToScan;

    protected boolean reuseSession;
}
