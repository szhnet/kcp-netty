package io.jpower.kcp.netty;

import static io.netty.channel.ChannelOption.IP_TOS;
import static io.netty.channel.ChannelOption.SO_RCVBUF;
import static io.netty.channel.ChannelOption.SO_REUSEADDR;
import static io.netty.channel.ChannelOption.SO_SNDBUF;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.Objects;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultChannelConfig;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.MessageSizeEstimator;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.WriteBufferWaterMark;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class DefaultUkcpServerChannelConfig extends DefaultChannelConfig implements UkcpServerChannelConfig {

    private final DatagramSocket javaSocket;

    public DefaultUkcpServerChannelConfig(UkcpServerChannel channel, DatagramSocket javaSocket) {
        super(channel, new FixedRecvByteBufAllocator(Consts.FIXED_RECV_BYTEBUF_ALLOCATE_SIZE));
        this.javaSocket = Objects.requireNonNull(javaSocket, "javaSocket");
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(
                super.getOptions(),
                SO_RCVBUF, SO_SNDBUF, SO_REUSEADDR, IP_TOS);
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <T> T getOption(ChannelOption<T> option) {
        if (option == SO_RCVBUF) {
            return (T) Integer.valueOf(getUdpReceiveBufferSize());
        }
        if (option == SO_SNDBUF) {
            return (T) Integer.valueOf(getUdpSendBufferSize());
        }
        if (option == SO_REUSEADDR) {
            return (T) Boolean.valueOf(isReuseAddress());
        }
        if (option == IP_TOS) {
            return (T) Integer.valueOf(getUdpTrafficClass());
        }
        return super.getOption(option);
    }

    @Override
    @SuppressWarnings("deprecation")
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == SO_RCVBUF) {
            setUdpReceiveBufferSize((Integer) value);
        } else if (option == SO_SNDBUF) {
            setUdpSendBufferSize((Integer) value);
        } else if (option == SO_REUSEADDR) {
            setReuseAddress((Boolean) value);
        } else if (option == IP_TOS) {
            setUdpTrafficClass((Integer) value);
        } else {
            return super.setOption(option, value);
        }

        return true;
    }

    @Override
    public int getUdpReceiveBufferSize() {
        try {
            return javaSocket.getReceiveBufferSize();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public UkcpServerChannelConfig setUdpReceiveBufferSize(int receiveBufferSize) {
        try {
            javaSocket.setReceiveBufferSize(receiveBufferSize);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public int getUdpSendBufferSize() {
        try {
            return javaSocket.getSendBufferSize();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public UkcpServerChannelConfig setUdpSendBufferSize(int sendBufferSize) {
        try {
            javaSocket.setSendBufferSize(sendBufferSize);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public int getUdpTrafficClass() {
        try {
            return javaSocket.getTrafficClass();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public UkcpServerChannelConfig setUdpTrafficClass(int trafficClass) {
        try {
            javaSocket.setTrafficClass(trafficClass);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public boolean isReuseAddress() {
        try {
            return javaSocket.getReuseAddress();
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
    }

    @Override
    public UkcpServerChannelConfig setReuseAddress(boolean reuseAddress) {
        try {
            javaSocket.setReuseAddress(reuseAddress);
        } catch (SocketException e) {
            throw new ChannelException(e);
        }
        return this;
    }

    @Override
    public UkcpServerChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    @Deprecated
    public UkcpServerChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        super.setMaxMessagesPerRead(maxMessagesPerRead);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setAllocator(ByteBufAllocator allocator) {
        super.setAllocator(allocator);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        super.setRecvByteBufAllocator(allocator);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setAutoClose(boolean autoClose) {
        super.setAutoClose(autoClose);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        super.setWriteBufferWaterMark(writeBufferWaterMark);
        return this;
    }

    @Override
    public UkcpServerChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        super.setMessageSizeEstimator(estimator);
        return this;
    }

}
