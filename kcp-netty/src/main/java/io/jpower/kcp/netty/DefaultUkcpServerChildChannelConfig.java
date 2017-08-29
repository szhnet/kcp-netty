package io.jpower.kcp.netty;

import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.*;

import java.util.Map;
import java.util.Objects;

import static io.jpower.kcp.netty.UkcpChannelOption.*;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class DefaultUkcpServerChildChannelConfig extends DefaultChannelConfig implements UkcpChannelConfig {

    private final Ukcp ukcp;

    public DefaultUkcpServerChildChannelConfig(UkcpServerChildChannel channel, Ukcp ukcp) {
        super(channel, new FixedRecvByteBufAllocator(512));
        this.ukcp = Objects.requireNonNull(ukcp, "ukcp");
    }

    @Override
    @SuppressWarnings("deprecation")
    public Map<ChannelOption<?>, Object> getOptions() {
        return getOptions(
                super.getOptions(),
                UKCP_NODELAY, UKCP_INTERVAL, UKCP_FAST_RESEND, UKCP_NOCWND, UKCP_MTU, UKCP_STREAM, UKCP_AUTO_SET_CONV);
    }

    @Override
    @SuppressWarnings({"unchecked", "deprecation"})
    public <T> T getOption(ChannelOption<T> option) {
        if (option == UKCP_NODELAY) {
            return (T) Boolean.valueOf(isNodelay());
        }
        if (option == UKCP_INTERVAL) {
            return (T) Integer.valueOf(getInterval());
        }
        if (option == UKCP_FAST_RESEND) {
            return (T) Integer.valueOf(getFastResend());
        }
        if (option == UKCP_NOCWND) {
            return (T) Boolean.valueOf(isNocwnd());
        }
        if (option == UKCP_MTU) {
            return (T) Integer.valueOf(getMtu());
        }
        if (option == UKCP_STREAM) {
            return (T) Boolean.valueOf(isStream());
        }
        if (option == UKCP_AUTO_SET_CONV) {
            return (T) Boolean.valueOf(isAutoSetConv());
        }
        return super.getOption(option);
    }

    @Override
    @SuppressWarnings("deprecation")
    public <T> boolean setOption(ChannelOption<T> option, T value) {
        validate(option, value);

        if (option == UKCP_NODELAY) {
            setNodelay((Boolean) value);
        } else if (option == UKCP_INTERVAL) {
            setInterval((Integer) value);
        } else if (option == UKCP_FAST_RESEND) {
            setFastResend((Integer) value);
        } else if (option == UKCP_NOCWND) {
            setNocwnd((Boolean) value);
        } else if (option == UKCP_MTU) {
            setMtu((Integer) value);
        } else if (option == UKCP_STREAM) {
            setStream((Boolean) value);
        } else if (option == UKCP_AUTO_SET_CONV) {
            setAutoSetConv((Boolean) value);
        }

        return true;
    }

    @Override
    public boolean isNodelay() {
        return ukcp.isNodelay();
    }

    @Override
    public UkcpChannelConfig setNodelay(boolean nodelay) {
        ukcp.setNodelay(nodelay);
        return this;
    }

    @Override
    public int getInterval() {
        return ukcp.getInterval();
    }

    @Override
    public UkcpChannelConfig setInterval(int interval) {
        ukcp.setInterval(interval);
        return this;
    }

    @Override
    public int getFastResend() {
        return ukcp.getFastResend();
    }

    @Override
    public UkcpChannelConfig setFastResend(int fastResend) {
        ukcp.setFastResend(fastResend);
        return this;
    }

    @Override
    public boolean isNocwnd() {
        return ukcp.isNocwnd();
    }

    @Override
    public UkcpChannelConfig setNocwnd(boolean nocwnd) {
        ukcp.setNocwnd(nocwnd);
        return this;
    }

    @Override
    public int getMtu() {
        return ukcp.getMtu();
    }

    @Override
    public UkcpChannelConfig setMtu(int mtu) {
        ukcp.setMtu(mtu);
        return this;
    }

    @Override
    public boolean isStream() {
        return ukcp.isStream();
    }

    @Override
    public UkcpChannelConfig setStream(boolean stream) {
        ukcp.setStream(stream);
        return this;
    }

    @Override
    public boolean isAutoSetConv() {
        return ukcp.isAutoSetConv();
    }

    @Override
    public UkcpChannelConfig setAutoSetConv(boolean autoSetConv) {
        ukcp.setAutoSetConv(autoSetConv);
        return this;
    }

    @Override
    public UkcpChannelConfig setAllocator(ByteBufAllocator allocator) {
        super.setAllocator(allocator);
        ukcp.setByteBufAllocator(allocator);
        return this;
    }

    @Override
    public UkcpChannelConfig setWriteSpinCount(int writeSpinCount) {
        super.setWriteSpinCount(writeSpinCount);
        return this;
    }

    @Override
    public UkcpChannelConfig setConnectTimeoutMillis(int connectTimeoutMillis) {
        super.setConnectTimeoutMillis(connectTimeoutMillis);
        return this;
    }

    @Override
    @Deprecated
    public UkcpChannelConfig setMaxMessagesPerRead(int maxMessagesPerRead) {
        super.setMaxMessagesPerRead(maxMessagesPerRead);
        return this;
    }

    @Override
    public UkcpChannelConfig setRecvByteBufAllocator(RecvByteBufAllocator allocator) {
        super.setRecvByteBufAllocator(allocator);
        return this;
    }

    @Override
    public UkcpChannelConfig setAutoRead(boolean autoRead) {
        super.setAutoRead(autoRead);
        return this;
    }

    @Override
    public UkcpChannelConfig setAutoClose(boolean autoClose) {
        super.setAutoClose(autoClose);
        return this;
    }

    @Override
    public UkcpChannelConfig setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
        super.setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        return this;
    }

    @Override
    public UkcpChannelConfig setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
        super.setWriteBufferLowWaterMark(writeBufferLowWaterMark);
        return this;
    }

    @Override
    public UkcpChannelConfig setWriteBufferWaterMark(WriteBufferWaterMark writeBufferWaterMark) {
        super.setWriteBufferWaterMark(writeBufferWaterMark);
        return this;
    }

    @Override
    public UkcpChannelConfig setMessageSizeEstimator(MessageSizeEstimator estimator) {
        super.setMessageSizeEstimator(estimator);
        return this;
    }

}
