/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.codec.IcapMessage;

/**
 * @author kraman
 *
 */
public class IcapChannelHandler extends SimpleChannelInboundHandler<IcapMessage> {

    /** The IcapSession pointer. */
    private final IcapSession session;

    /**
     * Constructor to create the handler.
     * 
     * @param session the session object
     */
    public IcapChannelHandler(@Nonnull final IcapSession session) {
        this.session = session;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final IcapMessage msg) throws Exception {
        messageReceived(ctx, msg);
    }

    /**
     * Called on incoming message in the channel.
     * 
     * @param ctx the handler context
     * @param msg incomging message
     * @throws Exception on failure
     */
    protected void messageReceived(final ChannelHandlerContext ctx, final IcapMessage msg) throws Exception {
        session.processResponse(msg);
    }
}
