package com.lafaspot.icap.client.codec;

import com.lafaspot.icap.client.IcapResponseConsumer;
import com.lafaspot.icap.client.session.IcapSession;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;

/**
 * Decoder to parse ICAP messages from Symantec server.
 *
 * @author kraman
 *
 */
public class IcapMessageDecoder extends ReplayingDecoder<IcapMessage> {

    /** The logger object. */
    private final Logger logger;

    private IcapResponseConsumer icapResponseConsumer;

    /**
     * Constructor for the decoder.
     *
     * @param logger the logger object
     */
    public IcapMessageDecoder(@Nonnull final Logger logger, @Nonnull final IcapResponseConsumer icapResponseConsumer) {
        super(new IcapMessage(logger));
        this.logger = logger;
        this.icapResponseConsumer = icapResponseConsumer;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf buf, final List<Object> out) throws Exception {

        // logger.debug("<- replay ri " + buf.readerIndex() + ", wi " + buf.writerIndex() + ", th " + Thread.currentThread().getId(), null);
        IcapMessage msg = state();
        msg.parse(buf, this);
        if (msg.parsingDone()) {
            out.add(msg);
            final IcapMessage newMsg = new IcapMessage(logger);
            state(newMsg);
        }

    }

    public IcapResponseConsumer getIcapResponseConsumer() {
        return icapResponseConsumer;
    }
}
