package io.jpower.kcp.netty;

import static io.jpower.kcp.netty.Consts.sheduleUpdateLog;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.SelectorProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.jpower.kcp.netty.internal.CodecOutputList;
import io.jpower.kcp.netty.internal.ReItrHashMap;
import io.jpower.kcp.netty.internal.ReusableIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelMetadata;
import io.netty.channel.ChannelOutboundBuffer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.RecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.nio.AbstractNioMessageChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.SocketUtils;
import io.netty.util.internal.StringUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public final class UkcpServerChannel extends AbstractNioMessageChannel implements ServerChannel, Runnable {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(UkcpServerChannel.class);

    private static final ChannelMetadata METADATA = new ChannelMetadata(false);

    private static final SelectorProvider DEFAULT_SELECTOR_PROVIDER = SelectorProvider.provider();
    private static final String EXPECTED_TYPES =
            " (expected: " + StringUtil.simpleClassName(DatagramPacket.class) + ')';

    private final DefaultUkcpServerChannelConfig config;

    private final ReItrHashMap<SocketAddress, UkcpServerChildChannel> childChannelMap = new ReItrHashMap<>();

    private final ReusableIterator<Map.Entry<SocketAddress, UkcpServerChildChannel>> childChannelMapItr =
            childChannelMap.entrySet().iterator();

    private final ReItrHashMap<SocketAddress, CloseWaitKcp> closeWaitKcpMap = new ReItrHashMap<>();

    private final ReusableIterator<Map.Entry<SocketAddress, CloseWaitKcp>> closeWaitKcpMapItr =
            closeWaitKcpMap.entrySet().iterator();

    private final KcpOutput output = new UkcpServerOutput();

    private int tsUpdate;

    private boolean scheduleUpdate;

    private boolean scheduleCloseWait = false;

    private List<UkcpServerChildChannel> writeChannels = new ArrayList<>();

    private List<Object> closeChildList = new ArrayList<>();

    private Runnable closeWaitRunner = new CloseWaitRun();

    private static DatagramChannel newSocket(SelectorProvider provider) {
        try {
            /**
             *  Use the {@link SelectorProvider} to open {@link SocketChannel} and so remove condition in
             *  {@link SelectorProvider#provider()} which is called by each DatagramChannel.open() otherwise.
             *
             *  See <a href="https://github.com/netty/netty/issues/2308">#2308</a>.
             */
            return provider.openDatagramChannel();
        } catch (IOException e) {
            throw new ChannelException("Failed to open a socket.", e);
        }
    }

    public UkcpServerChannel() {
        this(newSocket(DEFAULT_SELECTOR_PROVIDER));
    }

    public UkcpServerChannel(SelectorProvider provider) {
        this(newSocket(provider));
    }

    public UkcpServerChannel(DatagramChannel socket) {
        super(null, socket, SelectionKey.OP_READ);
        config = new DefaultUkcpServerChannelConfig(this, socket.socket());
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

    @Override
    public UkcpServerChannelConfig config() {
        return config;
    }

    @Override
    protected UkcpServerUnsafe newUnsafe() {
        return new UkcpServerUnsafe();
    }

    @Override
    public boolean isActive() {
        DatagramChannel ch = javaChannel();
        return ch.isOpen() && ch.socket().isBound();
    }

    @Override
    protected DatagramChannel javaChannel() {
        return (DatagramChannel) super.javaChannel();
    }

    @Override
    protected SocketAddress localAddress0() {
        return javaChannel().socket().getLocalSocketAddress();
    }

    @Override
    protected SocketAddress remoteAddress0() {
        return javaChannel().socket().getRemoteSocketAddress();
    }

    @Override
    protected void doBind(SocketAddress localAddress) throws Exception {
        doBind0(localAddress);
    }

    private void doBind0(SocketAddress localAddress) throws Exception {
        if (PlatformDependent.javaVersion() >= 7) {
            SocketUtils.bind(javaChannel(), localAddress);
        } else {
            javaChannel().socket().bind(localAddress);
        }
    }

    @Override
    protected void doClose() throws Exception {
        Exception exception = null;
        try {
            javaChannel().close();
        } catch (Exception t) {
            exception = t;
        }
        // close child channel
        for (Iterator<Map.Entry<SocketAddress, UkcpServerChildChannel>> itr = childChannelMapItr.rewind(); itr
                .hasNext(); ) {
            UkcpServerChildChannel childCh = itr.next().getValue();
            Unsafe childUnsafe = childCh.unsafe();
            try {
                childUnsafe.close(childUnsafe.voidPromise());
            } catch (Exception e) {
                log.error("Failed to close a child channel. childChannel={}", childCh);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    @Override
    protected int doReadMessages(List<Object> buf) throws Exception {
        DatagramChannel ch = javaChannel();
        UkcpServerChannelConfig config = config();
        RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();

        ByteBuf data = allocHandle.allocate(config.getAllocator());
        allocHandle.attemptedBytesRead(data.writableBytes());
        boolean free = true;
        try {
            ByteBuffer nioData = data.internalNioBuffer(data.writerIndex(), data.writableBytes());
            int pos = nioData.position();
            InetSocketAddress remoteAddress = (InetSocketAddress) ch.receive(nioData);
            if (remoteAddress == null) {
                return 0;
            }

            allocHandle.lastBytesRead(nioData.position() - pos);
            buf.add(UkcpPacket.newInstance(data.writerIndex(data.writerIndex() + allocHandle.lastBytesRead()),
                    remoteAddress));
            free = false;
            return 1;
        } catch (Throwable cause) {
            PlatformDependent.throwException(cause);
            return -1;
        } finally {
            if (free) {
                data.release();
            }
        }
    }

    @Override
    protected boolean doWriteMessage(Object msg, ChannelOutboundBuffer in) throws Exception {
        UkcpPacket packet = (UkcpPacket) msg;
        InetSocketAddress remoteAddress = packet.remoteAddress();
        ByteBuf data = packet.content();

        final int dataLen = data.readableBytes();
        if (dataLen == 0) {
            return true;
        }

        final ByteBuffer nioData = data.internalNioBuffer(data.readerIndex(), dataLen);
        final int writtenBytes;
        writtenBytes = javaChannel().send(nioData, remoteAddress);
        return writtenBytes > 0;
    }

    @Override
    protected Object filterOutboundMessage(Object msg) {
        if (msg instanceof UkcpPacket) {
            UkcpPacket p = (UkcpPacket) msg;
            ByteBuf content = p.content();
            if (isSingleDirectBuffer(content)) {
                return p;
            }
            content.retain(); // newDirectBuffer method call release method of content
            UkcpPacket np = UkcpPacket.newInstance(newDirectBuffer(content), p.remoteAddress());
            p.release();
            return np;
        }

        throw new UnsupportedOperationException(
                "unsupported message type: " + StringUtil.simpleClassName(msg) + EXPECTED_TYPES);
    }

    /**
     * Checks if the specified buffer is a direct buffer and is composed of a single NIO buffer.
     * (We check this because otherwise we need to make it a non-composite buffer.)
     */
    private static boolean isSingleDirectBuffer(ByteBuf buf) {
        return buf.isDirect() && buf.nioBufferCount() == 1;
    }

    @Override
    protected boolean continueOnWriteError() {
        // Continue on write error as a DatagramChannel can write to multiple remote peers
        //
        // See https://github.com/netty/netty/issues/2665
        return true;
    }

    @Override
    public InetSocketAddress localAddress() {
        return (InetSocketAddress) super.localAddress();
    }

    @Override
    public InetSocketAddress remoteAddress() {
        return (InetSocketAddress) super.remoteAddress();
    }

    @Override
    protected boolean closeOnReadError(Throwable cause) {
        // We do not want to close on SocketException when using DatagramChannel as we usually can continue receiving.
        // See https://github.com/netty/netty/issues/5893
        if (cause instanceof SocketException) {
            return false;
        }
        return super.closeOnReadError(cause);
    }

    @Override
    protected boolean doConnect(SocketAddress remoteAddress, SocketAddress localAddress) throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doFinishConnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void doDisconnect() throws Exception {
        throw new UnsupportedOperationException();
    }

    private UkcpServerChildChannel getOrCreateUkcpChannel(InetSocketAddress remoteAddress) {
        UkcpServerChildChannel ch = childChannelMap.get(remoteAddress);
        if (ch == null) {
            ch = createChildChannel(remoteAddress);
            childChannelMap.put(remoteAddress, ch);
        }
        return ch;
    }

    private UkcpServerChildChannel createChildChannel(InetSocketAddress remoteAddress) {
        Ukcp ukcp = new Ukcp(0, output); // temp conv, need to set conv in outter
        UkcpServerChildChannel ch = new UkcpServerChildChannel(this, ukcp, remoteAddress);

        ChannelPipeline pipeline = pipeline();
        pipeline.fireChannelRead(ch);
        pipeline.fireChannelReadComplete();

        if (log.isDebugEnabled()) {
            log.debug("Create childChannel. remoteAddress={}", remoteAddress);
        }

        if (!this.scheduleUpdate) { // haven't schedule update
            int current = Utils.milliSeconds();
            int tsUp = ch.kcpCheck(current);
            ch.kcpTsUpdate(tsUp);
            scheduleUpdate(tsUp, current);
        }

        return ch;
    }

    private void scheduleUpdate(int tsUpdate, int current) {
        if (sheduleUpdateLog.isDebugEnabled()) {
            sheduleUpdateLog.debug("schedule delay: " + (tsUpdate - current));
        }
        this.tsUpdate = tsUpdate;
        this.scheduleUpdate = true;
        eventLoop().schedule(this, tsUpdate - current, TimeUnit.MILLISECONDS);
    }

    void doCloseChildChannel(UkcpServerChildChannel childChannel) {
        UkcpServerChildChannel rmCh = childChannelMap.remove(childChannel.remoteAddress());
        if (rmCh == null) {
            log.error("Not found childChannel. remoteAddress={}", childChannel.remoteAddress());
        }
        if (rmCh != childChannel) {
            log.error("Mismatch instance of childChannel. remoteAddress={}", childChannel.remoteAddress());
        }
        Ukcp ukcp = childChannel.ukcp();
        if (isActive() && ukcp.getState() != -1 && ukcp.checkFlush()) {
            ukcp.setClosed(false);
            int current = Utils.milliSeconds();
            closeWaitKcpMap.put(ukcp.channel().remoteAddress(), new CloseWaitKcp(ukcp, current + Consts
                    .CLOSE_WAIT_TIME));
            tryScheduleCloseWait();
            if (!scheduleUpdate) {
                scheduleUpdate(ukcp.check(current), current); // schedule update
            }
        } else {
            ukcp.setClosed(true);
        }
    }

    private void tryScheduleCloseWait() {
        if (closeWaitKcpMap.isEmpty() || scheduleCloseWait) {
            return;
        }
        eventLoop().schedule(closeWaitRunner, Consts.CLOSE_WAIT_TIME / 2, TimeUnit.MILLISECONDS);
        scheduleCloseWait = true;
    }

    @Override
    public void run() {
        int current = Utils.milliSeconds();
        int nextTsUpdate = 0;
        boolean nextSchedule = false;

        for (Iterator<Map.Entry<SocketAddress, UkcpServerChildChannel>> itr = childChannelMapItr.rewind(); itr
                .hasNext(); ) {
            UkcpServerChildChannel childCh = itr.next().getValue();
            if (!childCh.isActive()) {
                continue;
            }
            int tsUp = childCh.kcpTsUpdate();
            int nextChildTsUp = 0;
            boolean nextChildSchedule = false;
            Throwable exception = null;
            if (Utils.itimediff(current, tsUp) >= 0) {
                try {
                    nextChildTsUp = childCh.kcpUpdate(current);
                    nextChildSchedule = true;
                } catch (Throwable t) {
                    exception = t;
                }

                if (childCh.kcpState() == -1 && exception == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("getState=-1 after update(). channel={}", childCh);
                    }
                    exception = new KcpException("State=-1 after update()");
                }

                if (exception != null) {
                    closeChildList.add(new ExceptionCloseWrapper(childCh, exception));
                    nextChildTsUp = 0;
                    nextChildSchedule = false;
                } else {
                    if (childCh.isFlushPending() && childCh.kcpCanSend()) {
                        writeChannels.add(childCh);
                    }
                }
            } else {
                nextChildTsUp = tsUp;
                nextChildSchedule = true;
            }
            if (nextChildSchedule && (!nextSchedule || Utils.itimediff(nextTsUpdate, nextChildTsUp) > 0)) {
                nextTsUpdate = nextChildTsUp;
                nextSchedule = true;
            }
        }

        if (writeChannels.size() > 0) {
            for (UkcpServerChildChannel childCh : writeChannels) {
                childCh.unsafe().forceFlush();
            }
            writeChannels.clear();
        }

        if (closeWaitKcpMap.size() > 0) {
            for (Iterator<Map.Entry<SocketAddress, CloseWaitKcp>> itr = closeWaitKcpMapItr.rewind(); itr.hasNext(); ) {
                CloseWaitKcp w = itr.next().getValue();
                Ukcp ukcp = w.ukcp;

                int tsUp = ukcp.getTsUpdate();
                int nextChildTsUp = 0;
                boolean nextChildSchedule = false;
                Throwable exception = null;
                if (Utils.itimediff(current, tsUp) >= 0) {
                    try {
                        nextChildTsUp = ukcp.update(current);
                        nextChildSchedule = true;
                    } catch (Throwable t) {
                        exception = t;
                        itr.remove();
                        ukcp.setKcpClosed();
                        log.error("Terminate closeWaitKcp. ukcp={}, cause={}", ukcp, "update error", t);
                    }

                    if (ukcp.getState() == -1 && exception == null) {
                        itr.remove();
                        ukcp.setKcpClosed();
                        nextChildTsUp = 0;
                        nextChildSchedule = false;
                        if (log.isDebugEnabled()) {
                            log.debug("Terminate closeWaitKcp. ukcp={}, cause={}", ukcp, "update -1");
                        }
                    }
                } else {
                    nextChildTsUp = tsUp;
                    nextChildSchedule = true;
                }
                if (nextChildSchedule && (!nextSchedule || Utils.itimediff(nextTsUpdate, nextChildTsUp) > 0)) {
                    nextTsUpdate = nextChildTsUp;
                    nextSchedule = true;
                }
            }
        }

        tsUpdate = nextTsUpdate;
        scheduleUpdate = nextSchedule;
        if (nextSchedule) {
            scheduleUpdate(tsUpdate, current);
        }

        if (closeChildList.size() > 0) {
            handleCloseChildList();
        }
    }

    void updateChildKcp(UkcpServerChildChannel childCh) {
        int current = Utils.milliSeconds();
        Throwable exception = null;
        try {
            childCh.kcpUpdate(current);
        } catch (Throwable t) {
            exception = t;
        }

        if (childCh.kcpState() == -1 && exception == null) {
            if (log.isDebugEnabled()) {
                log.debug("getState=-1 after update(). channel={}", childCh);
            }
            exception = new KcpException("State=-1 after update()");
        }

        if (exception != null) {
            Utils.fireExceptionAndClose(childCh, exception, true);
        }
    }

    private void handleCloseChildList() {
        for (Object obj : closeChildList) {
            if (obj instanceof UkcpServerChildChannel) {
                UkcpServerChildChannel childCh = (UkcpServerChildChannel) obj;
                Unsafe childUnsafe = childCh.unsafe();
                childUnsafe.close(childUnsafe.voidPromise());
            } else {
                ExceptionCloseWrapper wrap = (ExceptionCloseWrapper) obj;
                Utils.fireExceptionAndClose(wrap.channel, wrap.exception, true);
            }
        }
        closeChildList.clear();
    }

    private final class UkcpServerUnsafe extends AbstractNioUnsafe {

        private final List<Object> readBuf = new ArrayList<Object>();

        @Override
        public void read() {
            assert eventLoop().inEventLoop();
            final ChannelConfig config = config();
            final ChannelPipeline pipeline = pipeline();
            final RecvByteBufAllocator.Handle allocHandle = unsafe().recvBufAllocHandle();
            allocHandle.reset(config);

            boolean closed = false;
            Throwable exception = null;
            try {
                try {
                    do {
                        int localRead = doReadMessages(readBuf);
                        if (localRead == 0) {
                            break;
                        }
                        if (localRead < 0) {
                            closed = true;
                            break;
                        }

                        allocHandle.incMessagesRead(localRead);
                    } while (allocHandle.continueReading());
                } catch (Throwable t) {
                    exception = t;
                }

                int readBufSize = readBuf.size();
                for (int i = 0; i < readBufSize; i++) {
                    Throwable subException = null;
                    UkcpPacket packet = (UkcpPacket) readBuf.get(i);
                    InetSocketAddress remoteAddress = packet.remoteAddress();
                    ByteBuf byteBuf = packet.content();

                    CloseWaitKcp closeWaitKcp = closeWaitKcpMap.get(remoteAddress);
                    if (closeWaitKcp != null) {
                        CodecOutputList<ByteBuf> recvBufList = null;
                        Ukcp ukcp = closeWaitKcp.ukcp;
                        try {
                            ukcp.input(byteBuf);
                            ukcp.setTsUpdate(Utils.milliSeconds()); // update kcp

                            while (ukcp.canRecv()) {
                                if (recvBufList == null) {
                                    recvBufList = CodecOutputList.<ByteBuf>newInstance();
                                }
                                ukcp.receive(recvBufList);
                            }
                        } catch (Throwable t) {
                            subException = t;
                        } finally {
                            packet.release();
                        }
                        if (recvBufList != null) {
                            clearAndRelease(recvBufList);
                            recvBufList.recycle();
                        }

                        if (subException != null) {
                            closeWaitKcpMap.remove(remoteAddress);
                            ukcp.setKcpClosed();
                            log.error("Terminate closeWaitKcp. ukcp={}, cause={}", ukcp, "read error", subException);
                        }
                    } else {
                        UkcpServerChildChannel childCh = getOrCreateUkcpChannel(remoteAddress);
                        if (!childCh.isActive()) {
                            packet.release();
                            continue;
                        }

                        UkcpChannelConfig childConfig = childCh.config();
                        ChannelPipeline childPipeline = childCh.pipeline();
                        boolean mergeSegmentBuf = childConfig.isMergeSegmentBuf();
                        CodecOutputList<ByteBuf> recvBufList = null;
                        boolean recv = false;
                        try {
                            childCh.kcpInput(byteBuf);
                            childCh.kcpTsUpdate(Utils.milliSeconds()); // update kcp

                            if (mergeSegmentBuf) {
                                ByteBufAllocator childAllocator = childConfig.getAllocator();

                                int peekSize;
                                while ((peekSize = childCh.kcpPeekSize()) >= 0) {
                                    recv = true;
                                    ByteBuf recvBuf = childAllocator.ioBuffer(peekSize);
                                    childCh.kcpReceive(recvBuf);

                                    childPipeline.fireChannelRead(recvBuf);
                                }
                            } else {
                                while (childCh.kcpCanRecv()) {
                                    recv = true;
                                    if (recvBufList == null) {
                                        recvBufList = CodecOutputList.<ByteBuf>newInstance();
                                    }
                                    childCh.kcpReceive(recvBufList);
                                }
                            }

                        } catch (Throwable t) {
                            subException = t;
                        } finally {
                            packet.release();
                        }
                        if (recv) {
                            if (mergeSegmentBuf) {
                                childPipeline.fireChannelReadComplete();
                            } else {
                                Utils.fireChannelRead(childCh, recvBufList);
                                recvBufList.recycle();
                            }
                        }

                        if (subException != null) {
                            Utils.fireExceptionAndClose(childCh, subException, true);
                        }
                    }
                }
                readBuf.clear();
                allocHandle.readComplete();

                if (exception != null) {
                    closed = closeOnReadError(exception);

                    pipeline.fireExceptionCaught(exception);
                }

                if (closed) {
                    if (isOpen()) {
                        close(voidPromise());
                    }
                }
            } finally {
                // Check if there is a readPending which was not processed yet.
                // This could be for two reasons:
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelRead(...) method
                // * The user called Channel.read() or ChannelHandlerContext.read() in channelReadComplete(...) method
                //
                // See https://github.com/netty/netty/issues/2254
                if (!config.isAutoRead()) {
                    removeReadOp();
                }
            }
        }

        private void clearAndRelease(CodecOutputList<ByteBuf> bufList) {
            int size = bufList.size();
            for (int i = 0; i < size; i++) {
                ByteBuf msg = bufList.getUnsafe(i);
                msg.release();
            }
        }
    }

    private class UkcpServerOutput implements KcpOutput {

        @Override
        public void out(ByteBuf data, Kcp kcp) {
            UkcpServerChildChannel channel = (UkcpServerChildChannel) kcp.getUser();
            NioUnsafe unsafe = unsafe();
            unsafe.write(UkcpPacket.newInstance(data, channel.remoteAddress()), unsafe.voidPromise());
            unsafe.flush();
        }

    }

    private static class ExceptionCloseWrapper {

        final UkcpServerChildChannel channel;

        final Throwable exception;

        ExceptionCloseWrapper(UkcpServerChildChannel channel, Throwable exception) {
            this.channel = channel;
            this.exception = exception;
        }

    }

    private static class CloseWaitKcp {

        final Ukcp ukcp;

        final int closeTime;

        CloseWaitKcp(Ukcp ukcp, int closeTime) {
            this.ukcp = ukcp;
            this.closeTime = closeTime;
        }
    }

    private class CloseWaitRun implements Runnable {

        @Override
        public void run() {
            scheduleCloseWait = false;

            int current = Utils.milliSeconds();
            for (Iterator<Map.Entry<SocketAddress, CloseWaitKcp>> itr = closeWaitKcpMapItr.rewind(); itr.hasNext(); ) {
                CloseWaitKcp w = itr.next().getValue();
                Ukcp ukcp = w.ukcp;
                if (Utils.itimediff(current, w.closeTime) >= 0) {
                    ukcp.setKcpClosed();
                    itr.remove();
                    if (log.isDebugEnabled()) {
                        log.debug("Terminate closeWaitKcp. ukcp={}, cause={}", ukcp, "timeout");
                    }
                } else if (!ukcp.checkFlush()) {
                    ukcp.setKcpClosed();
                    itr.remove();
                    if (log.isDebugEnabled()) {
                        log.debug("Terminate closeWaitKcp. ukcp={}, cause={}", ukcp, "no flush");
                    }
                }
            }

            tryScheduleCloseWait();
        }

    }

}
