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

/**
 * Transport-neutral view of an established WebSocket connection.
 *
 * <p>
 * There are two implementations: the standalone server uses the NanoHTTPD
 * WebSocket ({@link BeetRootWebSocket}), a servlet container uses
 * {@link ServletWebSocketEndpoint}. The JSON protocol, the action registry and
 * the manager only talk to this interface, so both transports behave the same.
 * </p>
 */
public interface WebSocketConnection {

	/** Close code: endpoint is going away (normal shutdown). */
	int CLOSE_GOING_AWAY = 1001;

	/** Close code: policy violation (for example too many connections). */
	int CLOSE_POLICY_VIOLATION = 1008;

	/** Close code: message too big. */
	int CLOSE_TOO_BIG = 1009;

	/**
	 * Is the connection still open?
	 *
	 * @return true if open
	 */
	boolean isOpen();

	/**
	 * Send a text message.
	 *
	 * @param payload message payload
	 * @throws IOException IO exception
	 */
	void send(String payload) throws IOException;

	/**
	 * Send a ping frame.
	 *
	 * @throws IOException IO exception
	 */
	void ping() throws IOException;

	/**
	 * Close the connection.
	 *
	 * @param statusCode WebSocket close code, e.g. {@link #CLOSE_GOING_AWAY}
	 * @param reason human readable reason, may be truncated
	 */
	void close(int statusCode, String reason);

	/**
	 * Terminate the underlying transport after a close. Some transports need
	 * this to end a blocked read loop; others simply close the session.
	 */
	void closeTransport();

}
