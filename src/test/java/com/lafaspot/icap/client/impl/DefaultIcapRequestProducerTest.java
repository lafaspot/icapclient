package com.lafaspot.icap.client.impl;

import com.lafaspot.icap.client.AbstractIcapRequestProducer;
import com.lafaspot.icap.client.codec.IcapRespmod;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.net.URI;


public class DefaultIcapRequestProducerTest {

    @Test
    public void testRespMod() {
        final String expected = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0\r\n" + "Host: 127.0.0.1\r\n"
                + "Connection: keep-alive\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19\r\n" + "\r\n"
                + "GET virus.msg HTTP/1.1\r\n" + "Host: 127.0.0.1\r\n" + "\r\n" + "HTTP/1.1 200 OK\r\n" + "\r\n" + "f\r\n" + "\r\n";

        final byte[] inBuffer = { '0', '1', '2', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        final String filename = "virus.msg";
        final boolean keepAlive = true;
        final URI uri = URI.create("icap://127.0.0.1:1344");

        DefaultIcapRequestProducer defaultIcapRequestProducer = new DefaultIcapRequestProducer(uri, filename, inBuffer);
        IcapRespmod respmod = defaultIcapRequestProducer.generateRespMod(true);
        Assert.assertNotNull(respmod);
        Assert.assertNotEquals(respmod.getRespModString().length(), expected.length());
        int startIndex = 0;
        for(startIndex=0; startIndex < inBuffer.length; startIndex++) {
            Assert.assertEquals(respmod.getInStream()[startIndex], inBuffer[startIndex]);
        }
        for(int i = 0; startIndex < respmod.getInStream().length; startIndex++) {
            Assert.assertEquals(respmod.getInStream()[startIndex], AbstractIcapRequestProducer.TRAILER_BYTES[i++]);
        }

    }
}