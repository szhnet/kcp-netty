package io.jpower.kcp.netty;

import io.netty.channel.ChannelConfig;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public interface UkcpChannelConfig extends ChannelConfig {

    boolean isNodelay();

    UkcpChannelConfig setNodelay(boolean nodelay);

    int getInterval();

    UkcpChannelConfig setInterval(int interval);

    int getFastResend();

    UkcpChannelConfig setFastResend(int resend);

    boolean isNocwnd();

    UkcpChannelConfig setNocwnd(boolean nc);

    int getMtu();

    UkcpChannelConfig setMtu(int mtu);

    int getRcvWnd();

    UkcpChannelConfig setRcvWnd(int rcvWnd);

    int getSndWnd();

    UkcpChannelConfig setSndWnd(int sndWnd);

    boolean isStream();

    UkcpChannelConfig setStream(boolean stream);

    boolean isAutoSetConv();

    UkcpChannelConfig setAutoSetConv(boolean autoSetConv);

    boolean isFastFlush();

    UkcpChannelConfig setFastFlush(boolean fastFlush);

}
