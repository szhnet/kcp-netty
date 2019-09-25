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

    UkcpChannelConfig setFastResend(int fastResend);

    int getFastLimit();

    UkcpChannelConfig setFastLimit(int fastLimit);

    boolean isNocwnd();

    UkcpChannelConfig setNocwnd(boolean nc);

    int getMinRto();

    UkcpChannelConfig setMinRto(int minRto);

    int getMtu();

    UkcpChannelConfig setMtu(int mtu);

    int getRcvWnd();

    UkcpChannelConfig setRcvWnd(int rcvWnd);

    int getSndWnd();

    UkcpChannelConfig setSndWnd(int sndWnd);

    boolean isStream();

    UkcpChannelConfig setStream(boolean stream);

    int getDeadLink();

    UkcpChannelConfig setDeadLink(int deadLink);

    boolean isAutoSetConv();

    UkcpChannelConfig setAutoSetConv(boolean autoSetConv);

    boolean isFastFlush();

    UkcpChannelConfig setFastFlush(boolean fastFlush);

    boolean isMergeSegmentBuf();

    UkcpChannelConfig setMergeSegmentBuf(boolean mergeSegmentBuf);

}
