package com.chess4friends.netty;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Creates threads with following a defined naming strategy.
 * 
 * @author gciuloaica
 * 
 */
public class NettyServerThreadFactory implements ThreadFactory {
	private static final AtomicInteger counter = new AtomicInteger();

	private final String name;

	/**
	 * @param name
	 *            - thread name
	 */
	public NettyServerThreadFactory(final String name) {
		this.name = name;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(final Runnable runnable) {
		return new Thread(runnable, name + '-' + counter.getAndIncrement());
	}

}
