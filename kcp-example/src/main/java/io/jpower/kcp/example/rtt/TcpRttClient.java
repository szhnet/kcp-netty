package io.jpower.kcp.example.rtt;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * Measures RTT(Round-trip time) for TCP.
 * <p>
 * Sends a message to server and receive a response from server to measure RTT.
 *
 * @author <a href="mailto:szhnet@gmail.com">szh</a>
 */
public class TcpRttClient {

    static final String HOST = System.getProperty("host", "127.0.0.1");
    static final int PORT = Integer.parseInt(System.getProperty("port", "8009"));
    static final int SIZE = Integer.parseInt(System.getProperty("size", "200"));
    static final int COUNT = Integer.parseInt(System.getProperty("count", "300"));
    static final int RTT_INTERVAL = Integer.parseInt(System.getProperty("rttInterval", "20"));

    public static void main(String[] args) throws Exception {
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new TcpRttDecoder())
                                    .addLast(new TcpRttClientHandler(COUNT));
                        }
                    }).option(ChannelOption.TCP_NODELAY, true);

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
    }

}
