package com.lafaspot.icap.client.impl;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.exception.IcapException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Mockito.when;

public class DefaultIcapRespConsumerTest {

    @Test
    public void test200Ok() throws IcapException {
        DefaultIcapRespConsumer defaultIcapRespConsumer = new DefaultIcapRespConsumer();
        IcapMessage icapMessage1 = Mockito.mock(IcapMessage.class);
        when(icapMessage1.getIcapHeaders()).thenReturn(new String[] {"", "Encapsulated: req-hdr=0, req-body=23, res-hdr=33, res-body=57"});
        IcapResult result = defaultIcapRespConsumer.responseReceived(200,icapMessage1);
        Assert.assertEquals(result.getDisposition(), IcapResult.Disposition.CLEAN);
        Assert.assertEquals(result.getNumViolations(), 0);

        //Encapsulated: null-body=0
        IcapMessage icapMessage2 = Mockito.mock(IcapMessage.class);
        when(icapMessage2.getIcapHeaders()).thenReturn(new String[] {"", "Encapsulated: null-body=0"});
        result = defaultIcapRespConsumer.responseReceived(200,icapMessage2);
        Assert.assertNull(result.getDisposition());
        Assert.assertEquals(result.getNumViolations(), 0);
    }

    @Test(expectedExceptions = IcapException.class)
    public void test200OkWithNFException() throws IcapException {
        DefaultIcapRespConsumer defaultIcapRespConsumer = new DefaultIcapRespConsumer();
        IcapMessage icapMessage1 = Mockito.mock(IcapMessage.class);
        when(icapMessage1.getIcapHeaders()).thenReturn(new String[] {"", "Encapsulated: req-hdr=0, req-body=23, res-hdr=33, res-body=abc"});
        IcapResult result = defaultIcapRespConsumer.responseReceived(200, icapMessage1);

    }

    @Test
    public void test201Ok() throws IcapException {

        DefaultIcapRespConsumer defaultIcapRespConsumer = new DefaultIcapRespConsumer();
        IcapMessage icapMessage1 = Mockito.mock(IcapMessage.class);
        when(icapMessage1.getIcapHeaders()).thenReturn(new String[] {"","X-Violations-Found: 1","virus.msg","W32.Beagle.AO@mm","18411","0",
                "Encapsulated: res-hdr=0, res-body=83"});
        IcapResult result = defaultIcapRespConsumer.responseReceived(201, icapMessage1);
        Assert.assertEquals(result.getDisposition(), IcapResult.Disposition.INFECTED_UNREPAIRED);
        Assert.assertEquals(result.getNumViolations(), 1);
        Assert.assertEquals(result.getViolationFilename(),"virus.msg");
        Assert.assertEquals(result.getViolationName(),"W32.Beagle.AO@mm");
        Assert.assertEquals(result.getViolationId(),"18411");

    }

    @Test(expectedExceptions = IcapException.class)
    public void test201OkException() throws IcapException {
    DefaultIcapRespConsumer defaultIcapRespConsumer = new DefaultIcapRespConsumer();
        IcapMessage icapMessage1 = Mockito.mock(IcapMessage.class);
        when(icapMessage1.getIcapHeaders()).thenReturn(new String[] {"","X-Violations-Found: 1","virus.msg","W32.Beagle.AO@mm"});
        IcapResult result = defaultIcapRespConsumer.responseReceived(201, icapMessage1);
    }

    @Test
    public void test500() throws IcapException {
        DefaultIcapRespConsumer defaultIcapRespConsumer = new DefaultIcapRespConsumer();
        IcapMessage icapMessage1 = Mockito.mock(IcapMessage.class);
        when(icapMessage1.getIcapHeaders()).thenReturn(new String[] {"","X-Violations-Found: 1","virus.msg","W32.Beagle.AO@mm"});
        IcapResult result = defaultIcapRespConsumer.responseReceived(500, icapMessage1);
        Assert.assertNull(result.getDisposition());
    }

}