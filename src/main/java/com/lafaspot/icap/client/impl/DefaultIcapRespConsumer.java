package com.lafaspot.icap.client.impl;

import com.lafaspot.icap.client.AbstractIcapResponseConsumer;
import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.exception.IcapException;

import javax.annotation.Nonnull;
import java.util.Arrays;

/**
 * @author nimmyr
 * A  Default ICAP response consumer for any antivirus engine.
 */
public class DefaultIcapRespConsumer extends AbstractIcapResponseConsumer {

    @Override
    public IcapResult responseReceived(final int status, @Nonnull final IcapMessage icapMessage) throws IcapException {
        switch (status) {
        case HTTP_STATUS_CODE_200:
            return handleIcap200Ok(icapMessage.getIcapHeaders());
        case HTTP_STATUS_CODE_201:
            return handleIcap201Ok(icapMessage.getIcapHeaders());
        case HTTP_STATUS_CODE_500:
        default:
            return new IcapResult();
        }
    }

    /**
     * Parse ICAP 200 OK message.
     *
     * @param headers ICAP headers
     * @throws IcapException on failure
     */
    private IcapResult handleIcap200Ok(@Nonnull final String[] headers) throws IcapException {
        int index = 1;
        IcapResult result = new IcapResult();
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_RES_BODY_PREFIX);
                if (-1 != j) {
                    String resBodyStr = headers[index].substring(j + ICAP_RES_BODY_PREFIX.length() + 1);

                    try {
                        // TODO: validate the parsed value
                        Integer.parseInt(resBodyStr.trim());
                        result.setNumViolations(0);
                        result.setDisposition(IcapResult.Disposition.CLEAN);
                        break;
                    } catch (NumberFormatException e) {
                        final String partOfTheErrorStr = (resBodyStr.length() > MAX_DEBUG_STR_LEN ? resBodyStr.substring(0,
                                (MAX_DEBUG_STR_LEN - 1)) : resBodyStr);
                        throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("bodyLen", partOfTheErrorStr));
                    }

                } else if (headers[index].indexOf(ICAP_NULL_BODY_PREFIX) != -1) {
                    // done
                    return result;
                }
            }
        }
        return result;
    }

    /**
     * Parse ICAP 201 message.
     *
     * @param headers ICAP headers
     * @throws IcapException on failure
     */
    private IcapResult handleIcap201Ok(@Nonnull final String[] headers) throws IcapException {

        // skip first status line
        int index = 1;
        IcapResult result = new IcapResult();
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_VIOLATIONS_PREFIX)) {
                break;
            }
        }

        int numViolations;
        if (index < headers.length) {
            final int k = headers[index].indexOf(':');
            if (-1 != k) {
                try {
                    numViolations = Integer.parseInt(headers[index].substring(k + 1).trim());
                    result.setNumViolations(numViolations);
                } catch (NumberFormatException e) {
                    final String partOfTheErrorStr = (headers[index].length() > MAX_DEBUG_STR_LEN ? headers[index].substring(0,
                            (MAX_DEBUG_STR_LEN - 1))
                            : headers[index]);
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR, Arrays.asList("numViolations", partOfTheErrorStr));
                }
            } else {
                throw new IcapException(IcapException.FailureType.PARSE_ERROR);
            }
            // increment
            index++;

            final int headersPerViolation = 4;
            // validate header size
            if (index + (headersPerViolation * numViolations) < headers.length) {
                // look at first violation only
                result.setViolationFilename(headers[index++]);
                result.setViolationName(headers[index++]);
                result.setViolationId(headers[index++]);
                String dispositionStr = headers[index++];
                result.setDispositionAsStr(dispositionStr);
            } else {
                throw new IcapException(IcapException.FailureType.PARSE_ERROR);
            }

        } else {
            throw new IcapException(IcapException.FailureType.PARSE_ERROR);
        }
        return result;
    }


    /** ICAP violations found prefix. */
    private static final String ICAP_VIOLATIONS_PREFIX = "X-Violations-Found:";
}
