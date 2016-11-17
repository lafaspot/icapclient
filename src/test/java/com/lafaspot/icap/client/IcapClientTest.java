package com.lafaspot.icap.client;



import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Formatter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.session.IcapRouteSpecificSessionPool;
import com.lafaspot.icap.client.session.IcapSession;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;

public class IcapClientTest {

    private static final int CONNECT_TIMEOUT_MILLIS = 30000;
    private static final int INACTIVITY_TIMEOUT_MILLIS = 30000;
    private static final int MAX_SESSIONS = 128;

    private LogManager logManager;
    private Logger logger;

    private IcapClient client;

    @BeforeClass
    public void init() throws IcapException {
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        logger = logManager.getLogger(new LogContext(IcapClientTest.class.getName()) {
        });
        client = new IcapClient(2, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, 0, logManager);
    }

    @Test(enabled = false, expectedExceptions = IcapException.class)
    public void scanBadFile() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "badAvScanDoc.doc";
        InputStream in = getClass().getClassLoader().getResourceAsStream("badAvScanDoc.doc");
        byte buf[] = new byte[8192];

        int o =0;
        int n = 0;

         while ((o < 8192) && (n = in.read(buf, o, 1)) != -1) {
             o += n;
         }
         byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, copiedBuf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test(enabled = false)
    public void scanImgFile() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {

        final String filename = "koenigsegg.jpg";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, copiedBuf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
        Assert.assertEquals(r.getCleanedBytes().length, fileLen);

        FileOutputStream ostream = new FileOutputStream("scanned." + filename);
        try {
            ostream.write(r.getCleanedBytes());
        } finally {
            ostream.close();
        }
        Assert.assertEquals(r.getCleanedBytes(), buf);

        // Thread.sleep(3600000);
    }

    @Test(enabled = false)
    public void scanImgFileMultipleTimes() throws IcapException, IOException, InterruptedException, ExecutionException,
            NoSuchAlgorithmException {

        final String filename = "koenigsegg.jpg";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");

        Future<IcapResult>[] futureArr = new Future [3];

        for (int i = 0; i < futureArr.length; i++) {
            futureArr[i] = client.scanFile(uri, filename, copiedBuf);
            Thread.sleep(500);
        }


        long startTime = System.currentTimeMillis();
        boolean allDone = false;
        for (;;) {
            Assert.assertFalse((System.currentTimeMillis() - startTime) > 10000);
            if (allDone) {
                break;
            }

            allDone = true;
            for (int i = 0; i < futureArr.length; i++) {
                if (!futureArr[i].isDone()) {
                    allDone = false;
                } else {
                    IcapResult r = futureArr[i].get();
                    Assert.assertEquals(r.getNumViolations(), 0);
                    Assert.assertEquals(r.getCleanedBytes().length, fileLen);
                    Assert.assertEquals(r.getCleanedBytes(), buf);
                }
            }
        }


        // Thread.sleep(3600000);
    }

    @Test(enabled = false)
    public void scanLogFile() throws Exception {

        final String filename = "somelog.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, buf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test(enabled = false)
    public void scanTestFile() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "test.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, copiedBuf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);

        Assert.assertEquals(r.getCleanedBytes().length, fileLen);

        FileOutputStream ostream = new FileOutputStream("scanned." + filename);
        try {
            ostream.write(r.getCleanedBytes());
        } finally {
            ostream.close();
        }
        Assert.assertEquals(r.getCleanedBytes(), buf);
    }

    @Test(enabled = false)
    public void scanTestFileTwice() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "test.log";
        // final String filename = "somelog.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        byte buf[] = new byte[8192];

        int o = 0;
        int n = 0;

        while ((o < 8192) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, copiedBuf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);

        future = client.scanFile(uri, filename, copiedBuf);
        r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test(enabled = false)
    public void scanVirusFile() throws IcapException, IOException, InterruptedException, ExecutionException, NoSuchAlgorithmException {
        final String filename = "eicar_virus.com";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        java.util.concurrent.Future<IcapResult> future = client.scanFile(uri, filename, copiedBuf);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 1);
    }

    private String shaChecksum(byte[] buf) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(buf));
    }

    private String byteArray2Hex(final byte[] hash) {
        final Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        final String sha = formatter.toString();
        formatter.close();
        return sha;
    }

    @Test
    public void testLeaseWithValidSessionCreation() throws URISyntaxException, TimeoutException, IcapException {

        final Bootstrap bootstrap = Mockito.mock(Bootstrap.class);
        final ChannelFuture connectFuture = Mockito.mock(ChannelFuture.class);
        Mockito.when(bootstrap.group(Mockito.any(NioEventLoopGroup.class))).thenReturn(bootstrap);
        Mockito.when(bootstrap.channel(NioSocketChannel.class)).thenReturn(bootstrap);
        Mockito.when(bootstrap.connect(Mockito.anyString(), Mockito.anyInt())).thenReturn(connectFuture);
        final NioEventLoopGroup group = Mockito.mock(NioEventLoopGroup.class);
        final LogManager logManager = Mockito.mock(LogManager.class);
        final Logger logger = Mockito.mock(Logger.class);
        Mockito.when(logManager.getLogger(Mockito.any(LogContext.class))).thenReturn(logger);
        final IcapClient client = new IcapClient(bootstrap, group, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, MAX_SESSIONS,
                logManager);
        final URI route = new URI("icap://127.0.0.1:1344");

        Mockito.when(connectFuture.isSuccess()).thenReturn(true);
        final Channel connectChannel = Mockito.mock(Channel.class);
        Mockito.when(connectFuture.channel()).thenReturn(connectChannel);
        final ChannelPipeline channelPipeline = Mockito.mock(ChannelPipeline.class);
        Mockito.when(connectChannel.pipeline()).thenReturn(channelPipeline);
        Mockito.when(connectChannel.closeFuture()).thenReturn(connectFuture);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);
        IcapSession sess = pool.lease(10);
        Assert.assertNotNull(sess);
    }
}