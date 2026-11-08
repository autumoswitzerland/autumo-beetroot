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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ch.autumo.beetroot.routing.WebSocketActionRoute;
import ch.autumo.beetroot.server.HTTPServer;

/**
 * Registry of WebSocket action routes.
 *
 * <p>
 * The action paths are registered like normal routes in {@code cfg/routing.xml}
 * and are matched with the same path pattern syntax. WebSocket actions are not
 * language specific; actions that need the user language can read it from the
 * session.
 * </p>
 */
public class WebSocketActionRegistry {

	private final List<Entry> entries = new ArrayList<Entry>();

	/**
	 * Create the registry.
	 *
	 * @param routes configured WebSocket action routes
	 */
	public WebSocketActionRegistry(List<WebSocketActionRoute> routes) {
		if (routes != null) {
			for (WebSocketActionRoute route : routes) {
				entries.add(new Entry(route, new HTTPServer.UriResource(route.getPath(), route.getHandler())));
			}
		}
	}

	/**
	 * Resolve an action path.
	 *
	 * @param actionPath action path from a client message
	 * @return the matching route and its path parameters, or {@code null}
	 */
	public ResolvedAction resolve(String actionPath) {
		if (actionPath == null || actionPath.trim().length() == 0) {
			return null;
		}
		final String normalized = HTTPServer.normalizeUri(actionPath.trim());
		for (Entry entry : entries) {
			final Map<String, String> params = entry.uriResource.match(normalized);
			if (params != null) {
				return new ResolvedAction(entry.route, params);
			}
		}
		return null;
	}

	/**
	 * Get all registered action routes.
	 *
	 * @return action routes
	 */
	public List<WebSocketActionRoute> getRoutes() {
		final List<WebSocketActionRoute> routes = new ArrayList<WebSocketActionRoute>();
		for (Entry entry : entries) {
			routes.add(entry.route);
		}
		return Collections.unmodifiableList(routes);
	}

	private static final class Entry {

		private final WebSocketActionRoute route;
		private final HTTPServer.UriResource uriResource;

		private Entry(WebSocketActionRoute route, HTTPServer.UriResource uriResource) {
			this.route = route;
			this.uriResource = uriResource;
		}
	}

	/**
	 * A resolved action route.
	 */
	public static final class ResolvedAction {

		private final WebSocketActionRoute route;
		private final Map<String, String> pathParams;

		private ResolvedAction(WebSocketActionRoute route, Map<String, String> pathParams) {
			this.route = route;
			this.pathParams = pathParams;
		}

		/**
		 * Get the route.
		 *
		 * @return route
		 */
		public WebSocketActionRoute getRoute() {
			return route;
		}

		/**
		 * Get the extracted path parameters.
		 *
		 * @return path parameters
		 */
		public Map<String, String> getPathParams() {
			return pathParams;
		}
	}

}
