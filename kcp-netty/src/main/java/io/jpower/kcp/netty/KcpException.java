package io.jpower.kcp.netty;

/**
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class KcpException extends RuntimeException {

    private static final long serialVersionUID = 7055403679773787210L;

    public KcpException() {
        super();
    }

    public KcpException(String message, Throwable cause) {
        super(message, cause);
    }

    public KcpException(String message) {
        super(message);
    }

    public KcpException(Throwable cause) {
        super(cause);
    }

}
