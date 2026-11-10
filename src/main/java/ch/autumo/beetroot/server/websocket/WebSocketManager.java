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
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Session;
import ch.autumo.beetroot.SessionManager;
import ch.autumo.beetroot.routing.WebSocketActionRoute;

/**
 * Manages the active WebSocket connections.
 *
 * <p>
 * The manager owns the action registry, enforces the connection limit, sends
 * the configured heartbeats and closes connections when their user session
 * ends. It is transport neutral: the standalone server uses the NanoHTTPD
 * transport ({@link BeetRootWebSocket}) and a servlet container uses
 * {@link ServletWebSocketEndpoint}.
 * </p>
 */
public class WebSocketManager {

	private static final Logger LOG = LoggerFactory.getLogger(WebSocketManager.class);

	private final WebSocketConfig config;
	private final WebSocketActionRegistry registry;
	private final Set<WebSocketSessionHandler> connections = ConcurrentHashMap.newKeySet();

	private final SessionManager.SessionDestroyListener sessionDestroyListener =
			new SessionManager.SessionDestroyListener() {
				@Override
				public void sessionDestroyed(String token, Session session) {
					closeForSession(session);
				}
			};

	private ScheduledExecutorService scheduler;
	private volatile boolean started;

	/**
	 * Create the manager.
	 *
	 * @param config WebSocket configuration
	 * @param routes configured WebSocket action routes
	 */
	public WebSocketManager(WebSocketConfig config, List<WebSocketActionRoute> routes) {
		this.config = config;
		this.registry = new WebSocketActionRegistry(routes);
	}

	/**
	 * Get the WebSocket configuration.
	 *
	 * @return configuration
	 */
	public WebSocketConfig getConfig() {
		return config;
	}

	/**
	 * Get the action registry.
	 *
	 * @return registry
	 */
	public WebSocketActionRegistry getRegistry() {
		return registry;
	}

	/**
	 * Can another connection be accepted?
	 *
	 * @return true if the connection limit has not been reached
	 */
	public boolean canAccept() {
		return connections.size() < config.getMaxConnections();
	}

	/**
	 * Register an opened connection.
	 *
	 * @param handler connection handler
	 * @return true if the connection was accepted
	 */
	public boolean register(WebSocketSessionHandler handler) {
		if (connections.size() >= config.getMaxConnections()) {
			return false;
		}
		connections.add(handler);
		return true;
	}

	/**
	 * Remove a closed connection.
	 *
	 * @param handler connection handler
	 */
	public void unregister(WebSocketSessionHandler handler) {
		connections.remove(handler);
	}

	/**
	 * Start the manager, the heartbeat and the session listener.
	 */
	public void start() {
		if (!config.isEnabled() || started) {
			return;
		}
		started = true;
		SessionManager.addDestroyListener(sessionDestroyListener);

		if (config.getPingInterval() > 0) {
			scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					final Thread thread = new Thread(runnable, "beetroot-websocket-ping");
					thread.setDaemon(true);
					return thread;
				}
			});
			scheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					pingAll();
				}
			}, config.getPingInterval(), config.getPingInterval(), TimeUnit.SECONDS);
		}
	}

	private void pingAll() {
		for (WebSocketSessionHandler handler : connections) {
			if (!handler.isOpen()) {
				connections.remove(handler);
				continue;
			}
			// A WebSocket must not outlive its user session.
			if (handler.getUserSession() == null || handler.getUserSession().isOlderThanSessionTimeout()) {
				handler.close("Session timed out");
				continue;
			}
			try {
				handler.ping();
			} catch (IOException e) {
				LOG.trace("WebSocket ping failed; closing connection.", e);
				handler.close("Ping failed");
			}
		}
	}

	/**
	 * Close all WebSocket connections of a user session.
	 *
	 * @param session user session
	 */
	public void closeForSession(Session session) {
		if (session == null) {
			return;
		}
		for (WebSocketSessionHandler handler : connections) {
			if (handler.getUserSession() == session) {
				handler.close("Session ended");
			}
		}
	}

	/**
	 * Stop the manager: stop the heartbeat, remove the session listener and
	 * close all connections.
	 */
	public void stop() {
		started = false;
		SessionManager.removeDestroyListener(sessionDestroyListener);

		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}

		for (WebSocketSessionHandler handler : connections) {
			handler.close("Server shutdown");
		}
		connections.clear();
	}

}
