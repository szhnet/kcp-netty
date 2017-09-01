package io.jpower.kcp.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class Ukcp {

    private static final InternalLogger log = InternalLoggerFactory.getInstance(Ukcp.class);

    private Kcp kcp;

    private boolean fastFlush = true;

    private long tsUpdate = -1;

    private volatile boolean active;

    public Ukcp(int conv, KcpOutput output) {
        Kcp kcp = new Kcp(conv, output);
        this.kcp = kcp;
        this.active = true;
    }

    public void receive(List<ByteBuf> bufList) {
        kcp.recv(bufList);
    }

    public void input(ByteBuf data) throws IOException {
        int ret = kcp.input6(data);
        switch (ret) {
            case -1:
                throw new IOException("No enough bytes");
            case -2:
                throw new IOException("No enough bytes");
            case -3:
                throw new IOException("Mismatch cmd");
            case -4:
                throw new IOException("Conv inconsistency");
            default:
                break;
        }
    }

    public void send(ByteBuf buf) throws IOException {
        int ret = kcp.send6(buf);
        switch (ret) {
            case -2:
                throw new IOException("Too many fragments");
            default:
                break;
        }
    }

    public int peekSize() {
        return kcp.peekSize();
    }

    public boolean canRecv() {
        return kcp.canRecv();
    }

    public long update(long current) {
        kcp.update(current);
        long nextTsUp = check(current);
        setTsUpdate(nextTsUp);

        return nextTsUp;
    }

    public long check(long current) {
        return kcp.check(current);
    }

    public boolean checkFlush() {
        return kcp.checkFlush();
    }

    public void nodelay(boolean nodelay, int interval, int resend, boolean nc) {
        kcp.nodelay(nodelay, interval, resend, nc);
    }

    public int getConv() {
        return kcp.getConv();
    }

    public void setConv(int conv) {
        kcp.setConv(conv);
    }

    public boolean isNodelay() {
        return kcp.isNodelay();
    }

    public Ukcp setNodelay(boolean nodelay) {
        kcp.setNodelay(nodelay);
        return this;
    }

    public int getInterval() {
        return kcp.getInterval();
    }

    public Ukcp setInterval(int interval) {
        kcp.setInterval(interval);
        return this;
    }

    public int getFastResend() {
        return kcp.getFastresend();
    }

    public Ukcp setFastResend(int fastResend) {
        kcp.setFastresend(fastResend);
        return this;
    }

    public boolean isNocwnd() {
        return kcp.isNocwnd();
    }

    public Ukcp setNocwnd(boolean nocwnd) {
        kcp.setNocwnd(nocwnd);
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

    public long getTsUpdate() {
        return tsUpdate;
    }

    public Ukcp setTsUpdate(long tsUpdate) {
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
