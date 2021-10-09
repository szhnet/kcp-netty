package io.jpower.kcp.netty;

import java.io.IOException;
import java.util.List;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * Wrapper for kcp
 *
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class Ukcp {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(Ukcp.class);

    private Kcp kcp;

    private boolean fastFlush = true;

    private boolean mergeSegmentBuf = true;

    private int tsUpdate;

    private volatile boolean active;

    /**
     * Creates a new instance.
     *
     * @param conv   conv of kcp
     * @param output output for kcp
     */
    public Ukcp(int conv, KcpOutput output) {
        Kcp kcp = new Kcp(conv, output);
        this.kcp = kcp;
        this.active = true;
    }

    /**
     * Receives ByteBufs.
     *
     * @param buf
     * @throws IOException
     */
    public void receive(ByteBuf buf) throws IOException {
        int ret = kcp.recv(buf);
        switch (ret) {
            case -3:
                throw new IOException("Received Data exceeds maxCapacity of buf");
            default:
                break;
        }
    }

    /**
     * Receives ByteBufs.
     *
     * @param bufList received ByteBuf will be add to the list
     */
    public void receive(List<ByteBuf> bufList) {
        kcp.recv(bufList);
    }

    public void input(ByteBuf data) throws IOException {
        int ret = kcp.input(data);
        switch (ret) {
            case -1:
                throw new IOException("No enough bytes of head");
            case -2:
                throw new IOException("No enough bytes of data");
            case -3:
                throw new IOException("Mismatch cmd");
            case -4:
                throw new IOException("Conv inconsistency");
            default:
                break;
        }
    }

    /**
     * Sends a Bytebuf.
     *
     * @param buf
     * @throws IOException
     */
    public void send(ByteBuf buf) throws IOException {
        int ret = kcp.send(buf);
        switch (ret) {
            case -2:
                throw new IOException("Too many fragments");
            default:
                break;
        }
    }

    /**
     * The size of the first msg of the kcp.
     *
     * @return The size of the first msg of the kcp, or -1 if none of msg
     */
    public int peekSize() {
        return kcp.peekSize();
    }

    /**
     * Returns {@code true} if there are bytes can be received.
     *
     * @return
     */
    public boolean canRecv() {
        return kcp.canRecv();
    }

    /**
     * Returns {@code true} if the kcp can send more bytes.
     *
     * @param curCanSend last state of canSend
     * @return {@code true} if the kcp can send more bytes
     */
    public boolean canSend(boolean curCanSend) {
        int max = kcp.getSndWnd() * 2;
        int waitSnd = kcp.waitSnd();
        if (curCanSend) {
            return waitSnd < max;
        } else {
            int threshold = Math.max(1, max / 2);
            return waitSnd < threshold;
        }
    }

    /**
     * Udpates the kcp.
     *
     * @param current current time in milliseconds
     * @return the next time to update
     */
    public int update(int current) {
        kcp.update(current);
        int nextTsUp = check(current);
        setTsUpdate(nextTsUp);

        return nextTsUp;
    }

    /**
     * Determines when should you invoke udpate.
     *
     * @param current current time in milliseconds
     * @return
     * @see Kcp#check(int)
     */
    public int check(int current) {
        return kcp.check(current);
    }

    /**
     * Returns {@code true} if the kcp need to flush.
     *
     * @return {@code true} if the kcp need to flush
     */
    public boolean checkFlush() {
        return kcp.checkFlush();
    }

    /**
     * Sets params of nodelay.
     *
     * @param nodelay  {@code true} if nodelay mode is enabled
     * @param interval protocol internal work interval, in milliseconds
     * @param resend   fast retransmission mode, 0 represents off by default, 2 can be set (2 ACK spans will result
     *                 in direct retransmission)
     * @param nc       {@code true} if turn off flow control
     */
    public void nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        kcp.nodelay(nodelay, interval, resend, nc);
    }

    /**
     * Returns conv of kcp.
     *
     * @return conv of kcp
     */
    public int getConv() {
        return kcp.getConv();
    }

    /**
     * Set the conv of kcp.
     *
     * @param conv the conv of kcp
     */
    public void setConv(int conv) {
        kcp.setConv(conv);
    }

    /**
     * Returns {@code true} if and only if nodelay is enabled.
     *
     * @return {@code true} if and only if nodelay is enabled
     */
    public boolean isNodelay() {
        return kcp.isNodelay();
    }

    /**
     * Sets whether enable nodelay.
     *
     * @param nodelay {@code true} if enable nodelay
     * @return this object
     */
    public Ukcp setNodelay(boolean nodelay) {
        kcp.setNodelay(nodelay);
        return this;
    }

    /**
     * Returns update interval.
     *
     * @return update interval
     */
    public int getInterval() {
        return kcp.getInterval();
    }

    /**
     * Sets update interval
     *
     * @param interval update interval
     * @return this object
     */
    public Ukcp setInterval(int interval) {
        kcp.setInterval(interval);
        return this;
    }

    /**
     * Returns the fastresend of kcp.
     *
     * @return the fastresend of kcp
     */
    public int getFastResend() {
        return kcp.getFastresend();
    }

    /**
     * Sets the fastresend of kcp.
     *
     * @param fastResend
     * @return this object
     */
    public Ukcp setFastResend(int fastResend) {
        kcp.setFastresend(fastResend);
        return this;
    }

    public int getFastLimit() {
        return kcp.getFastlimit();
    }

    public Ukcp setFastLimit(int fastLimit) {
        kcp.setFastlimit(fastLimit);
        return this;
    }

    public boolean isNocwnd() {
        return kcp.isNocwnd();
    }

    public Ukcp setNocwnd(boolean nocwnd) {
        kcp.setNocwnd(nocwnd);
        return this;
    }

    public int getMinRto() {
        return kcp.getRxMinrto();
    }

    public Ukcp setMinRto(int minRto) {
        kcp.setRxMinrto(minRto);
        return this;
    }

    public int getMtu() {
        return kcp.getMtu();
    }

    public Ukcp setMtu(int mtu) {
        kcp.setMtu(mtu);
        return this;
    }

    public boolean isStream() {
        return kcp.isStream();
    }

    public Ukcp setStream(boolean stream) {
        kcp.setStream(stream);
        return this;
    }

    public int getDeadLink() {
        return kcp.getDeadLink();
    }

    public Ukcp setDeadLink(int deadLink) {
        kcp.setDeadLink(deadLink);
        return this;
    }

    /**
     * Sets the {@link ByteBufAllocator} which is used for the kcp to allocate buffers.
     *
     * @param allocator the allocator is used for the kcp to allocate buffers
     * @return this object
     */
    public Ukcp setByteBufAllocator(ByteBufAllocator allocator) {
        kcp.setByteBufAllocator(allocator);
        return this;
    }

    public boolean isAutoSetConv() {
        return kcp.isAutoSetConv();
    }

    public Ukcp setAutoSetConv(boolean autoSetConv) {
        kcp.setAutoSetConv(autoSetConv);
        return this;
    }

    public Ukcp wndSize(int sndWnd, int rcvWnd) {
        kcp.wndsize(sndWnd, rcvWnd);
        return this;
    }

    public int waitSnd() {
        return kcp.waitSnd();
    }

    public int getRcvWnd() {
        return kcp.getRcvWnd();
    }

    public Ukcp setRcvWnd(int rcvWnd) {
        kcp.setRcvWnd(rcvWnd);
        return this;
    }

    public int getSndWnd() {
        return kcp.getSndWnd();
    }

    public Ukcp setSndWnd(int sndWnd) {
        kcp.setSndWnd(sndWnd);
        return this;
    }

    public boolean isFastFlush() {
        return fastFlush;
    }

    public Ukcp setFastFlush(boolean fastFlush) {
        this.fastFlush = fastFlush;
        return this;
    }

    public boolean isMergeSegmentBuf() {
        return mergeSegmentBuf;
    }

    public Ukcp setMergeSegmentBuf(boolean mergeSegmentBuf) {
        this.mergeSegmentBuf = mergeSegmentBuf;
        return this;
    }

    public int getTsUpdate() {
        return tsUpdate;
    }

    public Ukcp setTsUpdate(int tsUpdate) {
        this.tsUpdate = tsUpdate;
        return this;
    }

    public int getState() {
        return kcp.getState();
    }

    public boolean isActive() {
        return active;
    }

    void setClosed(boolean closeKcp) {
        this.active = false;
        if (closeKcp) {
            setKcpClosed();
        }
    }

    void setKcpClosed() {
        kcp.setState(-1);
        kcp.release();
    }

    @SuppressWarnings("unchecked")
    public <T extends Channel> T channel() {
        return (T) kcp.getUser();
    }

    public Ukcp channel(Channel channel) {
        kcp.setUser(channel);
        return this;
    }

    @Override
    public String toString() {
        return "Ukcp(" +
                "getConv=" + kcp.getConv() +
                ", state=" + kcp.getState() +
                ", active=" + active +
                ')';
    }

}
