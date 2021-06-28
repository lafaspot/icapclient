package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.exception.IcapException;

import javax.annotation.Nonnull;

/**
 * @author nimmyr
 * This interface provide hooks to parse and extract necessary details from icap response message.
 */
public interface IcapResponseConsumer {
    /**
     * Parse and extract icap response message.
     * @param status ICAP response status
     * @param icapMessage ICAP message
     * @return Icapresult
     * @throws IcapException icapexception
     */
    IcapResult responseReceived(final int status, @Nonnull final IcapMessage icapMessage) throws IcapException;
}
