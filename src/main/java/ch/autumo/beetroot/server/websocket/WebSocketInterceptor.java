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

import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

import org.nanohttpd.protocols.http.IHTTPSession;
import org.nanohttpd.protocols.http.NanoHTTPD;
import org.nanohttpd.protocols.http.response.Response;
import org.nanohttpd.protocols.http.response.Status;
import org.nanohttpd.protocols.websockets.NanoWSD;
import org.nanohttpd.util.IHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.server.HTTPServer;

/**
 * HTTP interceptor that upgrades requests on the configured WebSocket endpoint.
 *
 * <p>
 * The interceptor is registered on the HTTP server and leaves every other
 * request untouched. The normal beetRoot router is not involved.
 * </p>
 */
public class WebSocketInterceptor implements IHandler<IHTTPSession, Response> {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketInterceptor.class);

	private final WebSocketManager manager;

	/**
	 * Create the interceptor.
	 *
	 * @param manager WebSocket manager
	 */
	public WebSocketInterceptor(WebSocketManager manager) {
		this.manager = manager;
	}

	@Override
	public Response handle(IHTTPSession session) {
		final WebSocketConfig config = manager.getConfig();
		if (!config.isEnabled()) {
			return null;
		}

		final String requestPath = HTTPServer.normalizeUri(session.getUri());
		final String endpointPath = HTTPServer.normalizeUri(config.getPath());
		if (!endpointPath.equals(requestPath)) {
			return null;
		}

		final Map<String, String> headers = session.getHeaders();

		if (!isWebSocketRequest(headers)) {
			return Response.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
					"This endpoint only accepts WebSocket connections.");
		}

		if (!NanoWSD.HEADER_WEBSOCKET_VERSION_VALUE
				.equalsIgnoreCase(headers.get(NanoWSD.HEADER_WEBSOCKET_VERSION))) {
			return Response.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
					"Invalid WebSocket version.");
		}

		final String key = headers.get(NanoWSD.HEADER_WEBSOCKET_KEY);
		if (key == null || key.trim().length() == 0) {
			return Response.newFixedLengthResponse(Status.BAD_REQUEST, NanoHTTPD.MIME_PLAINTEXT,
					"Missing WebSocket key.");
		}

		if (!isOriginAllowed(headers, config)) {
			return Response.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_PLAINTEXT,
					"WebSocket origin not allowed.");
		}

		if (!(session instanceof BeetRootHTTPSession)) {
			return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
					"Unsupported HTTP session.");
		}

		final BeetRootHTTPSession beetRootSession = (BeetRootHTTPSession) session;
		final Session userSession = SessionManager.getInstance().find(beetRootSession);
		if (userSession == null || userSession.getUser() == null) {
			return Response.newFixedLengthResponse(Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT,
					"Authentication required.");
		}

		// The handshake is a user request, so apply the same session timeout
		// and refresh semantics as a normal HTTP request.
		if (userSession.isOlderThanSessionTimeout()) {
			return Response.newFixedLengthResponse(Status.UNAUTHORIZED, NanoHTTPD.MIME_PLAINTEXT,
					"Session expired.");
		}
		userSession.refresh();

		if (!manager.canAccept()) {
			return Response.newFixedLengthResponse(Status.SERVICE_UNAVAILABLE, NanoHTTPD.MIME_PLAINTEXT,
					"Too many WebSocket connections.");
		}

		final BeetRootWebSocket webSocket = new BeetRootWebSocket(beetRootSession, userSession, manager);
		final Response handshake = webSocket.getHandshakeResponse();
		try {
			handshake.addHeader(NanoWSD.HEADER_WEBSOCKET_ACCEPT, NanoWSD.makeAcceptKey(key));
		} catch (NoSuchAlgorithmException e) {
			LOG.error("SHA-1 is not available for the WebSocket handshake.", e);
			return Response.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_PLAINTEXT,
					"WebSocket handshake failed.");
		}

		final String protocol = headers.get(NanoWSD.HEADER_WEBSOCKET_PROTOCOL);
		if (protocol != null && protocol.trim().length() > 0) {
			handshake.addHeader(NanoWSD.HEADER_WEBSOCKET_PROTOCOL, protocol.split(",")[0].trim());
		}

		LOG.debug("Upgrading request '{}' to a WebSocket connection for user '{}'.",
				requestPath, userSession.getUserName());
		return handshake;
	}

	private static boolean isWebSocketRequest(Map<String, String> headers) {
		final String upgrade = headers.get(NanoWSD.HEADER_UPGRADE);
		final String connection = headers.get(NanoWSD.HEADER_CONNECTION);
		return NanoWSD.HEADER_UPGRADE_VALUE.equalsIgnoreCase(upgrade)
				&& connection != null
				&& connection.toLowerCase().contains(NanoWSD.HEADER_CONNECTION_VALUE.toLowerCase());
	}

	private static boolean isOriginAllowed(Map<String, String> headers, WebSocketConfig config) {
		if (!config.isOriginCheck()) {
			return true;
		}
		if (config.getAllowedOrigins().contains("*")) {
			return true;
		}

		final String origin = headers.get("origin");
		if (origin == null || origin.trim().length() == 0) {
			return false;
		}

		for (String allowed : config.getAllowedOrigins()) {
			if (allowed.equalsIgnoreCase(origin.trim())) {
				return true;
			}
		}

		final String requestHost = hostName(headers.get("host"));
		if (requestHost == null) {
			return false;
		}
		try {
			final String originHost = hostName(new URI(origin.trim()).getHost());
			return requestHost.equalsIgnoreCase(originHost);
		} catch (Exception e) {
			return false;
		}
	}

	private static String hostName(String value) {
		if (value == null || value.trim().length() == 0) {
			return null;
		}
		final String host = value.trim();
		if (host.startsWith("[")) {
			final int end = host.indexOf(']');
			return end > 0 ? host.substring(0, end + 1) : host;
		}
		final int colon = host.indexOf(':');
		return colon > 0 ? host.substring(0, colon) : host;
	}

}
