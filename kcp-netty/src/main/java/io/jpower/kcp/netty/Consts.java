package io.jpower.kcp.netty;

import io.netty.util.internal.SystemPropertyUtil;
import io.netty.util.internal.logging.InternalLogger;
import io.netty.util.internal.logging.InternalLoggerFactory;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class Consts {

    public static final InternalLogger sheduleUpdateLog = InternalLoggerFactory.getInstance("io.jpower.kcp.netty" +
            ".sheduleUpdate");

    public static final int FIXED_RECV_BYTEBUF_ALLOCATE_SIZE = SystemPropertyUtil.getInt("io.jpower.kcp" +
            ".udpRecvAllocateSize", 2048);

    public static final int CLOSE_WAIT_TIME = 5 * 1000;

}
