package io.jpower.kcp.netty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.channel.AbstractChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPromise;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoop;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public final class UkcpServerChildChannel extends AbstractChannel implements UkcpChannel {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(UkcpServerChildChannel.class);

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ')';

    private final DefaultUkcpServerChildChannelConfig config;

    private final Ukcp ukcp;

    private final InetSocketAddress remoteAddress;

    private boolean flushPending;

    UkcpServerChildChannel(Channel parent, Ukcp ukcp, InetSocketAddress remoteAddress) {
        super(parent);
        this.config = new DefaultUkcpServerChildChannelConfig(this, ukcp);
        ukcp.channel(this);
        this.ukcp = ukcp;
        this.remoteAddress = remoteAddress;
    }

    @Override
    public UkcpServerChannel parent() {
        return (UkcpServerChannel) super.parent();
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public UkcpChannelConfig config() {
        return config;
    }

    @Override
    public boolean isOpen() {
        return ukcp.isActive();
    }

    @Override
    public boolean isActive() {
        return isOpen();
    }

    @Override
    public UkcpServerChildUnsafe unsafe() {
        return (UkcpServerChildUnsafe) super.unsafe();
    }

    @Override
    protected UkcpServerChildUnsafe newUnsafe() {
        return new UkcpServerChildUnsafe();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return parent().localAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return remoteAddress;
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doClose() throws Exception {
        parent().doCloseChildChannel(this); // callback parent
    }

    @Override
    protected void doBeginRead() throws Exception {
        // read operation be control by parent
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        boolean sent = false;
        for (; ; ) {
            Object msg = in.current();
            if (msg == null) {
                flushPending = false;
                break;
            }
            try {
                boolean done = false;
                if (kcpSend((ByteBuf) msg)) {
                    done = true;
                    sent = true;
                }

                if (done) {
                    in.remove();
                } else {
                    flushPending = true;
                    break;
                }
            } catch (IOException e) {
                throw e; // throw exception and close channel
            }
        }

        if (sent) {
            // update kcp
            if (ukcp.isFastFlush()) {
                parent().updateChildKcp(this);
            } else {
                kcpTsUpdate(Utils.milliSeconds());
            }
        }
    }

    @Override
    protected final Object filterOutboundMessage(Object msg) {
        if (msg instanceof ByteBuf) {
            return msg;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    boolean isFlushPending() {
        return flushPending;
    }

    Ukcp ukcp() {
        return ukcp;
    }

    public int conv() {
        return ukcp.getConv();
    }

    public UkcpServerChildChannel conv(int conv) {
        ukcp.setConv(conv);
        return this;
    }

    void kcpReceive(ByteBuf buf) throws IOException {
        ukcp.receive(buf);
    }

    void kcpReceive(List<ByteBuf> bufList) {
        ukcp.receive(bufList);
    }

    void kcpInput(ByteBuf buf) throws IOException {
        ukcp.input(buf);
    }

    boolean kcpSend(ByteBuf buf) throws IOException {
        if (ukcp.canSend(true)) {
            ukcp.send(buf);
            return true;
        } else {
            return false;
        }
    }

    boolean kcpCanRecv() {
        return ukcp.canRecv();
    }

    boolean kcpCanSend() {
        return ukcp.canSend(!flushPending);
    }

    int kcpPeekSize() {
        return ukcp.peekSize();
    }

    int kcpUpdate(int current) {
        return ukcp.update(current);
    }

    int kcpCheck(int current) {
        return ukcp.check(current);
    }

    int kcpTsUpdate() {
        return ukcp.getTsUpdate();
    }

    void kcpTsUpdate(int tsUpdate) {
        ukcp.setTsUpdate(tsUpdate);
    }

    int kcpState() {
        return ukcp.getState();
    }

    final class UkcpServerChildUnsafe extends AbstractUnsafe {

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            safeSetFailure(promise, new UnsupportedOperationException());
        }

        @Override
        protected void flush0() {
            if (isFlushPending()) {
                return;
            }
            super.flush0();
        }

        void forceFlush() {
            super.flush0();
        }
    }

}
