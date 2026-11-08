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
package ch.autumo.beetroot.routing;

/**
 * A WebSocket action route. It maps an action path to a
 * {@link ch.autumo.beetroot.server.websocket.WebSocketAction}.
 *
 * <p>
 * Action routes are configured in {@code cfg/routing.xml}:
 * </p>
 *
 * <pre>
 * &lt;Package name="ch.autumo.beetroot.handler.system"&gt;
 *     &lt;WebSocket path="/system/log/tail" handler="LogTailWebSocketAction" name="log" /&gt;
 * &lt;/Package&gt;
 * </pre>
 */
public class WebSocketActionRoute {

	private final String path;
	private final Class<?> handler;
	private final String name;

	/**
	 * Create a WebSocket action route.
	 *
	 * @param path action path, e.g. '/system/log/tail'
	 * @param handler action handler class
	 * @param name action name
	 */
	public WebSocketActionRoute(String path, Class<?> handler, String name) {
		this.path = path;
		this.handler = handler;
		this.name = name;
	}

	/**
	 * Get the action path.
	 *
	 * @return action path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the action handler class.
	 *
	 * @return handler class
	 */
	public Class<?> getHandler() {
		return handler;
	}

	/**
	 * Get the action name.
	 *
	 * @return action name
	 */
	public String getName() {
		return name;
	}

}
