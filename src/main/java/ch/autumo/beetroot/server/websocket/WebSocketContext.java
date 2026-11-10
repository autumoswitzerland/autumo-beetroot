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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import ch.autumo.beetroot.Session;

/**
 * Context of a running WebSocket action. It is used by an action to push data
 * back to the client.
 */
public final class WebSocketContext {

	private final WebSocketConnection connection;
	private final Session session;
	private final String actionPath;
	private final Map<String, String> pathParams;

	WebSocketContext(WebSocketConnection connection, Session session, String actionPath,
			Map<String, String> pathParams) {
		this.connection = connection;
		this.session = session;
		this.actionPath = actionPath;
		this.pathParams = pathParams == null
				? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(pathParams));
	}

	/**
	 * Get the user session of the connection.
	 *
	 * @return user session
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * Get the action path this context belongs to.
	 *
	 * @return action path
	 */
	public String getActionPath() {
		return actionPath;
	}

	/**
	 * Get the path parameters extracted from the action path.
	 *
	 * @return path parameters
	 */
	public Map<String, String> getPathParams() {
		return pathParams;
	}

	/**
	 * Is the underlying WebSocket still open?
	 *
	 * @return true if open
	 */
	public boolean isOpen() {
		return connection.isOpen();
	}

	/**
	 * Push a single event to the client.
	 *
	 * @param data event payload
	 * @throws IOException IO exception
	 */
	public void sendEvent(Object data) throws IOException {
		final JSONObject message = new JSONObject();
		message.put("action", actionPath);
		message.put("type", "event");
		message.put("data", data == null ? JSONObject.NULL : data);
		sendJson(message);
	}

	/**
	 * Push a snapshot to the client.
	 *
	 * @param data snapshot payload
	 * @throws IOException IO exception
	 */
	public void sendSnapshot(JSONArray data) throws IOException {
		final JSONObject message = new JSONObject();
		message.put("action", actionPath);
		message.put("type", "snapshot");
		message.put("data", data == null ? new JSONArray() : data);
		sendJson(message);
	}

	/**
	 * Send a raw JSON message.
	 *
	 * @param message message
	 * @throws IOException IO exception
	 */
	public void sendMessage(JSONObject message) throws IOException {
		sendJson(message);
	}

	/**
	 * Send an error for this action.
	 *
	 * @param message error message
	 */
	public void sendError(String message) {
		final JSONObject json = new JSONObject();
		json.put("action", actionPath);
		json.put("type", "error");
		json.put("message", message == null ? "Unknown error." : message);
		try {
			sendJson(json);
		} catch (IOException e) {
			// Nothing left to do; the connection is gone.
		}
	}

	private void sendJson(JSONObject message) throws IOException {
		if (!connection.isOpen()) {
			return;
		}
		connection.send(message.toString());
	}

}
