package io.netty.bootstrap;

import java.util.Map;

import io.jpower.kcp.netty.UkcpServerChannel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelOption;
import io.netty.util.AttributeKey;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class UkcpServerBootstrapConfig extends AbstractBootstrapConfig<UkcpServerBootstrap, UkcpServerChannel> {

    UkcpServerBootstrapConfig(UkcpServerBootstrap bootstrap) {
        super(bootstrap);
    }

    /**
     * Returns the configured {@link io.netty.channel.ChannelHandler} be used for the child channels or {@code null}
     * if non is configured yet.
     */
    public ChannelHandler childHandler() {
        return bootstrap.childHandler();
    }

    /**
     * Returns a copy of the configured options which will be used for the child channels.
     */
    public Map<ChannelOption<?>, Object> childOptions() {
        return bootstrap.childOptions();
    }

    /**
     * Returns a copy of the configured attributes which will be used for the child channels.
     */
    public Map<AttributeKey<?>, Object> childAttrs() {
        return bootstrap.childAttrs();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(super.toString());
        buf.setLength(buf.length() - 1);
        buf.append(", ");
        Map<ChannelOption<?>, Object> childOptions = childOptions();
        if (!childOptions.isEmpty()) {
            buf.append("childOptions: ");
            buf.append(childOptions);
            buf.append(", ");
        }
        Map<AttributeKey<?>, Object> childAttrs = childAttrs();
        if (!childAttrs.isEmpty()) {
            buf.append("childAttrs: ");
            buf.append(childAttrs);
            buf.append(", ");
        }
        ChannelHandler childHandler = childHandler();
        if (childHandler != null) {
            buf.append("childHandler: ");
            buf.append(childHandler);
            buf.append(", ");
        }
        if (buf.charAt(buf.length() - 1) == '(') {
            buf.append(')');
        } else {
            buf.setCharAt(buf.length() - 2, ')');
            buf.setLength(buf.length() - 1);
        }

        return buf.toString();
    }
}

