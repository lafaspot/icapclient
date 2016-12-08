package com.lafaspot.icap.client;

import java.net.URI;
import java.net.URISyntaxException;
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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class IcapClientTest {

    private static final int CONNECT_TIMEOUT_MILLIS = 30000;
    private static final int INACTIVITY_TIMEOUT_MILLIS = 30000;
    private static final int MAX_SESSIONS = 128;

    private LogManager logManager;
    private Logger logger;

    @BeforeClass
    public void init() throws IcapException {
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        logger = logManager.getLogger(new LogContext(IcapClientTest.class.getName()) {
        });
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