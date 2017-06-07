/**
 *
 */
package com.lafaspot.icap.client.session;

import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.IcapResult.Disposition;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.codec.IcapMessageDecoder;
import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * IcapSession - identifies a session that represents one scan request.
 *
 * @author kraman
 *
 */
public class IcapSession {

    /**
     * Creates a ICAP session.
     *
     * @param sessionId session identifier used for logging
     * @param bootstrap the client bootstrap object
     * @param uri remote ICAP server URI
     * @param connectTimeout channel connect timeout
     * @param inactivityTimeout channel inactivity timeout
     * @param reuseSession if sessions should be reused
     * @param logManager the LogManager instance
     * @throws IcapException on failure
     */
    public IcapSession(@Nonnull final String sessionId, @Nonnull final Bootstrap bootstrap, @Nonnull final URI uri,
            final int connectTimeout,
            final int inactivityTimeout, final boolean reuseSession, @Nonnull final LogManager logManager)
            throws IcapException {
        this.bootstrap = bootstrap;
        this.serverUri = uri;
        this.connectTimeout = connectTimeout;
        this.inactivityTimeout = inactivityTimeout;
        this.count = 0;
        this.createTime = System.currentTimeMillis();
        this.reuseSession = reuseSession;
        LogContext context = new SessionLogContext("IcapSession-" + uri.toASCIIString(), sessionId);
        this.logger = logManager.getLogger(context);
    }

    /**
     * Open a connection to the server identified by route.
     *
     * @throws IcapException on connect failure
     */
    public void connect() throws IcapException {
        logger.debug(" +++ connect to  " + serverUri, null);
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        ChannelFuture future = bootstrap.connect(serverUri.getHost(), serverUri.getPort());
        future.awaitUninterruptibly();

        if (future.isCancelled()) {
            // ignore
        } else if (!future.isSuccess()) {
            throw new IcapException(IcapException.FailureType.NOT_CONNECTED);
        } else {
            stateRef.set(IcapSessionState.CONNECTED);

            this.sessionChannel = future.channel();
            this.sessionChannel.pipeline().addLast("inactivityHandler", new IcapInactivityHandler(this, inactivityTimeout, logger));
            this.sessionChannel.pipeline().addLast(new IcapMessageDecoder(logger));
            this.sessionChannel.pipeline().addLast(new IcapChannelHandler(this));

            final IcapSession thisSession = this;
            this.sessionChannel.closeFuture().addListener(new GenericFutureListener() {
                @Override
                public void operationComplete(final io.netty.util.concurrent.Future future) throws Exception {
                    thisSession.onDisconnect();
                }
            });
        }
    }

    /**
     * Request to scan an file, a request will be sent to the Symantec AV server to scan the request to clean/determine if the file is clean.
     *
     * @param filename name of the file to be scanned
     * @param fileToScan byte stream of the file to be scanned
     * @return the future object
     * @throws IcapException on failure
     */
    @SuppressWarnings({ "unchecked", "rawtypes", "checkstyle:illegalcatch" })
    public Future<IcapResult> scanFile(@Nonnull final String filename, @Nonnull final byte[] fileToScan)
            throws IcapException {

        if (stateRef.get() != IcapSessionState.CONNECTED) {
            throw new IcapException(IcapException.FailureType.NOT_CONNECTED);
        }
        this.filename = filename;
        this.fileToScan = fileToScan;

        if (fileToScan.length == 0) {
            IcapFuture icapFuture = new IcapFuture(this);
            IcapResult icapResult = new IcapResult();
            icapResult.setCleanedBytes(fileToScan);
            icapResult.setDisposition(Disposition.CLEAN);
            icapResult.setNumViolations(0);
            icapFuture.done(icapResult);
            futureRef.set(icapFuture);
            return futureRef.get();
        }

        try {
            logger.debug("connected, sending", null);
            stateRef.set(IcapSessionState.OPTIONS);
            Future writeFuture = this.sessionChannel.writeAndFlush(new IcapOptions(serverUri).getMessage());

        } catch (final Exception e) {
            throw new IcapException(IcapException.FailureType.SCAN_REQUEST_FAILED, e);
        }

        futureRef.set(new IcapFuture(this));
        return futureRef.get();
    }

