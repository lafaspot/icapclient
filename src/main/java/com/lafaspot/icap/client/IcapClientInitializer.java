/**
 *
 */
package com.lafaspot.icap.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * The client initilaized, setup the static encoder/decoders.
 *
 * @author kraman
 *
 */
public class IcapClientInitializer extends ChannelInitializer<SocketChannel> {

    /** String encoder to encode HTTP/ICAP headers. */
    private static final StringEncoder STRING_ENCODER = new StringEncoder();

    /** Byte encoder to encode ICAP attachment payload. */
    private static final ByteArrayEncoder BYTE_ENCODER = new ByteArrayEncoder();

    @Override
    protected void initChannel(final SocketChannel ch) throws Exception {
        ch.pipeline().addLast(STRING_ENCODER);
        ch.pipeline().addLast(BYTE_ENCODER);
    }

}

