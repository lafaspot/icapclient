package com.lafaspot.icap.client.session;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.lafaspot.icap.client.AbstractIcapRequestProducer;
import com.lafaspot.icap.client.IcapRequestProducer;
import com.lafaspot.icap.client.IcapResponseConsumer;
import com.lafaspot.icap.client.codec.IcapRespmod;
import com.lafaspot.icap.client.impl.DefaultIcapRequestProducer;
import com.lafaspot.icap.client.impl.DefaultIcapRespConsumer;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.IcapResult.Disposition;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

public class IcapSessionTest {

    private static final int CONNECT_TIMEOUT_MILLIS = 3000;
    private static final int INACTIVITY_TIMEOUT_MILLIS = 3000;

    private static final String sessionId = "1";
    private static URI uri;

    private static final Boolean reuseSession = false;

    private static final Bootstrap mockBootstrap = Mockito.mock(Bootstrap.class);

    private LogManager logManager;
    private Logger logger;

    private IcapSession client;
    private static final byte[] TRAILER_BYTES = { '\r', '\n', '0', '\r', '\n' };

    @BeforeClass
    public void init() throws IcapException, URISyntaxException {
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        logger = logManager.getLogger(new LogContext(IcapSessionTest.class.getName()) {
        });

        uri = URI.create("icap://localhost:1344");
    }

