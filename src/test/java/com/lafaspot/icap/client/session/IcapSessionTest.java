package com.lafaspot.icap.client.session;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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
        Mockito.when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        Mockito.when(mockChannelFuture.isCancelled()).thenReturn(false);
        Mockito.when(mockChannelFuture.isSuccess()).thenReturn(false);

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager);
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

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager);
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
        Mockito.when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        Mockito.when(mockChannelFuture.isCancelled()).thenReturn(false);
        Mockito.when(mockChannelFuture.isSuccess()).thenReturn(true);
        Mockito.when(mockChannelFuture.channel()).thenReturn(mockChannel);
        Mockito.when(mockChannel.pipeline()).thenReturn(mockChannelPipeline);
        Mockito.when(mockChannel.closeFuture()).thenReturn(mockChannelFuture);

        final String filename = "emptyFile.jpg";
        byte buf[] = new byte[0];

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager);
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
    public void testScanFile() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        final ChannelFuture mockChannelFuture = Mockito.mock(ChannelFuture.class);
        final Channel mockChannel = Mockito.mock(Channel.class);
        final ChannelPipeline mockChannelPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(mockBootstrap.connect("localhost", 1344)).thenReturn(mockChannelFuture);
        Mockito.when(mockChannelFuture.isCancelled()).thenReturn(false);
        Mockito.when(mockChannelFuture.isSuccess()).thenReturn(true);
        Mockito.when(mockChannelFuture.channel()).thenReturn(mockChannel);
        Mockito.when(mockChannel.pipeline()).thenReturn(mockChannelPipeline);
        Mockito.when(mockChannel.closeFuture()).thenReturn(mockChannelFuture);
        Mockito.when(mockChannel.writeAndFlush(new IcapOptions(uri).getMessage())).thenReturn(mockChannelFuture);

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

        client = new IcapSession(sessionId, mockBootstrap, uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, reuseSession, logManager);
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
        Mockito.when(mockScanIcapMessage.getResult()).thenReturn(mockScanIcapResult);

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