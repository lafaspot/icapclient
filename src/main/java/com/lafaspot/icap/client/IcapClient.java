/**
 *
 */
package com.lafaspot.icap.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.exception.IcapException.FailureType;
import com.lafaspot.icap.client.session.IcapRouteSpecificSessionPool;
import com.lafaspot.icap.client.session.IcapSession;
import com.lafaspot.icap.client.session.SessionLogContext;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;

/**
 * IcapClient - used to communicate with Symantec AV server using ICAP protocol.
 *
 * @author kraman
 *
 */
public class IcapClient {

    /**
     * IcapClient constructor for unit tests.
     *
     * @param bootstrap Server bootstrap
     * @param group NioEventLoopGroup
     * @param connectTimeout channel connect timeout
     * @param inactivityTimeout channel inactivity timeout
     * @param maxAllowedSessions max number of sessiosn to a given route
     * @param logManager the logger
     */
    public IcapClient(@Nonnull final Bootstrap bootstrap, @Nonnull final NioEventLoopGroup group, final int connectTimeout,
            final int inactivityTimeout, final int maxAllowedSessions, @Nonnull final LogManager logManager) {
        try {
            this.bootstrap = bootstrap;
            this.group = group;
            this.connectTimeout = connectTimeout;
            this.inactivityTimeout = inactivityTimeout;
            this.maxAllowedRoutes = MAX_ROUTES;
            this.maxAllowedSessions = maxAllowedSessions;
            this.logManager = logManager;
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new IcapClientInitializer());
            LogContext context = new SessionLogContext("IcapClient");
            this.logger = logManager.getLogger(context);
        } finally {
            // this.group.shutdownGracefully();
        }
    }

    /**
     * IcapClient constructor.
     *
     * @param threads number of threads to be used in the event loop.
     * @param connectTimeout channel connect timeout
     * @param inactivityTimeout channel inactivity timeout
     * @param maxAllowedSessions max allowed sessions
     * @param logManager the logger framework
     */
    public IcapClient(final int threads, final int connectTimeout, final int inactivityTimeout, final int maxAllowedSessions,
            @Nonnull final LogManager logManager) {

        try {
            this.bootstrap = new Bootstrap();
            this.group = new NioEventLoopGroup(threads);
            this.connectTimeout = connectTimeout;
            this.inactivityTimeout = inactivityTimeout;
            this.logManager = logManager;
            this.maxAllowedSessions = maxAllowedSessions;
            this.maxAllowedRoutes = MAX_ROUTES;
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new IcapClientInitializer());

            LogContext context = new SessionLogContext("IcapClient");
            this.logger = logManager.getLogger(context);
        } finally {
            // this.group.shutdownGracefully();
        }
    }


    /**
     * IcapClient constructor.
     *
     * @param threads number of threads to be used in the event loop.
     * @param connectTimeout channel connect timeout
     * @param inactivityTimeout channel inactivity timeout
     * @param logManager the logger framework
     */
    public IcapClient(final int threads, final int connectTimeout, final int inactivityTimeout,
            @Nonnull final LogManager logManager) {

        try {
            this.bootstrap = new Bootstrap();
            this.group = new NioEventLoopGroup(threads);
            this.connectTimeout = connectTimeout;
            this.inactivityTimeout = inactivityTimeout;
            this.logManager = logManager;
            this.maxAllowedSessions = MAX_SESSIONS;
            this.maxAllowedRoutes = MAX_ROUTES;
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new IcapClientInitializer());

            LogContext context = new SessionLogContext("IcapClient");
            this.logger = logManager.getLogger(context);
        } finally {
            // this.group.shutdownGracefully();
        }
    }

    /**
     * API to scan a file, will return a future object to be polled for result.
     *
     * @param server URI pointing to the Symantec AV scan server
     * @param filename name of the file to be scanned
     * @param toScanFile byte stream of the file to be scanned
     * @return the future object
     * @throws IcapException on failure
     */
    public Future<IcapResult> scanFile(@Nonnull final URI server, @Nonnull final String filename, @Nonnull final byte[] toScanFile)
            throws IcapException {

        try {
            if (!lock.tryLock(connectTimeout, TimeUnit.MILLISECONDS)) {
                throw new IcapException(FailureType.INTERNAL_ERROR);
            }
            try {
                IcapRouteSpecificSessionPool pool = poolMap.get(server);
                if (null == pool) {
                    if (poolMap.size() >= maxAllowedRoutes) {
                        throw new IcapException(FailureType.NO_MORE_ROUTES);
                    }
                    pool = new IcapRouteSpecificSessionPool(this, server, maxAllowedSessions, logger);
                    poolMap.put(server, pool);
                }

                IcapSession sess = pool.lease(connectTimeout);
                return sess.scanFile(filename, toScanFile);
            } finally {
                lock.unlock();
            }
        } catch (TimeoutException e) {
            throw new IcapException(FailureType.NOT_CONNECTED, e);
        } catch (InterruptedException e) {
            throw new IcapException(FailureType.NOT_CONNECTED, e);
        }
    }

    /**
     * Create a new IcapSession and connect to server.
     *
     * @param route server URI
     * @return IcapSession
     * @throws IcapException on failure
     */
    public IcapSession connect(@Nonnull final URI route) throws IcapException {
        final IcapSession sess = new IcapSession(bootstrap, route, connectTimeout, inactivityTimeout, (0 != maxAllowedSessions), logManager);
        sess.connect();
        return sess;
    }

    /**
     * API to scan a file, will return a future object to be polled for result.
     *
     * @param server URI pointing to the Symantec AV scan server
     * @param connectTimeout socket connect timeout value
     * @param inactivityTimeout channel inactivity timeout
     * @param fileName name of the file to be scanned
     * @param toScanFile byte stream of the file to be scanned
     * @return the future object
     * @throws IcapException on failure
     */
    public Future<IcapResult> scanFileOld(@Nonnull final URI server, final int connectTimeout, final int inactivityTimeout,
            @Nonnull final String fileName, @Nonnull final byte[] toScanFile) throws IcapException {
        return new IcapSession("abc", server, connectTimeout, inactivityTimeout, bootstrap, (0 != maxAllowedSessions), logManager)
                .scanFile(fileName, toScanFile);
    }

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for ICAP client. */
    private final EventLoopGroup group;

    /** The logger. */
    private final LogManager logManager;

    /** Map that holds session pools per route. */
    private final Map<URI, IcapRouteSpecificSessionPool> poolMap = new HashMap<URI, IcapRouteSpecificSessionPool>();

    /** Lock for synchronization. */
    private final ReentrantLock lock = new ReentrantLock();

    /** The logger object. */
    private final Logger logger;

    /** Max sessions allowed per route. */
    private final int maxAllowedSessions;

    /** Max routes allowed. */
    private final int maxAllowedRoutes;

    /** Channel connect timeout. */
    private final int connectTimeout;

    /** Channel inactivity timeout. */
    private final int inactivityTimeout;

    /** Max sessions to cache. */
    private static final int MAX_SESSIONS = 128;

    /** Max routes allowed. */
    private static final int MAX_ROUTES = 64;

}
