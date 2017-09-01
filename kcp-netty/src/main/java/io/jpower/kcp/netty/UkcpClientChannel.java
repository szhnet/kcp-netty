package io.jpower.kcp.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoop;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public final class UkcpClientChannel extends AbstractChannel implements UkcpChannel, Runnable {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(UkcpClientChannel.class);

    private static final InternalLogger sheduleUpdateLog = InternalLoggerFactory.getInstance("io.jpower.kcp.netty" +
            ".sheduleUpdate");

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(ByteBuf.class) + ')';

    private final DefaultUkcpClientChannelConfig config;

    private final UkcpClientUdpChannel udpChannel;

    private final Ukcp ukcp;

    private final KcpOutput output = new UkcpClientOutput();

    private long tsUpdate = -1;

    boolean closeAnother = false;

    public UkcpClientChannel() {
        super(null);
        this.udpChannel = new UkcpClientUdpChannel(this);
        this.ukcp = createUkcp();
        this.config = new DefaultUkcpClientChannelConfig(this, ukcp, udpChannel.javaChannel().socket());
    }

    private Ukcp createUkcp() {
        Ukcp ukcp = new Ukcp(0, output); // temp conv, need to set conv in outter
        ukcp.channel(this);

        return ukcp;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public UkcpClientChannelConfig config() {
        return config;
    }

    @Override
    protected UkcpClientUnsafe newUnsafe() {
        return new UkcpClientUnsafe();
    }

    @Override
    public boolean isOpen() {
        return udpChannel.isOpen();
    }

    @Override
    public boolean isActive() {
        return udpChannel.isActive();
    }

    @Override
    protected boolean isCompatible(EventLoop loop) {
        return loop instanceof NioEventLoop;
    }

    @Override
    protected SocketAddress localAddress0() {
        return udpChannel.localAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return udpChannel.remoteAddress();
    }

    @Override
    protected void doRegister() throws Exception {
        eventLoop().register(udpChannel).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    forceClose(future.cause());
                }
            }
        });
    }

    private void forceClose(Throwable t) {
        unsafe().closeForcibly();
        ((ChannelPromise) closeFuture()).trySuccess();
        log.warn("Failed to register an UkcpClientUdpChannel: {}", this, t);
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        udpChannel.doBind(localAddress);
    }

    private boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        return udpChannel.doConnect(remoteAddress, localAddress);
    }

    @Override
    protected void doDisconnect() throws Exception {
        udpChannel.doDisconnect();
    }

    @Override
    protected void doClose() throws Exception {
        ukcp.setClosed(true);
        if (!closeAnother) {
            closeAnother = true;
            udpChannel.unsafe().close(udpChannel.unsafe().voidPromise());
        }
    }

    @Override
    protected void doBeginRead() throws Exception {
        udpChannel.doBeginRead();
    }

    @Override
    protected void doWrite(ChannelOutboundBuffer in) throws Exception {
        boolean sent = false;
        for (; ; ) {
            Object msg = in.current();
            if (msg == null) {
                break;
            }
            try {
                kcpSend((ByteBuf) msg);
                sent = true;

                in.remove();
            } catch (IOException e) {
                throw e; // throw exception and close channel
            }
        }

        if (sent) {
            // update kcp
            if (ukcp.isFastFlush()) {
                updateKcp();
            } else {
                kcpTsUpdate(-1);
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

    public int conv() {
        return ukcp.getConv();
    }

    public UkcpClientChannel conv(int conv) {
        ukcp.setConv(conv);
        return this;
    }

    void kcpReceive(List<ByteBuf> bufList) {
        ukcp.receive(bufList);
    }

    void kcpInput(ByteBuf buf) throws IOException {
        ukcp.input(buf);
    }

    void kcpSend(ByteBuf buf) throws IOException {
        ukcp.send(buf);
    }

    boolean kcpCanRecv() {
        return ukcp.canRecv();
    }

    int kcpPeekSize() {
        return ukcp.peekSize();
    }

    long kcpUpdate(long current) {
        return ukcp.update(current);
    }

    long kcpCheck(long current) {
        return ukcp.check(current);
    }

    long kcpTsUpdate() {
        return ukcp.getTsUpdate();
    }

    void kcpTsUpdate(long tsUpdate) {
        ukcp.setTsUpdate(tsUpdate);
    }

    int kcpState() {
        return ukcp.getState();
    }

    void scheduleUpdate(long tsUpdate, long current) {
        if (sheduleUpdateLog.isDebugEnabled()) {
            sheduleUpdateLog.debug("schedule delay: " + (tsUpdate - current));
        }
        this.tsUpdate = tsUpdate;
        eventLoop().schedule(this, tsUpdate - current, TimeUnit.MILLISECONDS);
    }

    @Override
    public void run() {
        long current = System.currentTimeMillis();

        long nextTsUpadte = -1;
        long tsUp = kcpTsUpdate();
        Throwable exception = null;
        if (current >= tsUp) {
            try {
                nextTsUpadte = kcpUpdate(current);
            } catch (Throwable t) {
                exception = t;
            }

            if (kcpState() == -1 && exception == null) {
                if (log.isDebugEnabled()) {
                    log.debug("getState=-1 after update(). channel={}", this);
                }
                exception = new KcpException("State=-1 after update()");
            }
        } else {
            nextTsUpadte = tsUp;
        }

        boolean close = false;
        if (exception != null) {
            close = true;
            nextTsUpadte = -1;
        }

        tsUpdate = nextTsUpadte;
        if (tsUpdate != -1) {
            scheduleUpdate(tsUpdate, current);
        }

        if (close) {
            Utils.fireExceptionAndClose(this, exception, true);
        }
    }

    private void updateKcp() {
        long current = System.currentTimeMillis();
        Throwable exception = null;
        try {
            kcpUpdate(current);
        } catch (Throwable t) {
            exception = t;
        }

        if (kcpState() == -1 && exception == null) {
            if (log.isDebugEnabled()) {
                log.debug("getState=-1 after update(). channel={}", this);
            }
            exception = new KcpException("State=-1 after update()");
        }

        if (exception != null) {
            Utils.fireExceptionAndClose(this, exception, true);
        }
    }

    private final class UkcpClientUnsafe extends AbstractUnsafe {

        @Override
        public void connect(SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) {
            if (!promise.setUncancellable() || !ensureOpen(promise)) {
                return;
            }

            try {
                boolean wasActive = isActive();
                if (doConnect(remoteAddress, localAddress)) {
                    fulfillConnectPromise(promise, wasActive);
                } else {
                    throw new Error();
                }
            } catch (Throwable t) {
                promise.tryFailure(annotateConnectException(t, remoteAddress));
                closeIfClosed();
            }
        }

        private void fulfillConnectPromise(ChannelPromise promise, boolean wasActive) {
            if (promise == null) {
                // Closed via cancellation and the promise has been notified already.
                return;
            }

            // Get the state as trySuccess() may trigger an ChannelFutureListener that will close the Channel.
            // We still need to ensure we call fireChannelActive() in this case.
            boolean active = isActive();

            // trySuccess() will return false if a user cancelled the connection attempt.
            boolean promiseSet = promise.trySuccess();

            // Regardless if the connection attempt was cancelled, channelActive() event should be triggered,
            // because what happened is what happened.
            if (!wasActive && active) {
                pipeline().fireChannelActive();
            }

            // If a user cancelled the connection attempt, close the channel, which is followed by channelInactive().
            if (!promiseSet) {
                close(voidPromise());
            }
        }

    }

    private class UkcpClientOutput implements KcpOutput {

        @Override
        public void out(ByteBuf data, Kcp kcp) {
            UkcpClientChannel ukcpChannel = (UkcpClientChannel) kcp.getUser();
            UkcpClientUdpChannel udpChannel = ukcpChannel.udpChannel;
            udpChannel.unsafe().write(data, udpChannel.voidPromise());
            udpChannel.unsafe().flush();
        }

    }

}
