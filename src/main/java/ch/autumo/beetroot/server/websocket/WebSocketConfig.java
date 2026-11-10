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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import ch.autumo.beetroot.BeetRootConfigurationManager;
import ch.autumo.beetroot.Constants;

/**
 * WebSocket configuration, read from the beetRoot configuration.
 *
 * <p>
 * The WebSocket facility is a general framework feature. It is active if
 * {@code websocket_enabled=yes} and a WebSocket transport is available: the
 * standalone server uses the NanoHTTPD transport, a servlet container uses the
 * Jakarta WebSocket API.
 * </p>
 */
public final class WebSocketConfig {

	/** Default WebSocket endpoint path. */
	public static final String DEFAULT_PATH = "/ws";
	/** Default socket timeout in seconds; 0 means no timeout. */
	public static final int DEFAULT_TIMEOUT = 0;
	/** Default server ping interval in seconds; 0 disables pings. */
	public static final int DEFAULT_PING_INTERVAL = 30;
	/** Default maximum number of concurrent connections. */
	public static final int DEFAULT_MAX_CONNECTIONS = 200;
	/** Default maximum message size in bytes. */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 1048576;

	private final boolean enabled;
	private final String path;
	private final int timeout;
	private final int pingInterval;
	private final int maxConnections;
	private final int maxMessageSize;
	private final boolean originCheck;
	private final Set<String> allowedOrigins;
	private final int socketPort;

	private WebSocketConfig(boolean enabled, String path, int timeout, int pingInterval,
			int maxConnections, int maxMessageSize, boolean originCheck, Set<String> allowedOrigins, int socketPort) {
		this.enabled = enabled;
		this.path = path;
		this.timeout = timeout;
		this.pingInterval = pingInterval;
		this.maxConnections = maxConnections;
		this.maxMessageSize = maxMessageSize;
		this.originCheck = originCheck;
		this.allowedOrigins = Collections.unmodifiableSet(allowedOrigins);
		this.socketPort = socketPort;
	}

	/**
	 * Load the WebSocket configuration.
	 *
	 * @return WebSocket configuration
	 */
	public static WebSocketConfig load() {
		final BeetRootConfigurationManager cfg = BeetRootConfigurationManager.getInstance();

		final boolean enabled = cfg.getYesOrNo(Constants.KEY_WEBSOCKET_ENABLED, Constants.NO);

		String path = cfg.getString(Constants.KEY_WEBSOCKET_PATH, DEFAULT_PATH);
		if (path == null || path.trim().length() == 0) {
			path = DEFAULT_PATH;
		} else {
			path = path.trim();
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		if (path.length() > 1 && path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}

		int socketPort = cfg.getInt(Constants.KEY_WEBSOCKET_PORT, -1);
		if (socketPort < 0) {
			socketPort = cfg.getInt(Constants.KEY_WS_PORT, 8778);
		}

		int timeout = cfg.getInt(Constants.KEY_WEBSOCKET_TIMEOUT, DEFAULT_TIMEOUT);
		if (timeout < 0) {
			timeout = DEFAULT_TIMEOUT;
		}

		int pingInterval = cfg.getInt(Constants.KEY_WEBSOCKET_PING_INTERVAL, DEFAULT_PING_INTERVAL);
		if (pingInterval < 0) {
			pingInterval = DEFAULT_PING_INTERVAL;
		}

		int maxConnections = cfg.getInt(Constants.KEY_WEBSOCKET_MAX_CONNECTIONS, DEFAULT_MAX_CONNECTIONS);
		if (maxConnections < 1) {
			maxConnections = DEFAULT_MAX_CONNECTIONS;
		}

		int maxMessageSize = cfg.getInt(Constants.KEY_WEBSOCKET_MAX_MESSAGE_SIZE, DEFAULT_MAX_MESSAGE_SIZE);
		if (maxMessageSize < 1) {
			maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
		}

		final boolean originCheck = cfg.getYesOrNo(Constants.KEY_WEBSOCKET_ORIGIN_CHECK, Constants.YES);

		final Set<String> allowedOrigins = new LinkedHashSet<>();
		final String origins = cfg.getString(Constants.KEY_WEBSOCKET_ALLOWED_ORIGINS, "");
		if (origins != null) {
			for (String origin : origins.split(",")) {
				final String value = origin.trim();
				if (value.length() > 0) {
					allowedOrigins.add(value);
				}
			}
		}

		return new WebSocketConfig(enabled, path, timeout, pingInterval, maxConnections,
				maxMessageSize, originCheck, allowedOrigins, socketPort);
	}

	/**
	 * Is WebSocket support enabled?
	 *
	 * @return true if enabled
	 */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Is this configuration used inside a servlet container?
	 *
	 * @return true if the WebSocket transport is the container's Jakarta
	 *         WebSocket implementation
	 */
	public boolean isServletTransport() {
		return BeetRootConfigurationManager.getInstance().runsWithinServletContext();
	}

	/**
	 * Get the WebSocket endpoint path.
	 *
	 * @return endpoint path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Get the socket timeout of upgraded WebSocket connections in seconds.
	 *
	 * @return timeout in seconds; 0 means no timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * Get the server ping interval in seconds.
	 *
	 * @return ping interval in seconds; 0 disables server pings
	 */
	public int getPingInterval() {
		return pingInterval;
	}

	/**
	 * Get the maximum number of concurrent WebSocket connections.
	 *
	 * @return maximum number of connections
	 */
	public int getMaxConnections() {
		return maxConnections;
	}

	/**
	 * Get the maximum message size in bytes.
	 *
	 * @return maximum message size in bytes
	 */
	public int getMaxMessageSize() {
		return maxMessageSize;
	}

	/**
	 * Should the Origin header be checked during the handshake?
	 *
	 * @return true if the Origin header is checked
	 */
	public boolean isOriginCheck() {
		return originCheck;
	}

	/**
	 * Get the optional Origin whitelist.
	 *
	 * @return configured origins; an empty set means same-host only
	 */
	public Set<String> getAllowedOrigins() {
		return allowedOrigins;
	}

	/**
	 * Get the WebSocket server port.
	 *
	 * @return WebSocket port
	 */
	public int getSocketPort() {
		return socketPort;
	}

}
