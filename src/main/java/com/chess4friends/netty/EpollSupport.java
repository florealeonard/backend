package com.chess4friends.netty;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import jersey.repackaged.com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.Locale;
import java.util.concurrent.ThreadFactory;

/**
 * Use epoll if platform is linux.
 */
@SuppressWarnings("uncheked")
public class EpollSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(EpollSupport.class);

    private static final boolean USE_EPOLL;

    private static final Constructor<? extends EventLoopGroup> EPOLL_EVENT_LOOP_GROUP_CONSTRUCTOR;

    private static final Class<? extends ServerSocketChannel> EPOLL_SERVER_CHANNEL_CLASS;

    private static final Class[] EVENT_GROUP_ARGUMENTS = {int.class, ThreadFactory.class};

    private EpollSupport(){}


    static {
        boolean useEpoll = false;
        try {
            Class<?> epoll = Class.forName("io.netty.channel.epoll.Epoll");
            if (!System.getProperty("os.name", "").toLowerCase(Locale.US).equals("linux")) {
                LOGGER.warn("Found Netty's native epoll transport, but not running on linux-based operating " +
                        "system. Using NIO instead.");
            } else if (!(Boolean) epoll.getMethod("isAvailable").invoke(null)) {
                LOGGER.warn("Found Netty's native epoll transport in the classpath, but epoll is not available. "
                        + "Using NIO instead.", (Throwable) epoll.getMethod("unavailabilityCause").invoke(null));
            } else {
                LOGGER.info("Found Netty's native epoll transport in the classpath, using it");
                useEpoll = true;
            }
        } catch (ClassNotFoundException e) {
            LOGGER.info("Did not find Netty's native epoll transport in the classpath, defaulting to NIO.");
        } catch (Exception e) {
            LOGGER.warn("Unexpected error trying to find Netty's native epoll transport in the classpath, defaulting to NIO.", e);
        }

        USE_EPOLL = useEpoll;
        Constructor<? extends EventLoopGroup> constructor = null;
        Class<? extends ServerSocketChannel> channelClass = null;
        if (USE_EPOLL) {
            try {
                channelClass = (Class<? extends ServerSocketChannel>)Class.forName("io.netty.channel.epoll" +
                        ".EpollServerSocketChannel");
                Class<?> epoolEventLoupGroupClass = Class.forName("io.netty.channel.epoll.EpollEventLoopGroup");
                constructor = (Constructor<? extends EventLoopGroup>)epoolEventLoupGroupClass.getDeclaredConstructor(EVENT_GROUP_ARGUMENTS);
            } catch (Exception e) {
                throw new AssertionError("Netty's native epoll is in use but cannot locate Epoll classes, this should not happen: " + e);
            }
        }
        EPOLL_EVENT_LOOP_GROUP_CONSTRUCTOR = constructor;
        EPOLL_SERVER_CHANNEL_CLASS = channelClass;
    }

    /**
     * @return true if native epoll transport is available in the classpath, false otherwise.
     */
    public static boolean isEpollAvailable() {
        return USE_EPOLL;
    }

    /**
     * Return a new instance of {@link EventLoopGroup}.
     * <p>
     * Returns an instance of {@link io.netty.channel.epoll.EpollEventLoopGroup} if {@link #isEpollAvailable() epoll is available},
     * or an instance of {@link NioEventLoopGroup} otherwise.
     *
     * @param nThreads - number of threads to be created.
     * @param factory the {@link ThreadFactory} instance to use to create the new instance of {@link EventLoopGroup}
     * @return a new instance of {@link EventLoopGroup}
     */
    public static EventLoopGroup newEventLoopGroupInstance(int nThreads, ThreadFactory factory) {
        if (isEpollAvailable()) {
            try {
                return EPOLL_EVENT_LOOP_GROUP_CONSTRUCTOR.newInstance(nThreads, factory);
            } catch (Exception e) {
                throw Throwables.propagate(e); // should not happen
            }
        } else {
            return new NioEventLoopGroup(nThreads, factory);
        }
    }

    /**
     * Return the SocketChannel class to use.
     * <p>
     * Returns an instance of {@link io.netty.channel.epoll.EpollServerSocketChannel} if {@link #isEpollAvailable()
     * epoll is available},
     * or an instance of {@link NioSocketChannel} otherwise.
     *
     * @return the SocketChannel class to use.
     */
    public static Class<? extends ServerSocketChannel> channelClass() {
        if (isEpollAvailable()) {
            return EPOLL_SERVER_CHANNEL_CLASS;
        } else {
            return NioServerSocketChannel.class;
        }
    }

}
