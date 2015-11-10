package com.chess4friends.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.chess4friends.netty.EpollSupport.channelClass;
import static  com.chess4friends.netty.EpollSupport.newEventLoopGroupInstance;

/**
 * Netty based server.
 * 
 * @author gciuloaica
 * 
 */
public class NettyServer {

	public static final String PROPERTY_BASE_URI = "com.woow.jersey.container.netty.baseUri";
	public static final String RESTRICTED_ACCESS_RESOURCES = "com.woow.authentication.required.resources";
	public static final String MAX_WORKER_THREADS_PROPERTY = "com.woow.netty.max.worker.threads";
	public static final String JERSEY_HANDLER_CORE_POOL_SIZE = "com.woow.netty.jersey.handler.core.pool.size";
	public static final String USE_REQUEST_FILTER = "com.woow.netty.request.filter";
	public static final String MAX_CONNECTIONS = "com.woow.netty.connection.limit";
	public static final String MAX_MESSAGE_SIZE = "com.woow.netty.message.size";

	private static final Logger LOG = LoggerFactory
			.getLogger(NettyServer.class);

	private final SocketAddress localSocket;

	private final ServerBootstrap server = new ServerBootstrap();
	private final EventLoopGroup bossGroup;
	private final EventLoopGroup workerGroup;

	private ChannelFuture runningInstance;

	/**
	 * Create new server instance.
	 *
	 * @param localSocket
	 *            - local socket address
	 */
	public NettyServer(final NettyChannelInitializer channelInitializer,
				final SocketAddress localSocket, final int maxWorkerThreads) {

		ThreadFactory bossFactory = new NettyServerThreadFactory("boss");
		ThreadFactory workersFactory = new NettyServerThreadFactory("worker");

		bossGroup = newEventLoopGroupInstance(2, bossFactory);
		workerGroup = newEventLoopGroupInstance(maxWorkerThreads, workersFactory);


        this.localSocket = localSocket;

		
		initialize(channelInitializer);

	}

	/**
	 * Starts the server.
	 * 
	 */
	public void startServer() {
		InetSocketAddress address = (InetSocketAddress) localSocket;
		LOG.info("Starting server on {}:{}", address.getHostString(),
				address.getPort());

		// ReactorBootstrap the server.
		this.runningInstance = server.bind();
		this.runningInstance.addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
	}

	/**
	 * Stop the server.
	 * 
	 */
	public void stopServer() {
		LOG.info("Stopping server....");

		try {
			if (this.runningInstance != null) {
				this.runningInstance.channel().closeFuture()
						.await(5, TimeUnit.SECONDS);
			}
			// Shut down all event loops to terminate all threads.
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();

			// Wait until all threads are terminated.
			bossGroup.terminationFuture().sync();
			workerGroup.terminationFuture().sync();
		} catch (InterruptedException e) {
			LOG.error("Got interrupted before releasing resources.", e);
		}

	}

	private void initialize(
			io.netty.channel.ChannelInitializer channelInitializator) {
		server.group(bossGroup, workerGroup)
				.channel(channelClass())
				.localAddress(localSocket)
				.option(ChannelOption.SO_BACKLOG, 128)
				.option(ChannelOption.SO_REUSEADDR, true)
				.option(ChannelOption.SO_RCVBUF, 128 * 1024)
				.option(ChannelOption.SO_SNDBUF, 128 * 1024)
				.childOption(ChannelOption.SO_KEEPALIVE, true)
				.childOption(ChannelOption.TCP_NODELAY, true)
				.childOption(ChannelOption.ALLOCATOR,
						PooledByteBufAllocator.DEFAULT)
				.childHandler(channelInitializator);

	}

}
