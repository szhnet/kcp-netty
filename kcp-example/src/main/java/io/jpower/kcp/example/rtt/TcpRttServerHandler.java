package io.jpower.kcp.example.rtt;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class TcpRttServerHandler extends ChannelInboundHandlerAdapter {

    private static Logger log = LoggerFactory.getLogger(TcpRttServerHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = (ByteBuf) msg;
        short curCount = buf.getShort(buf.readerIndex());
        ctx.writeAndFlush(msg);

        if (curCount == -1) {
            ctx.close();
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        log.error("exceptionCaught", cause);
        ctx.close();
    }

}
