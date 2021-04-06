package io.jpower.kcp.netty;


import java.util.concurrent.TimeUnit;

import io.jpower.kcp.netty.internal.CodecOutputList;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
class Utils {

    static int milliSeconds() {
        return (int) TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
    }

    static int itimediff(int later, int earlier) {
        return later - earlier;
    }

    static void fireExceptionAndClose(Channel channel, Throwable t, boolean close) {
        channel.pipeline().fireExceptionCaught(t);
        if (channel.isActive()) {
            Channel.Unsafe unsafe = channel.unsafe();
            unsafe.close(unsafe.voidPromise());
        }
    }

    static void fireChannelRead(Channel ch, CodecOutputList<ByteBuf> bufList) {
        ChannelPipeline pipeline = ch.pipeline();
        int size = bufList.size();
        if (size <= 0) {
            return;
        }
        for (int i = 0; i < size; i++) {
            ByteBuf msg = bufList.getUnsafe(i);
            pipeline.fireChannelRead(msg);
        }
        pipeline.fireChannelReadComplete();
    }

}
