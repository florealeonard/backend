/**
 * 
 */
package com.chess4friends.netty;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

/**
 * Prepare a response for the situation when errors are identified.
 * 
 * @author gciuloaica
 * 
 */
public class ErrorResponseHanlder {

	/**
	 * Sends an error to the client.
	 * 
	 * @param ctx
	 *            The handler context.
	 * @param status
	 *            The HTTP status.
	 */
	public void sendError(ChannelHandlerContext ctx, HttpResponseStatus status,
				   boolean isAuthenticationFailed) {
		HttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				status);
		writeResponse(ctx, response);
	}

	private void writeResponse(final ChannelHandlerContext ctx,
			final HttpResponse response) {
		if (ctx.channel().isWritable()) {
			// Close the connection as soon as the error message is sent.
			ctx.writeAndFlush(response)
					.addListener(ChannelFutureListener.CLOSE);
		}
	}

}
