/**
 *
 */
package com.lafaspot.icap.client;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.exception.IcapException;

/**
 * @author kraman
 *
 */
public class IcapResult {

    /** violation filename. */
    private String violationFilename;

    /** Symantec violation id. */
    private String violationId;

    /** Symantec violation name. */
    private String violationName;

    /** Number of violations found. */
    private int numViolations;

    /** Disposition returned by Symantec server. */
    private Disposition disposition;

    /** Stream where cleaned file needs to be written to. If client does not want cleaned data, they can pass null. */
    private byte[] cleanedBytes;

    /**
     * @return the cleanedBytes
     */
    public byte[] getCleanedBytes() {
        return cleanedBytes;
    }

    /**
     * @param cleanedBytes the cleanedBytes to set
     */
    public void setCleanedBytes(final byte[] cleanedBytes) {
        this.cleanedBytes = cleanedBytes;
    }

    /**
     * @return the violationFilename
     */
    public String getViolationFilename() {
        return violationFilename;
    }




    /**
     * @param violationFilename the violationFilename to set
     */
    public void setViolationFilename(final String violationFilename) {
        this.violationFilename = violationFilename;
    }




    /**
     * @return the violationId
     */
    public String getViolationId() {
        return violationId;
    }




    /**
     * @param violationId the violationId to set
     */
    public void setViolationId(final String violationId) {
        this.violationId = violationId;
    }




    /**
     * @return the violationName
     */
    public String getViolationName() {
        return violationName;
    }




    /**
     * @param violationName the violationName to set
     */
    public void setViolationName(final String violationName) {
        this.violationName = violationName;
    }




    /**
     * @return the numViolations
     */
    public int getNumViolations() {
        return numViolations;
    }




    /**
     * @param numViolations the numViolations to set
     */
    public void setNumViolations(final int numViolations) {
        this.numViolations = numViolations;
    }




    /**
     * @return the disposition
     */
    public Disposition getDisposition() {
        return disposition;
    }

    /**
     * Sets the disposition value.
     *
     * @param disposition disposition enum value
     */

    public void setDisposition(@Nonnull final Disposition disposition) {
        this.disposition = disposition;
    }

    /**
     * Sets the disposition value.
     *
     * @param dispositionStr the disposition to set as String
     * @throws IcapException on failure
     */
    public void setDispositionAsStr(final String dispositionStr) throws IcapException {
        this.disposition = Disposition.fromStrng(dispositionStr.trim());
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        buf.append("NumberOfViolations: ");
        buf.append(numViolations);
        if (null != violationFilename) {
            buf.append(", ViolationFilename: ");
            buf.append(violationFilename);
        }
        if (null != violationId) {
            buf.append(", ViolationId: ");
            buf.append(violationId);
        }
        if (null != violationName) {
            buf.append(", ViolationName: ");
            buf.append(violationName);
        }

        if (null != disposition) {

            buf.append(", Disposition: ");
            buf.append(disposition);
        }
        return buf.toString();
    }


    /**
     * Enum that defines the disposition result form the Symantec server.
     *
     * @author kraman
     *
     */
    public enum Disposition {
        /** file is clean. */
        CLEAN(-1),
        /** infected could not be repaired. */
        INFECTED_UNREPAIRED(0),
        /** file infected was repaired. */
        INFECTED_REPAIRED(1),
        /** file infected and content replaced. */
        INFECTED_REPLACED(2);

        /**
         * Private constructor.
         *
         * @param v int value of enum
         */
        private Disposition(final int v) {
            intVal = v;
        }

        /**
         * Parse disposition value from string.
         *
         * @param val string value
         * @return the disposition enum
         * @throws IcapException on failure
         */
        public static Disposition fromStrng(@Nonnull final String val) throws IcapException {
            try {
                int intVal = Integer.parseInt(val);
                switch (intVal) {
                case 0:
                    return INFECTED_UNREPAIRED;
                case 1:
                    return INFECTED_REPAIRED;
                case 2:
                    return INFECTED_REPLACED;
                default:
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR);
                }
            } catch (NumberFormatException e) {
                throw new IcapException(IcapException.FailureType.PARSE_ERROR);
            }

        }

        /** The integer value of enum. */
        private final int intVal;
    }

}
