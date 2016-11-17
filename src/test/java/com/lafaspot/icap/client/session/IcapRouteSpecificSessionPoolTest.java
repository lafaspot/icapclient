package com.lafaspot.icap.client.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.icap.client.IcapClient;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.exception.IcapException.FailureType;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;

/**
 * UTs for IcapRouteSpecificSessionPool.
 *
 * @author kraman
 *
 */
public class IcapRouteSpecificSessionPoolTest {

    private static final int MAX_SESSIONS = 10;
    private static final int CONNECT_TIMEOUT = 500;
    private static final int INACTITIVY_TIMEOUT = 30000;

    /** Create two sessions first both are in use, available is 0 and used is 2. */
    @Test
    public void testLease2Session() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);
        final IcapSession createdSess1 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess1.isAvailable()).thenReturn(false);
        Mockito.when(createdSess1.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(createdSess1.isDead()).thenReturn(false);
        Mockito.when(createdSess1.getCount()).thenReturn(1L);

        final IcapSession createdSess2 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess2.isAvailable()).thenReturn(false);
        Mockito.when(createdSess2.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(createdSess2.isDead()).thenReturn(false);
        Mockito.when(createdSess2.getCount()).thenReturn(1L);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);

        Mockito.when(client.connect(Mockito.any(URI.class))).thenAnswer(new Answer<IcapSession>() {
            private int count = 0;
            @Override
            public IcapSession answer(final InvocationOnMock inv) {
                if (++count == 1) {
                    return createdSess1;
                } else {
                    return createdSess2;
                }
            }

        });

        IcapSession sess1 = pool.lease(10);
        Assert.assertNotNull(sess1);
        IcapSession sess2 = pool.lease(10);
        Assert.assertNotNull(sess2);
        Mockito.verify(client, Mockito.times(2)).connect(Mockito.any(URI.class));
        Assert.assertEquals(pool.size(), 2);
        Assert.assertEquals(pool.availableSize(), 0);
        Assert.assertEquals(pool.leasedSize(), 2);
    }

    @Test
    public void testLease2SessionWithOneDead() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);
        final IcapSession createdSess1 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess1.isAvailable()).thenReturn(true);
        Mockito.when(createdSess1.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(createdSess1.isDead()).thenReturn(true);
        Mockito.when(createdSess1.getCount()).thenReturn(1L);

        final IcapSession createdSess2 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess2.isAvailable()).thenReturn(false);
        Mockito.when(createdSess2.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(createdSess2.isDead()).thenReturn(false);
        Mockito.when(createdSess2.getCount()).thenReturn(1L);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);

        Mockito.when(client.connect(Mockito.any(URI.class))).thenAnswer(new Answer<IcapSession>() {
            private int count = 0;
            @Override
            public IcapSession answer(final InvocationOnMock inv) {
                if (++count == 1) {
                    return createdSess1;
                } else {
                    return createdSess2;
                }
            }
        });

        IcapSession sess1 = pool.lease(10);
        Assert.assertNotNull(sess1);
        IcapSession sess2 = pool.lease(10);
        Assert.assertNotNull(sess2);
        Mockito.verify(client, Mockito.times(2)).connect(Mockito.any(URI.class));
        Assert.assertEquals(pool.size(), 1);
        Assert.assertEquals(pool.availableSize(), 0);
        Assert.assertEquals(pool.leasedSize(), 1);
    }

    /** Create two sessions, first one is ready to be reused, first one is also expired. */
    @Test
    public void testLease2SessionWithOneExpired() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);
        final IcapSession createdSess1 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess1.isAvailable()).thenReturn(true);
        final long createTime = 5 * 60 * 60 * 1000;
        Mockito.when(createdSess1.getCreateTime()).thenReturn(createTime);
        Mockito.when(createdSess1.isDead()).thenReturn(false);
        Mockito.when(createdSess1.getCount()).thenReturn(1L);

        final IcapSession createdSess2 = Mockito.mock(IcapSession.class);
        Mockito.when(createdSess2.isAvailable()).thenReturn(false);
        Mockito.when(createdSess2.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(createdSess2.isDead()).thenReturn(false);
        Mockito.when(createdSess2.getCount()).thenReturn(1L);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);

        Mockito.when(client.connect(Mockito.any(URI.class))).thenAnswer(new Answer<IcapSession>() {
            private int count = 0;
            @Override
            public IcapSession answer(final InvocationOnMock inv) {
                if (++count == 1) {
                    return createdSess1;
                } else {
                    return createdSess2;
                }
            }
        });

        IcapSession sess1 = pool.lease(10);
        Assert.assertNotNull(sess1);
        IcapSession sess2 = pool.lease(10);
        Assert.assertNotNull(sess2);
        Mockito.verify(client, Mockito.times(2)).connect(Mockito.any(URI.class));
        Assert.assertEquals(pool.size(), 1);
        Assert.assertEquals(pool.availableSize(), 0);
        Assert.assertEquals(pool.leasedSize(), 1);
    }

    /** Request two leases with one release inbetween, the session should be reused. */
    @Test
    public void testLease2SessionWithFree() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);
        final IcapSession sess = Mockito.mock(IcapSession.class);
        Mockito.when(sess.isAvailable()).thenReturn(true);
        Mockito.when(sess.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(sess.isDead()).thenReturn(false);
        Mockito.when(sess.getCount()).thenReturn(1L);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);
        Mockito.when(client.connect(Mockito.any(URI.class))).thenReturn(sess);

        IcapSession sess1 = pool.lease(CONNECT_TIMEOUT);
        Assert.assertNotNull(sess1);
        IcapSession sess2 = pool.lease(CONNECT_TIMEOUT);
        Assert.assertNotNull(sess2);
        Mockito.verify(client, Mockito.times(1)).connect(Mockito.any(URI.class));
        Assert.assertEquals(pool.size(), 1);
        Assert.assertEquals(pool.availableSize(), 0);
        Assert.assertEquals(pool.leasedSize(), 1);
    }

    /** Test failed to connect case. */
    @Test(expectedExceptions = IcapException.class, expectedExceptionsMessageRegExp = "Not connected to server")
    public void testLeaseWithConnectException() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);

        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, MAX_SESSIONS, logger);
        Mockito.when(client.connect(Mockito.any(URI.class))).thenThrow(new IcapException(FailureType.NOT_CONNECTED));

        IcapSession sess = pool.lease(10000);
        Assert.assertNotNull(sess);
    }

    /** Request another lease when max sessions is reached. */
    @Test(expectedExceptions = IcapException.class, expectedExceptionsMessageRegExp = "Ran out of connections")
    public void testLeaseWithMaxSessionCountReached() throws URISyntaxException, TimeoutException, IcapException {

        final IcapClient client = Mockito.mock(IcapClient.class);
        final URI route = new URI("icap://127.0.0.1:1344");
        final Logger logger = Mockito.mock(Logger.class);
        final IcapSession sess = Mockito.mock(IcapSession.class);
        Mockito.when(sess.isAvailable()).thenReturn(false);
        Mockito.when(sess.getCreateTime()).thenReturn(System.currentTimeMillis());
        Mockito.when(sess.isDead()).thenReturn(false);
        Mockito.when(sess.getCount()).thenReturn(1L);

        final int maxAllowedSessions = 1;
        final IcapRouteSpecificSessionPool pool = new IcapRouteSpecificSessionPool(client, route, maxAllowedSessions, logger);
        Mockito.when(client.connect(Mockito.any(URI.class))).thenReturn(sess);

        IcapSession sess1 = pool.lease(CONNECT_TIMEOUT);
        Assert.assertNotNull(sess1);
        IcapSession sess2 = pool.lease(CONNECT_TIMEOUT);
        Assert.assertNotNull(sess2);
        Mockito.verify(client, Mockito.times(1)).connect(Mockito.any(URI.class));
        Assert.assertEquals(pool.size(), 1);
        Assert.assertEquals(pool.availableSize(), 0);
        Assert.assertEquals(pool.leasedSize(), 1);
    }
}
