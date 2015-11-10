package com.chess4friends.jersey;

import io.netty.buffer.ByteBufInputStream;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.slf4j.MDC;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;


/**
 * Handle a client request.
 *
 * @author gciuloaica
 */
@Sharable
public class JerseyHandler extends SimpleChannelInboundHandler<FullHttpRequest> implements Container {

    private static final ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(8);

    private final ApplicationHandler application;

    private final ResourceConfig resourceConfig;

    private final String baseUri;

    /**
     * Create a new handler instance.
     *
     * @param application    - the jersey application context.
     */
    public JerseyHandler(final ApplicationHandler application) {
        this.application = application;
        this.resourceConfig = application.getConfiguration();
        this.baseUri = (String) resourceConfig
                .getProperty("com.woow.jersey.container.netty.baseUri");
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx,
                                FullHttpRequest request) throws Exception {
        final String baseURI = getBaseUri(request);
        final URI baseUriFromRequest = new URI(baseURI);
        final URI requestUri = new URI(baseURI.substring(0, baseURI.length() - 1)
                + request.getUri());

        SecurityContext securityContext = new SecurityContext() {
            @Override
            public Principal getUserPrincipal() {
                return null;
            }

            @Override
            public boolean isUserInRole(String s) {
                return false;
            }

            @Override
            public boolean isSecure() {
                return false;
            }

            @Override
            public String getAuthenticationScheme() {
                return null;
            }
        };


        PropertiesDelegate properties = new MapPropertiesDelegate();


        ContainerRequest containerRequest = new ContainerRequest(baseUriFromRequest, requestUri, request.getMethod().name(), securityContext, properties);
        containerRequest.setEntityStream(new ByteBufInputStream(request.retain().content()));
        List<Map.Entry<String, String>> headers = request.headers().entries();
        for (Map.Entry<String, String> entry: headers){
            containerRequest.getHeaders().putSingle(entry.getKey(), entry.getValue());
        }

        containerRequest.setWriter(new JerseyResponseWriter(ctx.channel(), executor ));

        application.handle(containerRequest);

    }

    private String getBaseUri(final HttpRequest request) {
        String baseUriStringValue = this.baseUri;
        if (baseUriStringValue == null) {
            baseUriStringValue = "http://"
                    + request.headers().get(HttpHeaders.Names.HOST) + "/";
        }
        return baseUriStringValue;

    }

    /**
     * @return resource configuration
     */
    public ResourceConfig getResourceConfig() {
        return resourceConfig;
    }



    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }


    @Override
    public ResourceConfig getConfiguration() {
        return resourceConfig;
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return application;
    }

    @Override
    public void reload() {
        // not supported
    }

    @Override
    public void reload(ResourceConfig configuration) {
        // not supported yet
    }
}
