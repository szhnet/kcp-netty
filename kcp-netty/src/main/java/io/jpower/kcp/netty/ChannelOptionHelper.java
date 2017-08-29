package io.jpower.kcp.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.UkcpServerBootstrap;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class ChannelOptionHelper {

    public static Bootstrap nodelay(Bootstrap b, boolean nodelay, int interval, int fastResend, boolean nocwnd) {
        b.option(UkcpChannelOption.UKCP_NODELAY, nodelay)
                .option(UkcpChannelOption.UKCP_INTERVAL, interval)
                .option(UkcpChannelOption.UKCP_FAST_RESEND, fastResend)
                .option(UkcpChannelOption.UKCP_NOCWND, nocwnd);
        return b;
    }

    public static UkcpServerBootstrap nodelay(UkcpServerBootstrap b, boolean nodelay, int interval, int fastResend,
                                              boolean nocwnd) {
        b.childOption(UkcpChannelOption.UKCP_NODELAY, nodelay)
                .childOption(UkcpChannelOption.UKCP_INTERVAL, interval)
                .childOption(UkcpChannelOption.UKCP_FAST_RESEND, fastResend)
                .childOption(UkcpChannelOption.UKCP_NOCWND, nocwnd);
        return b;
    }

}
