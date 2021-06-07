package com.lafaspot.icap.client;

import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.exception.IcapException;

import javax.annotation.Nonnull;

public interface IcapResponseConsumer {
    void responseReceived(final int status, @Nonnull final IcapMessage icapMessage) throws IcapException;
}
