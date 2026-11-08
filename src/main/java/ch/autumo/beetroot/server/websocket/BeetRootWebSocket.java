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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.nanohttpd.protocols.websockets.CloseCode;
import org.nanohttpd.protocols.websockets.OpCode;
import org.nanohttpd.protocols.websockets.WebSocket;
import org.nanohttpd.protocols.websockets.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.BeetRootHTTPSession;
import ch.autumo.beetroot.Session;

/**
 * A beetRoot WebSocket connection.
 *
 * <p>
 * The connection uses a small JSON protocol. A client starts an action by
 * sending:
 * </p>
 *
 * <pre>
 * {"action":"/system/log/tail"}
 * </pre>
 *
 * <p>
 * Further messages addressed to the same action are forwarded to the action.
 * The server pushes data as:
 * </p>
 *
 * <pre>
 * {"action":"/system/log/tail","type":"event","data":...}
 * </pre>
 *
 * <p>
 * An action can be stopped with {@code {"action":"...","command":"stop"}}.
 * </p>
 */
public class BeetRootWebSocket extends WebSocket {

	private static final Logger LOG = LoggerFactory.getLogger(BeetRootWebSocket.class);

	private final BeetRootHTTPSession httpSession;
	private final Session userSession;
	private final WebSocketManager manager;
	private final Map<String, ActiveAction> actions = new ConcurrentHashMap<String, ActiveAction>();

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
		this.userSession = userSession;
		this.manager = manager;
	}

	/**
	 * Get the user session of the connection.
	 *
	 * @return user session
	 */
	public Session getUserSession() {
		return userSession;
	}

	@Override
	protected void onOpen() {
		applySocketTimeout();
		if (!manager.register(this)) {
			try {
				close(CloseCode.PolicyViolation, "Too many WebSocket connections", false);
			} catch (IOException e) {
				LOG.debug("Couldn't close rejected WebSocket connection.", e);
			}
		}
	}

	@Override
	protected void onMessage(WebSocketFrame message) {
		if (message.getBinaryPayload().length > manager.getConfig().getMaxMessageSize()) {
			try {
				close(CloseCode.MessageTooBig, "Message too big", false);
			} catch (IOException e) {
				LOG.debug("Couldn't close WebSocket after oversized message.", e);
			}
			return;
		}

		if (message.getOpCode() != OpCode.Text) {
			sendError(null, "Only text messages are supported.");
			return;
		}

		try {
			handleMessage(message.getTextPayload());
		} catch (JSONException e) {
			sendError(null, "Invalid JSON message.");
		} catch (IOException e) {
			LOG.debug("Couldn't read WebSocket message.", e);
		} catch (Exception e) {
			LOG.warn("WebSocket message handling failed.", e);
			sendError(null, "Action failed: " + e.getMessage());
		}
	}

	private void handleMessage(String payload) throws IOException {
		final JSONObject json = new JSONObject(payload);
		final String actionPath = json.optString("action", null);
		if (actionPath == null || actionPath.trim().length() == 0) {
			sendError(null, "Missing action.");
			return;
		}

		if ("stop".equalsIgnoreCase(json.optString("command", null))) {
			stopAction(actionPath);
			return;
		}

		ActiveAction active = actions.get(actionPath);
		if (active == null) {
			active = startAction(actionPath, json);
			if (active == null) {
				return;
			}
			return;
		}

		try {
			active.action.onMessage(active.context, json);
		} catch (Exception e) {
			LOG.warn("WebSocket action '{}' failed.", actionPath, e);
			sendError(actionPath, "Action failed: " + e.getMessage());
		}
	}

	private ActiveAction startAction(String actionPath, JSONObject request) throws IOException {
		final WebSocketActionRegistry.ResolvedAction resolved = manager.getRegistry().resolve(actionPath);
		if (resolved == null) {
			sendError(actionPath, "Unknown action.");
			return null;
		}

		final WebSocketAction action;
		try {
			action = (WebSocketAction) resolved.getRoute().getHandler().getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			LOG.error("Couldn't create WebSocket action '{}'.", resolved.getRoute().getHandler().getName(), e);
			sendError(actionPath, "Couldn't create action.");
			return null;
		}

		if (!action.hasAccess(userSession)) {
			sendError(actionPath, "Access denied.");
			return null;
		}

		final WebSocketContext context = new WebSocketContext(this, userSession, actionPath,
				resolved.getPathParams());
		final ActiveAction active = new ActiveAction(action, context);
		actions.put(actionPath, active);

		try {
			action.onStart(context, request);
		} catch (Exception e) {
			actions.remove(actionPath);
			LOG.warn("WebSocket action '{}' couldn't be started.", actionPath, e);
			sendError(actionPath, "Action start failed: " + e.getMessage());
			return null;
		}
		return active;
	}

	private void stopAction(String actionPath) {
		final ActiveAction active = actions.remove(actionPath);
		if (active != null) {
			try {
				active.action.onStop(active.context);
			} catch (Exception e) {
				LOG.warn("WebSocket action '{}' failed while stopping.", actionPath, e);
			}
		}
	}

	@Override
	protected void onClose(CloseCode code, String reason, boolean initiatedByRemote) {
		for (ActiveAction active : actions.values()) {
			try {
				active.action.onStop(active.context);
			} catch (Exception e) {
				LOG.warn("WebSocket action failed while closing.", e);
			}
		}
		actions.clear();
		manager.unregister(this);
		closeTransport();
		LOG.debug("WebSocket closed: code={}, reason={}, remote={}", code, reason, initiatedByRemote);
	}

	@Override
	protected void onPong(WebSocketFrame pong) {
		// Nothing to do; receiving a pong proves that the peer is still alive.
	}

	@Override
	protected void onException(IOException exception) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("WebSocket exception: {}", exception.getMessage());
		}
	}

	private void applySocketTimeout() {
		final Socket socket = httpSession.getSocket();
		if (socket == null) {
			return;
		}
		final int seconds = manager.getConfig().getTimeout();
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
	void closeTransport() {
		final Socket socket = httpSession.getSocket();
		if (socket != null) {
			try {
				if (!socket.isClosed()) {
					socket.close();
				}
			} catch (IOException e) {
				LOG.debug("Couldn't close WebSocket transport.", e);
			}
		}
	}

	void sendJson(JSONObject message) throws IOException {
		send(message.toString());
	}

	void sendActionEvent(String actionPath, Object data) throws IOException {
		final JSONObject message = new JSONObject();
		message.put("action", actionPath);
		message.put("type", "event");
		message.put("data", data == null ? JSONObject.NULL : data);
		sendJson(message);
	}

	void sendActionSnapshot(String actionPath, JSONArray data) throws IOException {
		final JSONObject message = new JSONObject();
		message.put("action", actionPath);
		message.put("type", "snapshot");
		message.put("data", data == null ? new JSONArray() : data);
		sendJson(message);
	}

	void sendError(String actionPath, String error) {
		final JSONObject message = new JSONObject();
		if (actionPath != null) {
			message.put("action", actionPath);
		}
		message.put("type", "error");
		message.put("message", error == null ? "Unknown error." : error);
		try {
			sendJson(message);
		} catch (IOException e) {
			LOG.debug("Couldn't send WebSocket error message.", e);
		}
	}

	private static final class ActiveAction {

		private final WebSocketAction action;
		private final WebSocketContext context;

		private ActiveAction(WebSocketAction action, WebSocketContext context) {
			this.action = action;
			this.context = context;
		}
	}

}
