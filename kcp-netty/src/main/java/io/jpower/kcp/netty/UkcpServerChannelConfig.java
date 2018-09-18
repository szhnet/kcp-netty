package io.jpower.kcp.netty;

import java.net.StandardSocketOptions;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public interface UkcpServerChannelConfig extends ChannelConfig {

    /**
     * Gets the {@link StandardSocketOptions#SO_RCVBUF} option.
     */
    int getUdpReceiveBufferSize();

    /**
     * Sets the {@link StandardSocketOptions#SO_RCVBUF} option.
     */
    UkcpServerChannelConfig setUdpReceiveBufferSize(int receiveBufferSize);

    /**
     * Gets the {@link StandardSocketOptions#SO_SNDBUF} option.
     */
    int getUdpSendBufferSize();

    /**
     * Sets the {@link StandardSocketOptions#SO_SNDBUF} option.
     */
    UkcpServerChannelConfig setUdpSendBufferSize(int sendBufferSize);

    /**
     * Gets the {@link StandardSocketOptions#IP_TOS} option.
     */
    int getUdpTrafficClass();

    /**
     * Sets the {@link StandardSocketOptions#IP_TOS} option.
     */
    UkcpServerChannelConfig setUdpTrafficClass(int trafficClass);

    /**
     * Gets the {@link StandardSocketOptions#SO_REUSEADDR} option.
     */
    boolean isReuseAddress();

    /**
     * Gets the {@link StandardSocketOptions#SO_REUSEADDR} option.
     */
    UkcpServerChannelConfig setReuseAddress(boolean reuseAddress);

    @Override
    @Deprecated
    UkcpServerChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead);

    @Override
    UkcpServerChannelConfig setWriteSpinCount(int writeSpinCount);

    @Override
    UkcpServerChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis);

    @Override
    UkcpServerChannelConfig setAllocator(ByteBufAllocator allocator);

    @Override
    UkcpServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator);

    @Override
    UkcpServerChannelConfig setAutoRead(boolean autoRead);

    @Override
    UkcpServerChannelConfig setAutoClose(boolean autoClose);

    @Override
    UkcpServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator);

    @Override
    UkcpServerChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark);

}
