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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Session;

/**
 * Transport-independent WebSocket protocol.
 *
 * <p>
 * The handler implements the JSON protocol of the project:
 * </p>
 *
 * <pre>
 * {"action":"/system/log/tail"}                     start an action
 * {"action":"/system/log/tail","command":"stop"}    stop it
 * </pre>
 *
 * <p>
 * The server pushes:
 * </p>
 *
 * <pre>
 * {"action":"/system/log/tail","type":"event","data":...}
 * {"action":"/system/log/tail","type":"snapshot","data":[...]}
 * </pre>
 *
 * <p>
 * One handler exists per connection and is used by the standalone transport
 * ({@link BeetRootWebSocket}) as well as by the servlet container transport
 * ({@link ServletWebSocketEndpoint}).
 * </p>
 */
final class WebSocketSessionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketSessionHandler.class);

	private final WebSocketConnection connection;
	private final Session userSession;
	private final WebSocketManager manager;
	private final Map<String, ActiveAction> actions = new ConcurrentHashMap<String, ActiveAction>();

	/**
	 * Create a handler for an opened connection.
	 *
	 * @param connection transport connection
	 * @param userSession authenticated user session
	 * @param manager WebSocket manager
	 */
	WebSocketSessionHandler(WebSocketConnection connection, Session userSession, WebSocketManager manager) {
		this.connection = connection;
		this.userSession = userSession;
		this.manager = manager;
	}

	/**
	 * Called when the transport is open: register the connection and enforce
	 * the connection limit.
	 */
	void opened() {
		if (!manager.register(this)) {
			LOG.warn("Rejecting WebSocket connection: connection limit of {} reached.",
					manager.getConfig().getMaxConnections());
			connection.close(WebSocketConnection.CLOSE_POLICY_VIOLATION,
					"Too many WebSocket connections");
		}
	}

	/**
	 * Called for every incoming text message.
	 *
	 * @param payload message payload
	 */
	void received(String payload) {
		if (payload == null) {
			return;
		}
		if (payload.length() > manager.getConfig().getMaxMessageSize()) {
			connection.close(WebSocketConnection.CLOSE_TOO_BIG, "Message too big");
			return;
		}

		try {
			handleMessage(payload);
		} catch (JSONException e) {
			sendError(null, "Invalid JSON message.");
		} catch (IOException e) {
			LOG.trace("Couldn't read WebSocket message.", e);
		} catch (Exception e) {
			LOG.warn("WebSocket message handling failed.", e);
			sendError(null, "Action failed: " + e.getMessage());
		}
	}

	/**
	 * Called when the transport is closed: stop all actions and unregister.
	 */
	void closed() {
		for (ActiveAction active : actions.values()) {
			try {
				active.action.onStop(active.context);
			} catch (Exception e) {
				LOG.warn("WebSocket action failed while closing.", e);
			}
		}
		actions.clear();
		manager.unregister(this);
	}

	/**
	 * Is the connection still open?
	 *
	 * @return true if open
	 */
	boolean isOpen() {
		return connection.isOpen();
	}

	/**
	 * Get the user session of the connection.
	 *
	 * @return user session
	 */
	Session getUserSession() {
		return userSession;
	}

	/**
	 * Send a ping through the transport.
	 *
	 * @throws IOException IO exception
	 */
	void ping() throws IOException {
		connection.ping();
	}

	/**
	 * Close the connection.
	 *
	 * @param reason reason
	 */
	void close(String reason) {
		connection.close(WebSocketConnection.CLOSE_GOING_AWAY, reason);
		connection.closeTransport();
	}

	/**
	 * Get the configured maximum message size.
	 *
	 * @return maximum message size in bytes
	 */
	int getMaxMessageSize() {
		return manager.getConfig().getMaxMessageSize();
	}

	/**
	 * Get the configured socket timeout.
	 *
	 * @return timeout in seconds; 0 means no timeout
	 */
	int getTimeout() {
		return manager.getConfig().getTimeout();
	}

	/**
	 * Send an error message to the client.
	 *
	 * @param actionPath action path, may be null
	 * @param error error message
	 */
	void sendError(String actionPath, String error) {
		final JSONObject message = new JSONObject();
		if (actionPath != null) {
			message.put("action", actionPath);
		}
		message.put("type", "error");
		message.put("message", error == null ? "Unknown error." : error);
		try {
			connection.send(message.toString());
		} catch (IOException e) {
			LOG.trace("Couldn't send WebSocket error message.", e);
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

		final ActiveAction active = actions.get(actionPath);
		if (active == null) {
			startAction(actionPath, json);
			return;
		}

		try {
			active.action.onMessage(active.context, json);
		} catch (Exception e) {
			LOG.warn("WebSocket action '{}' failed.", actionPath, e);
			sendError(actionPath, "Action failed: " + e.getMessage());
		}
	}

	private void startAction(String actionPath, JSONObject request) throws IOException {
		final WebSocketActionRegistry.ResolvedAction resolved = manager.getRegistry().resolve(actionPath);
		if (resolved == null) {
			sendError(actionPath, "Unknown action.");
			return;
		}

		final WebSocketAction action;
		try {
			action = (WebSocketAction) resolved.getRoute().getHandler()
					.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			LOG.error("Couldn't create WebSocket action '{}'.",
					resolved.getRoute().getHandler().getName(), e);
			sendError(actionPath, "Couldn't create action.");
			return;
		}

		if (!action.hasAccess(userSession)) {
			sendError(actionPath, "Access denied.");
			return;
		}

		final WebSocketContext context = new WebSocketContext(connection, userSession, actionPath,
				resolved.getPathParams());
		final ActiveAction active = new ActiveAction(action, context);
		actions.put(actionPath, active);

		try {
			action.onStart(context, request);
		} catch (Exception e) {
			actions.remove(actionPath);
			LOG.warn("WebSocket action '{}' couldn't be started.", actionPath, e);
			sendError(actionPath, "Action start failed: " + e.getMessage());
		}
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

	private static final class ActiveAction {

		private final WebSocketAction action;
		private final WebSocketContext context;

		private ActiveAction(WebSocketAction action, WebSocketContext context) {
			this.action = action;
			this.context = context;
		}
	}

}
