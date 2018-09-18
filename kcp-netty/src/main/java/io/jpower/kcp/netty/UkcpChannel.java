package io.jpower.kcp.netty;

import java.net.InetSocketAddress;

import io.netty.channel.Channel;

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