    /**
     * Test connect method when client is unable to connect to server.
     * @throws IcapException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NoSuchAlgorithmException
  	*/
    @Test (expectedExceptions = IcapException.class, expectedExceptionsMessageRegExp = "Not connected to server")
    public void testConnectWhenServerIsNotConnected() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        final ChannelFuture mockChannelFuture = Mockito.mock(ChannelFuture.class);
        when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        when(mockChannelFuture.isCancelled()).thenReturn(false);
        when(mockChannelFuture.isSuccess()).thenReturn(false);
        final IcapRequestProducer requestProducer = Mockito.mock(IcapRequestProducer.class);
        final IcapResponseConsumer responseConsumer = Mockito.mock(IcapResponseConsumer.class);

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager,
                requestProducer, responseConsumer);
        client.connect();
    }

    /**
     * Test scanFile method when not connected to server.
     * @throws IcapException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NoSuchAlgorithmException
  	*/
    @Test (expectedExceptions = IcapException.class, expectedExceptionsMessageRegExp = "Not connected to server")
    public void testScanFileWhenServerIsNotConnected() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        final String filename = "koenigsegg.jpg";
        final InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        final IcapRequestProducer requestProducer = Mockito.mock(IcapRequestProducer.class);
        final IcapResponseConsumer responseConsumer = Mockito.mock(IcapResponseConsumer.class);

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager,
                requestProducer, responseConsumer);
        client.scanFile(filename, copiedBuf);
    }

    /**
     * Test scanFile method when file size is zero.
     * @throws IcapException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NoSuchAlgorithmException
  	*/
    @Test
    public void testScanFileWhenFileSizeIsZero() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        final ChannelFuture mockChannelFuture = Mockito.mock(ChannelFuture.class);
        final Channel mockChannel = Mockito.mock(Channel.class);
        final ChannelPipeline mockChannelPipeline = Mockito.mock(ChannelPipeline.class);
        when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        when(mockChannelFuture.isCancelled()).thenReturn(false);
        when(mockChannelFuture.isSuccess()).thenReturn(true);
        when(mockChannelFuture.channel()).thenReturn(mockChannel);
        when(mockChannel.pipeline()).thenReturn(mockChannelPipeline);
        when(mockChannel.closeFuture()).thenReturn(mockChannelFuture);
        final IcapRequestProducer requestProducer = Mockito.mock(IcapRequestProducer.class);
        final IcapResponseConsumer responseConsumer = Mockito.mock(IcapResponseConsumer.class);

        final String filename = "emptyFile.jpg";
        byte buf[] = new byte[0];

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager,
                requestProducer, responseConsumer);
        client.connect();
        Future<IcapResult> future = client.scanFile(filename, buf);
        Assert.assertNotNull(future);
        Assert.assertTrue(future.isDone());
        final IcapResult r = future.get();
        Assert.assertNotNull(r);
        Assert.assertEquals(r.getNumViolations(), 0);
        Assert.assertEquals(r.getCleanedBytes().length, 0);
        Assert.assertEquals(r.getDisposition(), Disposition.CLEAN);
    }

    /**
     * Test scanFile method when client successfully gets a future instance.
     * @throws IcapException
     * @throws IOException
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws NoSuchAlgorithmException
  	*/
    @Test
    public void testScanFile()
            throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException, URISyntaxException {
        final ChannelFuture mockChannelFuture = Mockito.mock(ChannelFuture.class);
        final Channel mockChannel = Mockito.mock(Channel.class);
        final ChannelPipeline mockChannelPipeline = Mockito.mock(ChannelPipeline.class);
        final String respMod = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0\r\n" + "Host: 127.0.0.1\r\n"
                + "Connection: keep-alive\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19\r\n" + "\r\n"
                + "GET virus.msg HTTP/1.1\r\n" + "Host: 127.0.0.1\r\n" + "\r\n" + "HTTP/1.1 200 OK\r\n" + "\r\n" + "f\r\n" + "\r\n";

        final byte[] inBuffer = { '0', '1', '2', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        //final URI uri = new URI("http://localhost:4080");

        when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        when(mockChannelFuture.isCancelled()).thenReturn(false);
        when(mockChannelFuture.isSuccess()).thenReturn(true);
        when(mockChannelFuture.channel()).thenReturn(mockChannel);
        when(mockChannel.pipeline()).thenReturn(mockChannelPipeline);
        when(mockChannel.closeFuture()).thenReturn(mockChannelFuture);
        when(mockChannel.writeAndFlush(new IcapOptions(uri, "SYMCScanResp-AV").getMessage())).thenReturn(mockChannelFuture);
        final IcapRequestProducer requestProducer = Mockito.mock(IcapRequestProducer.class);
        final IcapResponseConsumer responseConsumer = Mockito.mock(IcapResponseConsumer.class);
        when(requestProducer.generateOptions()).thenReturn(new IcapOptions(uri, "SYMCScanResp-AV"));


        final String filename = "koenigsegg.jpg";
        final InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
    	byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);
        when(requestProducer.generateRespMod(false)).thenReturn(new IcapRespmod(uri,respMod,buf, TRAILER_BYTES));

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager,
                requestProducer, responseConsumer);
        client.connect();
        Future<IcapResult> future = client.scanFile(filename, copiedBuf);
        Assert.assertNotNull(future);
        Assert.assertFalse(future.isDone());

        // Mocking options response
        final IcapMessage mockOptionsIcapMessage = Mockito.mock(IcapMessage.class);
        // Mocking scan response
        final IcapMessage mockScanIcapMessage = Mockito.mock(IcapMessage.class);
        IcapResult mockScanIcapResult = new IcapResult();
        mockScanIcapResult.setCleanedBytes(buf);
        mockScanIcapResult.setDisposition(Disposition.CLEAN);
        mockScanIcapResult.setNumViolations(0);
        when(mockScanIcapMessage.getResult()).thenReturn(mockScanIcapResult);

        // processResponse when state is OPTIONS
        client.processResponse(mockOptionsIcapMessage);

        // processResponse when state is SCAN
        client.processResponse(mockScanIcapMessage);

        Assert.assertTrue(future.isDone());
        final IcapResult r = future.get();
        Assert.assertNotNull(r);
        Assert.assertEquals(r.getNumViolations(), 0);
        Assert.assertEquals(r.getCleanedBytes().length, fileLen);
        Assert.assertEquals(r.getDisposition(), Disposition.CLEAN);
    }
}