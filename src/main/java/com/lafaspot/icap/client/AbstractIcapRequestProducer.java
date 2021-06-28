package com.lafaspot.icap.client;

import javax.annotation.Nonnull;
import java.net.URI;

/**
 * A Generic Icap request message producer.
 * @author nimmyr
 */
public abstract class AbstractIcapRequestProducer implements IcapRequestProducer {

    /**
     * Constructor to create request producer.
     * @param uri uri of Icap server
     * @param fileName name of the file to be scanned
     * @param dataToScan file data to be scanned
     */
    public AbstractIcapRequestProducer(@Nonnull final URI uri, @Nonnull final String fileName, @Nonnull final byte[] dataToScan) {
        this.uri = uri;
        this.fileName = fileName;
        this.dataToScan = dataToScan;
    }


    /**
     * Called to create a RESP MOD message.
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc3507#page-27">https://datatracker.ietf.org/doc/html/rfc3507#page-27</a>
     * @param keepAlive KeepAlive flag
     * @return Returns a RESP MOD message
     */
    protected String constructIcapRespMod(@Nonnull final String serviceName, final boolean keepAlive) {
        final StringBuffer buf = new StringBuffer();
        buf.append("RESPMOD icap://");
        buf.append(uri.getHost());
        buf.append(":");
        buf.append(uri.getPort());
        buf.append("/").append(serviceName).append(" ICAP/1.0\r\n");
        buf.append("Host: ");
        buf.append(uri.getHost());
        buf.append("\r\n");
        buf.append("Connection: ");
        if (keepAlive) {
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
    public static final byte[] TRAILER_BYTES = { '\r', '\n', '0', '\r', '\n' };

    /** Server URI. */
    protected final URI uri;

    /** Name of the file to be scanned. */
    protected final String fileName;

    /** File data to be scanned. */
    protected final byte[] dataToScan;

}
