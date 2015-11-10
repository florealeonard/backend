package com.chess4friends.jersey;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.internal.JerseyRequestTimeoutHandler;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Write the response to the channel.
 */
class JerseyResponseWriter implements ContainerResponseWriter {

    private final Channel channel;
    private FullHttpResponse response;
    private final JerseyRequestTimeoutHandler requestTimeoutHandler;

    private static final Logger LOG = LoggerFactory.getLogger(JerseyResponseWriter.class);

    /**
	 * Create a new jersey response writer handler.
	 * 
	 * @param channel
	 *            - active channel
	 */
	public JerseyResponseWriter(final Channel channel, final ScheduledExecutorService backgroundScheduler) {
        this.channel = channel;
        this.requestTimeoutHandler = new JerseyRequestTimeoutHandler(this, backgroundScheduler);
	}


    @Override
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse responseContext) {
        ByteBuf buffer = Unpooled.buffer();
        response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.valueOf(responseContext.getStatus()), buffer);

        for (Map.Entry<String, List<Object>> e : responseContext.getHeaders().entrySet()) {
            List<String> values = new ArrayList<String>();
            for (Object v : e.getValue()) {
                values.add(v.toString());
            }
            response.headers().add(e.getKey(), values);
        }
        return new ByteBufOutputStream(buffer);
    }

    @Override
    public boolean suspend(long timeOut, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
        return requestTimeoutHandler.suspend(timeOut, timeUnit, timeoutHandler);
    }

    @Override
    public void setSuspendTimeout(long timeOut, TimeUnit timeUnit) {
        requestTimeoutHandler.setSuspendTimeout(timeOut, timeUnit);
    }

    @Override
    public void commit() {
        final FullHttpResponse current = response;
        channel.writeAndFlush(current).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void failure(Throwable error) {
        LOG.error("Error encounter: ", error);
        channel.close();
    }

    @Override
    public boolean enableResponseBuffering() {
        return false;
    }
}