    /**
     * Callback from netty on receiving a message from the network.
     *
     * @param msg incoming message
     */
    public void processResponse(@Nonnull final IcapMessage msg) {
        logger.debug("<- messageReceived in " + stateRef.get() + ", [\r\n" + msg.toString() + "\r\n]", null);
        switch (stateRef.get()) {
        case OPTIONS:
            if (null != msg.getCause()) {
                logger.debug("options failed " + msg.getCause(), null);
                final IcapFuture f = futureRef.get();
                futureRef.set(null);
                if (null != f) {
                    f.done(msg.getCause());
                }
            } else {
                stateRef.set(IcapSessionState.SCAN);
                msg.reset();
                final IcapRespmod scanReq = new IcapRespmod(serverUri, reuseSession, filename, fileToScan);
                logger.debug(" sending scan req [\r\n" + scanReq.getIcapMessage() + "\r\n]", null);
                this.sessionChannel.writeAndFlush(scanReq.getIcapMessage());
                this.sessionChannel.writeAndFlush(scanReq.getInStream());
                this.sessionChannel.writeAndFlush(scanReq.getTrailerBytes());
                logger.debug(" written payload -> ", null);
                this.sessionChannel.flush();
            }
            break;
        case SCAN:
            if (msg.getCause() != null) {
                logger.debug(" SCAN state - failed " + msg.getCause(), null);
                final IcapFuture f = futureRef.get();
                if (f != null) {
                    futureRef.set(null);
                    f.done(msg.getCause());
                }
            } else {
                logger.debug(" SCAN state - success " + msg.getResult(), null);
                final IcapFuture f = futureRef.get();
                if (f != null) {
                    futureRef.set(null);
                    f.done(msg.getResult());
                }
            }
            if (reuseSession) {
                stateRef.set(IcapSessionState.CONNECTED);
            } else {
                close();
            }
            break;
        default:
        }
    }

    /**
     * Callback from netty on channel inactivity.
     */
    public void onTimeout() {
        logger.debug("**channel timeout** TH " + Thread.currentThread().getId(), null);
        stateRef.set(IcapSessionState.DISCONNECTED);
        if (null != this.sessionChannel) {
            this.sessionChannel.close();
        }
        this.sessionChannel = null;
        if (null != futureRef.get()) {
            final IcapFuture f = futureRef.get();
            futureRef.set(null);
            if (null != f) {
                f.done(new IcapException("inactivity timeout"));
            }
        }
    }

    /**
     * Callback from netty on channel closure.
     */
    public void onDisconnect() {
        logger.debug("**channel disconnected (not-ignored)** TH " + Thread.currentThread().getId(), null);
        final IcapSessionState prevState = stateRef.get();
        stateRef.set(IcapSessionState.DISCONNECTED);
        if (null != this.sessionChannel) {
            this.sessionChannel.close();
        }
        this.sessionChannel = null;
        if (futureRef.get() != null) {
            final IcapFuture f = futureRef.get();
            futureRef.set(null);
            if (null != f) {
                f.done(new IcapException("Channel disconnected, state: " + prevState));
            }
        }
    }

    /**
     * Close this session.
     */
    public void close() {
        stateRef.set(IcapSessionState.DISCONNECTED);
        final Channel ch = this.sessionChannel;
        futureRef.set(null);
        this.sessionChannel = null;
        ch.close();
    }

    /**
     * Is this session active?
     *
     * @return return if active
     */
    public boolean isDead() {
        return stateRef.get() == IcapSessionState.DISCONNECTED;
    }

    /**
     * Is this session available to be used. Yes if it is in connected state.
     *
     * @return true if this session can be used
     */
    public boolean isAvailable() {
        return stateRef.get() == IcapSessionState.CONNECTED;
    }


    /**
     * Return the logger object.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Return the server URI.
     *
     * @return server URI
     */
    public URI getRoute() {
        return serverUri;
    }

    /**
     * Return the command count on this session.
     *
     * @return number of commands run on this session
     */
    public long getCount() {
        return count;
    }

    /**
     * Return the time stamp when the session was created.
     *
     * @return time this session was created
     */
    public long getCreateTime() {
        return createTime;
    }

    /** Reference to the current IcapFuture object. */
    private final AtomicReference<IcapFuture> futureRef = new AtomicReference<IcapFuture>();

    /** pointer to the byte stream of the file to be scanned. */
    private byte[] fileToScan;

    /** filename of the input file to be scanned. */
    private String filename;

    /** Server to connect to. */
    private final URI serverUri;

    /** Bootstrap. */
    private final Bootstrap bootstrap;

    /** channel inactivity timeout. */
    private final int inactivityTimeout;

    /** socket connect timeout. */
    private final int connectTimeout;

    /** Reference to the current state of the session. */
    private AtomicReference<IcapSessionState> stateRef = new AtomicReference<IcapSession.IcapSessionState>(IcapSessionState.DISCONNECTED);

    /** The channel associated with this session. */
    private Channel sessionChannel;

    /** The logger. */
    private final Logger logger;

    /** Timestamp when the session was created. */
    private final long createTime;

    /** Number of commands sent in this session. */
    private long count;

    /** Enable session reuse. */
    private final boolean reuseSession;

    /** Enum identifying the session states. */
    enum IcapSessionState {
        /** Session not started. */
        DISCONNECTED,
        /** connected. */
        CONNECTED,
        /** options request sent. */
        OPTIONS,
        /** scan request sent. */
        SCAN
    };

}
