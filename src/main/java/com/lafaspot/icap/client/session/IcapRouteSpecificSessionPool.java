/**
 *
 */
package com.lafaspot.icap.client.session;

import java.net.URI;
import java.time.Clock;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.IcapClient;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.exception.IcapException.FailureType;
import com.lafaspot.logfast.logging.Logger;

/**
 * Manages pool of sessions. Configured at the beginning to be of static size, holds sessions only to one server.
 * @author kraman
 *
 */
public class IcapRouteSpecificSessionPool {

    /** Max number of sessions allowed. */
    private final int maxAllowedSessions;

    /** The logger. */
    private final Logger logger;

    /** The Icap Client object. */
    private final IcapClient client;

    /** clock object. */
    private final Clock clock = Clock.systemUTC();

    /** URI that defines the server. */
    private final URI route;

    /** List of available IcapSession objects . */
    private final LinkedList<IcapSession> available;

    /** Set of in-use IcapSession objects. */
    private final Set<IcapSession> leased;

    /** Lock for synchronizing. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Max number of commands to be sent on the session. */
    private static final long MAX_COMMAND_COUNT = Integer.MAX_VALUE;

    /** Max time to use the session. */
    private static final long MAX_SESSION_TIME = 5 * 60 * 60 * 1000;

    /**
     * Constructor to create IcapSessionPool.
     *
     * @param client the IcapClient instance
     * @param route the server URI
     * @param maxAllowedSessions max allowed sessions
     * @param logger the logger object
     */
    public IcapRouteSpecificSessionPool(@Nonnull final IcapClient client, @Nonnull final URI route, final int maxAllowedSessions,
            @Nonnull final Logger logger) {
        this.client = client;
        this.available = new LinkedList<IcapSession>();
        this.leased = new HashSet<IcapSession>();
        this.route = route;
        this.maxAllowedSessions = maxAllowedSessions;
        this.logger = logger;
    }

    /**
     * Returns a IcapSession object, if available. Returns null if not.
     *
     * @param timeout time in millisecond
     * @return IcapSession object
     * @throws TimeoutException when a session could not be found within timeout given
     * @throws IcapException on failure
     */
    @Nonnull
    public IcapSession lease(final int timeout) throws TimeoutException, IcapException {
        final long now = clock.millis();
        final long deadline = timeout + now;

        logger.debug("### = available A:" + available.size() + ", L:" + leased.size(), null);
        try {
            if (!lock.tryLock(timeout, TimeUnit.MILLISECONDS)) {
                throw new IcapException(FailureType.TIMEOUT);
            }

            // housekeeping, remove unused sessions
            final Iterator<IcapSession> leasedIter = leased.iterator();
            while (leasedIter.hasNext()) {
                IcapSession leasedSess = leasedIter.next();
                if (leasedSess.isDead()) {
                    leasedIter.remove();
                    continue;
                }

                if (leasedSess.isAvailable()) {
                    leasedIter.remove();
                    available.add(leasedSess);
                    continue;
                }

                // time check
                if (deadline <= clock.millis()) {
                    throw new IcapException(FailureType.TIMEOUT);
                }
            }



            final Iterator<IcapSession> availableIter = available.iterator();
            IcapSession sess = null;
            while (availableIter.hasNext()) {
                sess = availableIter.next();
                if ((sess.getCount() + 1 >= MAX_COMMAND_COUNT) || ((now - sess.getCreateTime()) >= MAX_SESSION_TIME) || sess.isDead()) {
                    availableIter.remove();
                    sess = null;
                } else {
                    break;
                }

                // time check
                if (deadline <= clock.millis()) {
                    throw new IcapException(FailureType.TIMEOUT);
                }
            }

            if (null == sess) {
                // all sessions are in use
                if (maxAllowedSessions > 0 && size() >= maxAllowedSessions) {
                    throw new IcapException(FailureType.NO_FREE_CONNECTION);
                }
                // try getting a new session
                sess = client.connect(route);
                leased.add(sess);
            } else {
                availableIter.remove();
                leased.add(sess);
            }

            return sess;

        } catch (IcapException e) {
            throw e;
        } catch (InterruptedException e) {
            throw new IcapException(FailureType.NO_FREE_CONNECTION, e);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Size of the session pool.
     *
     * @return session pool size
     */
    protected int size() {
        return leased.size() + available.size();
    }

    /**
     * Returns the number of sessions available in the leased pool.
     *
     * @return size of leased pool
     */
    protected int leasedSize() {
        return leased.size();
    }

    /**
     * Returns the number of sessions available in the available pool.
     *
     * @return size of the available pool
     */
    protected int availableSize() {
        return available.size();
    }

}
