/**
 * 
 */
package com.chess4friends.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle exceptions that has been propagate in upstream to this handler.
 * 
 * @author gabrielciuloaica
 * 
 */
public class ExceptionHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final Logger LOG = LoggerFactory
			.getLogger(ExceptionHandler.class);

	private final ErrorResponseHanlder errorResponseHandler = new ErrorResponseHanlder();


	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		String s = cause.toString();

		if (isUnrecoverableException(s)) {
			LOG.debug("Unrecovarable error: {}", cause.getMessage());
			ctx.channel().close();

		} else {
			HttpResponseStatus status = HttpResponseStatus.INTERNAL_SERVER_ERROR;
			boolean isAuthenticationFailed = false;
			if (s.startsWith("com.woow.jersey.container.netty.AuthenticationRequiredException")) {
				status = HttpResponseStatus.UNAUTHORIZED;
				isAuthenticationFailed = true;
			}
			if (s.startsWith("com.woow.jersey.container.netty.AuthenticationFailedException")) {
				status = HttpResponseStatus.FORBIDDEN;
				isAuthenticationFailed = true;
			}

			if (s.startsWith("org.jboss.netty.handler.codec.frame.TooLongFrameException")) {
				status = HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE;
			}
			if (cause.toString().startsWith("RequestFilterException")) {
				status = HttpResponseStatus.SERVICE_UNAVAILABLE;
			}

			if (status.equals(HttpResponseStatus.INTERNAL_SERVER_ERROR)) {
				LOG.error("Unexpected exception from downstream.", cause);
			}

			sendError(ctx, status, isAuthenticationFailed);
		}
	}

	private boolean isUnrecoverableException(String s) {
		return s.startsWith("java.nio.channels.ClosedChannelException")
				|| s.startsWith("java.io.IOException");

	}

	private void sendError(ChannelHandlerContext ctx,
						   HttpResponseStatus status, boolean isAuthenticaitonFailed) {
		errorResponseHandler.sendError(ctx, status, isAuthenticaitonFailed);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg)
			throws Exception {
		LOG.info("Exception: {}", msg);
		
	}

}
