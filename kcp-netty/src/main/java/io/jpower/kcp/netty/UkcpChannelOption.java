package io.jpower.kcp.netty;

import io.netty.channel.ChannelOption;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public final class UkcpChannelOption<T> extends ChannelOption<T> {

    public static final ChannelOption<Boolean> UKCP_NODELAY =
            valueOf(UkcpChannelOption.class, "UKCP_NODELAY");

    public static final ChannelOption<Integer> UKCP_INTERVAL =
            valueOf(UkcpChannelOption.class, "UKCP_INTERVAL");

    public static final ChannelOption<Integer> UKCP_FAST_RESEND =
            valueOf(UkcpChannelOption.class, "UKCP_FAST_RESEND");

    public static final ChannelOption<Boolean> UKCP_NOCWND =
            valueOf(UkcpChannelOption.class, "UKCP_NOCWND");

    public static final ChannelOption<Integer> UKCP_MTU =
            valueOf(UkcpChannelOption.class, "UKCP_MTU");

    public static final ChannelOption<Boolean> UKCP_STREAM =
            valueOf(UkcpChannelOption.class, "UKCP_STREAM");

    public static final ChannelOption<Boolean> UKCP_AUTO_SET_CONV =
            valueOf(UkcpChannelOption.class, "UKCP_AUTO_SET_CONV");

    @SuppressWarnings("deprecation")
    private UkcpChannelOption() {
        super(null);
    }

}
