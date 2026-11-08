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

import org.json.JSONObject;

import ch.autumo.beetroot.Session;

/**
 * A WebSocket action.
 *
 * <p>
 * Actions are registered in {@code cfg/routing.xml} and are addressed by their
 * action path. An action instance is created per connection as soon as the
 * client starts it with a message such as:
 * </p>
 *
 * <pre>
 * {"action":"/system/log/tail"}
 * </pre>
 *
 * <p>
 * The server may push data to the client at any time through the
 * {@link WebSocketContext}. Every pushed message carries the action path, so a
 * client can multiplex several actions over a single WebSocket connection.
 * </p>
 */
public interface WebSocketAction {

	/**
	 * Check whether the current user may use this action. The default is to
	 * allow every authenticated user.
	 *
	 * @param userSession the user session of the WebSocket connection
	 * @return true if access is allowed
	 */
	default boolean hasAccess(Session userSession) {
		return true;
	}

	/**
	 * Called once when the client starts the action.
	 *
	 * @param context action context
	 * @param request the start message
	 * @throws Exception exception
	 */
	default void onStart(WebSocketContext context, JSONObject request) throws Exception {
	}

	/**
	 * Called for every further message addressed to a running action.
	 *
	 * @param context action context
	 * @param message the message
	 * @throws Exception exception
	 */
	default void onMessage(WebSocketContext context, JSONObject message) throws Exception {
	}

	/**
	 * Called when the action is stopped or the connection is closed.
	 *
	 * @param context action context
	 */
	default void onStop(WebSocketContext context) {
	}

}
