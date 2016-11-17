/**
 *
 */
package com.lafaspot.icap.client.exception;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.lafaspot.icap.client.session.IcapSession;

/**
 * IcapException - encapsulates failure reason.
 *
 * @author kraman
 *
 */
public class IcapException extends Exception {


    /** Context for failure. */
    private IcapSession context;

    /** Failure type. */
    private FailureType failureType;

    /**
     * The map of validation errors. Specifies what input caused the invalid input error. Can be null.
     */
    @Nullable
    private List<String> errorDetail = null;

    /**
     * Constructor that takes in a string message.
     *
     * @param message the message
     */
    public IcapException(@Nonnull final String message) {
        super(message);
    }

    @Override
    public String getMessage() {
        final StringBuffer buf = new StringBuffer(super.getMessage());
        if (null != errorDetail) {
            buf.append(", Details:");
            buf.append(errorDetail);
        }
        return buf.toString();
    }

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     */
    public IcapException(@Nonnull final FailureType failureType) {
        super(failureType.getMessage());
        this.failureType = failureType;
    }

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     * @param errorDetail more info on the error
     */
    public IcapException(@Nonnull final FailureType failureType, @Nullable final List<String> errorDetail) {
        super(failureType.getMessage());
        this.failureType = failureType;
        this.errorDetail = errorDetail;
    }

    /**
     * Constructor with failure type.
     *
     * @param failureType type of failure
     * @param cause the wrapped exception
     */
    public IcapException(@Nonnull final FailureType failureType, @Nullable final Throwable cause) {
        super(failureType.getMessage(), cause);
    }

    /**
     * Constructor with context from IcapSession.
     *
     * @param type failure type
     * @param ctx context
     */
    public IcapException(@Nonnull final FailureType type, @Nonnull final IcapSession ctx) {
        this.context = ctx;
        this.failureType = failureType;
    }

    /**
     * Types of failures.
     *
     * @author kraman
     *
     */
    public static enum FailureType {
        /** Message parsing failed. */
        PARSE_ERROR("Parse error - failed to parse ICAP message"),
        /** ICAP Message parsing failed. */
        PARSE_ERROR_ICAP_STATUS("Icap Parse error - failed to parse ICAP header"),
        /** Not connected to Symantec AV Server. */
        NOT_CONNECTED("Not connected to server"),
        /** The session object is already in use. */
        SESSION_IN_USE("Session in use."),
        /** Timeout occurred. */
        TIMEOUT("Operation timed out"),
        /** No free conenction available. */
        NO_FREE_CONNECTION("Ran out of connections"),
        /** Request to scan file failed. */
        SCAN_REQUEST_FAILED("Scan request failed"),
        /** Internal error. */
        INTERNAL_ERROR("Internal error"),
        /** Invalid response from server. */
        SERVER_ERROR("Invalid response from server."),
        /** Reached max number of routes. */
        NO_MORE_ROUTES("Reached max routes."),
        /** conneciton reuse not implemented. */
        CONNECTION_REUSE_NOT_IMPLEMENTED("Connection reuse not implemented.");

        /** The error message. */
        @Nonnull
        private final String message;

        /**
         * private constructor to create the enum.
         *
         * @param message the string message
         * */
        @Nonnull
        private FailureType(@Nonnull final String message) {
            this.message = message;
        }

        /**
         * Returns the string message.
         *
         * @return the cause
         */
        @Nonnull
        public String getMessage() {
            return message;
        }
    }
}
