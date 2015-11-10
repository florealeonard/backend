package com.chess4friends.jersey;

import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import reactor.core.Reactor;
import reactor.net.NetChannel;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Created by florealeonard on 11/9/15.
 */
public class JerseyHelper {


    public static void handleRequest(ApplicationHandler jerseyHandler, FullHttpRequest fullHttpRequest, NetChannel<FullHttpRequest, FullHttpResponse> channel){
        try {
            final String baseURI = getBaseUri(fullHttpRequest);
            final URI baseUriFromRequest = new URI(baseURI);
            final URI requestUri = new URI(baseURI.substring(0, baseURI.length() - 1) + fullHttpRequest.getUri());

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


            ContainerRequest containerRequest = new ContainerRequest(baseUriFromRequest, requestUri, fullHttpRequest.getMethod().name(), securityContext, new MapPropertiesDelegate());
            containerRequest.setEntityStream(new ByteBufInputStream(fullHttpRequest.retain().content()));
            List<Map.Entry<String, String>> headers = fullHttpRequest.headers().entries();
            for (Map.Entry<String, String> entry : headers) {
                containerRequest.getHeaders().putSingle(entry.getKey(), entry.getValue());
            }

            containerRequest.setWriter(new JerseyReactorResponseWriter(channel, Executors.newScheduledThreadPool(8)));
            jerseyHandler.handle(containerRequest);
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private static String getBaseUri(final HttpRequest request) {
        String baseUriStringValue = null;

        if (baseUriStringValue == null) {
            baseUriStringValue = "http://"
                    + request.headers().get(HttpHeaders.Names.HOST) + "/";
        }

        return baseUriStringValue;
    }
}
