package io.jpower.kcp.netty;

import io.netty.channel.Channel;

import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public interface UkcpChannel extends Channel {

    int conv();

    @Override
    UkcpChannelConfig config();

    UkcpChannel conv(int conv);

    @Override
    InetSocketAddress localAddress();

    @Override
    InetSocketAddress remoteAddress();

}
