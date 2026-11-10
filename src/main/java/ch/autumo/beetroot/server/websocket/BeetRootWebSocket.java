/**
 *
 * Copyright (c) 2026 autumo Ltd. Switzerland, Michael Gasche
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ch.autumo.beetroot.server.websocket;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.OpCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;

/**
 * Standalone WebSocket connection on top of NanoHTTPD.
 *
 * <p>
 * The class only implements the transport: frames in, frames out and the
 * socket life-cycle. The JSON protocol and the actions are handled by
 * {@link WebSocketSessionHandler}, which is shared with the servlet container
 * transport.
 * </p>
 */
public class BeetRootWebSocket extends WebSocket implements WebSocketConnection {

	private static final Logger LOG = LoggerFactory.getLogger(BeetRootWebSocket.class);

	private final BeetRootHTTPSession httpSession;
	private final WebSocketSessionHandler handler;

	/**
	 * Create a WebSocket for an authenticated HTTP session.
	 *
	 * @param httpSession HTTP session of the handshake
	 * @param userSession authenticated user session
	 * @param manager WebSocket manager
	 */
	public BeetRootWebSocket(BeetRootHTTPSession httpSession, Session userSession, WebSocketManager manager) {
		super(httpSession);
		this.httpSession = httpSession;
		this.handler = new WebSocketSessionHandler(this, userSession, manager);
	}

	/**
	 * Get the user session of the connection.
	 *
	 * @return user session
	 */
	public Session getUserSession() {
		return handler.getUserSession();
	}

	@Override
	protected void onOpen() {
		applySocketTimeout();
		handler.opened();
	}

	@Override
	protected void onMessage(WebSocketFrame message) {
		if (message.getBinaryPayload().length > managerConfigMaxMessageSize()) {
			close(CLOSE_TOO_BIG, "Message too big");
			return;
		}

		if (message.getOpCode() != OpCode.Text) {
			handler.sendError(null, "Only text messages are supported.");
			return;
		}

		handler.received(message.getTextPayload());
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
		handler.closed();
		closeTransport();
		LOG.trace("WebSocket closed: code={}, reason={}, remote={}", code, reason, initiatedByRemote);
	}

	@Override
	protected void onPong(WebSocketFrame pong) {
		// Nothing to do; receiving a pong proves that the peer is still alive.
	}

	@Override
	protected void onException(IOException exception) {
		if (LOG.isTraceEnabled()) {
			LOG.trace("WebSocket exception: {}", exception.getMessage());
		}
	}

	@Override
	public void ping() throws IOException {
		ping(new byte[0]);
	}

	@Override
	public void close(int statusCode, String reason) {
		try {
			CloseCode code = CloseCode.find(statusCode);
			if (code == null) {
				code = CloseCode.GoingAway;
			}
			close(code, truncate(reason), false);
		} catch (IOException e) {
			LOG.trace("Couldn't send WebSocket close frame.", e);
		}
	}

	private int managerConfigMaxMessageSize() {
		// The handler has the manager; the size check for binary frames happens
		// before the payload is converted to text.
		return handler.getMaxMessageSize();
	}

	private static String truncate(String reason) {
		if (reason == null) {
			return "";
		}
		return reason.length() > 120 ? reason.substring(0, 120) : reason;
	}

	private void applySocketTimeout() {
		final Socket socket = httpSession.getSocket();
		if (socket == null) {
			return;
		}
		final int seconds = handler.getTimeout();
		try {
			socket.setSoTimeout(seconds <= 0 ? 0 : seconds * 1000);
		} catch (SocketException e) {
			LOG.warn("Couldn't set WebSocket socket timeout.", e);
		}
	}

	/**
	 * Close the underlying socket. This ends the blocked WebSocket read loop
	 * and the HTTP client handler.
	 */
	@Override
	public void closeTransport() {
		final Socket socket = httpSession.getSocket();
		if (socket != null) {
			try {
				if (!socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				LOG.trace("Couldn't close WebSocket transport.", e);
			}
		}
	}

}
