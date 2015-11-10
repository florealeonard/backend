package com.chess4friends.netty;

import com.chess4friends.jersey.JerseyHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.cors.CorsConfig;
import io.netty.handler.codec.http.cors.CorsHandler;
import io.netty.util.concurrent.EventExecutorGroup;

/**
 * Build the channel pipeline
 */
public class NettyChannelInitializer extends io.netty.channel.ChannelInitializer<SocketChannel> {

    private final EventExecutorGroup eventExecutor;
    private final JerseyHandler jerseyHandler;
    private final int maxMessageSize;
    private final CorsConfig corsConfig;
    private static final String XIP = "X-IP";
    private static final String X_GEOIP_CITY_COUNTRY_CODE = "X-GEOIP_CITY_COUNTRY_CODE";
    private static final String X_TOKEN_ONLY = "X-TOKEN-ONLY";

    /**
     * Channel initializer
     *
     * @param jerseyHandler         - jersey handler
     * @param pipelineExecutor      - pipeline executor
     * @param maxMessageSize        - maximum message size
     */
    public NettyChannelInitializer(final JerseyHandler jerseyHandler,
                                   final EventExecutorGroup pipelineExecutor,
                                   int maxMessageSize) {
        eventExecutor = pipelineExecutor;
        this.jerseyHandler = jerseyHandler;

        this.maxMessageSize = maxMessageSize;
        this.corsConfig = CorsConfig.withAnyOrigin().allowedRequestHeaders(HttpHeaders.Names.CONTENT_TYPE, XIP,
                X_GEOIP_CITY_COUNTRY_CODE, X_TOKEN_ONLY)
                .build();

    }

    /*
     * (non-Javadoc)
     *
     * @see
     * io.netty.channel.NettyChannelInitializer#initChannel(io.netty.channel.Channel)
     */
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch
                .pipeline()
                .addLast("decoder", new HttpRequestDecoder())
                .addLast("aggregator", new HttpObjectAggregator(maxMessageSize))
                .addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast(new CorsHandler(corsConfig));
        pipeline.addLast(eventExecutor, jerseyHandler).addLast(
                "exceptionHandler", new ExceptionHandler());

    }
}
