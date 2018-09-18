package io.jpower.kcp.netty;

import java.net.InetSocketAddress;

import io.netty.buffer.ByteBuf;
import io.netty.util.AbstractReferenceCounted;
import io.netty.util.Recycler;
import io.netty.util.ReferenceCounted;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class UkcpPacket extends AbstractReferenceCounted {

    private static final Recycler<UkcpPacket> RECYCLER = new Recycler<UkcpPacket>() {
        @Override
        protected UkcpPacket newObject(Handle<UkcpPacket> handle) {
            return new UkcpPacket(handle);
        }
    };

    private final Recycler.Handle<UkcpPacket> recyclerHandle;

    private ByteBuf content;

    private InetSocketAddress remoteAddress;

    public static UkcpPacket newInstance(ByteBuf content, InetSocketAddress remoteAddress) {
        UkcpPacket packet = RECYCLER.get();
        packet.setRefCnt(1);
        packet.content = content;
        packet.remoteAddress = remoteAddress;
        return packet;
    }

    private UkcpPacket(Recycler.Handle<UkcpPacket> recyclerHandle) {
        this.recyclerHandle = recyclerHandle;
    }

    public void recycle() {
        recyclerHandle.recycle(this);
    }

    public InetSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public ByteBuf content() {
        return content;
    }

    @Override
    protected void deallocate() {
        if (content != null) {
            content.release();
            content = null;
            remoteAddress = null;

            recycle();
        }
    }

    @Override
    public ReferenceCounted touch(Object hint) {
        return this;
    }

}
