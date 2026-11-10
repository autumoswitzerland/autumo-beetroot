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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.autumo.beetroot.Constants;
import ch.autumo.beetroot.SessionManager;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;
import jakarta.websocket.server.HandshakeRequest;
import jakarta.websocket.server.ServerEndpointConfig;

/**
 * Jakarta WebSocket endpoint for servlet containers.
 *
 * <p>
 * This endpoint is registered programmatically during servlet initialization
 * and delegates all protocol handling to the transport-neutral
 * {@link WebSocketSessionHandler}.
 * </p>
 */
public class ServletWebSocketEndpoint extends Endpoint {

    private static final Logger LOG = LoggerFactory.getLogger(ServletWebSocketEndpoint.class);

    /**
     * Configurator that captures the handshake request and stores it in the config's user properties.
     */
    public static class ServletEndpointConfigurator extends ServerEndpointConfig.Configurator {

        @Override
        public void modifyHandshake(ServerEndpointConfig config, HandshakeRequest request,
                jakarta.websocket.HandshakeResponse response) {
            config.getUserProperties().put("jakarta.handshakeRequest", request);
            super.modifyHandshake(config, request, response);
        }
    }

    private final WebSocketManager manager;

    /**
     * Create the endpoint with a reference to the WebSocket manager.
     *
     * @param manager WebSocket manager
     */
    public ServletWebSocketEndpoint(WebSocketManager manager) {
        this.manager = manager;
    }

    public ServerEndpointConfig getConfig(String path) {
        return ServerEndpointConfig.Builder.create(ServletWebSocketEndpoint.class, path)
                .configurator(new ServletEndpointConfigurator())
                .build();
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        try {
            HandshakeRequest handshake = null;
            if (config != null && config.getUserProperties() != null) {
                Object stored = config.getUserProperties().get("jakarta.handshakeRequest");
                if (stored instanceof HandshakeRequest) {
                    handshake = (HandshakeRequest) stored;
                }
            }
            if (handshake == null) {
                LOG.error("WebSocket handshake without handshake request; rejecting");
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Configuration error"));
                return;
            }

            // Extract the beetRoot user session from the HTTP session
            final Object httpSessionObj = handshake.getHttpSession();
            if (httpSessionObj == null) {
                LOG.warn("WebSocket handshake without HTTP session; rejecting");
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "No HTTP session"));
                return;
            }

            final jakarta.servlet.http.HttpSession httpSession =
                    (jakarta.servlet.http.HttpSession) httpSessionObj;

            // Get the session token from the HTTP session attribute or cookie
            String token = null;
            final Object tokenAttr = httpSession.getAttribute(Constants.KEY_WEBSOCKET_SESSION_TOKEN);
            if (tokenAttr != null) {
                token = String.valueOf(tokenAttr);
            }

            final ch.autumo.beetroot.Session userSession = SessionManager.getInstance().findByExternalSessionId(token);
            if (userSession == null || userSession.getUser() == null) {
                LOG.warn("WebSocket handshake with invalid session; rejecting");
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Invalid session"));
                return;
            }

            if (userSession.isOlderThanSessionTimeout()) {
                LOG.warn("WebSocket handshake with expired session; rejecting");
                session.close(new CloseReason(CloseReason.CloseCodes.CANNOT_ACCEPT, "Session expired"));
                return;
            }

            // Check connection limit
            if (!manager.canAccept()) {
                LOG.warn("WebSocket connection limit reached; rejecting");
                session.close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "Too many connections"));
                return;
            }

            // Create the transport connection and session handler
            final ServletWebSocketConnection connection = new ServletWebSocketConnection(session);
            final WebSocketSessionHandler handler = new WebSocketSessionHandler(connection, userSession, manager);

            // Store handler in user properties for access from message handlers
            session.getUserProperties().put("beetroot.handler", handler);

            // Register the connection
            if (!manager.register(handler)) {
                LOG.warn("Failed to register WebSocket connection");
                session.close(new CloseReason(CloseReason.CloseCodes.TOO_BIG, "Too many connections"));
                return;
            }

            // Set up message handler
            session.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    handler.received(message);
                }
            });

            // Notify handler that connection is open
            handler.opened();
            userSession.refresh();

            LOG.debug("WebSocket connection opened for user '{}'", userSession.getUserName());
        } catch (Exception e) {
            LOG.error("WebSocket open failed", e);
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.UNEXPECTED_CONDITION,
                        "Internal error: " + e.getMessage()));
            } catch (IOException ioe) {
                LOG.trace("Could not close WebSocket session", ioe);
            }
        }
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        final WebSocketSessionHandler handler =
                (WebSocketSessionHandler) session.getUserProperties().get("beetroot.handler");
        if (handler != null) {
            handler.closed();
            manager.unregister(handler);
            LOG.debug("WebSocket connection closed: {} - {}", closeReason.getCloseCode(),
                    closeReason.getReasonPhrase());
        }
    }

    @Override
    public void onError(Session session, Throwable thr) {
        final WebSocketSessionHandler handler =
                (WebSocketSessionHandler) session.getUserProperties().get("beetroot.handler");
        if (handler != null) {
            LOG.warn("WebSocket error for user '{}'", handler.getUserSession().getUserName(), thr);
            handler.close("Error: " + thr.getMessage());
        } else {
            LOG.warn("WebSocket error during handshake", thr);
        }
    }

    /**
     * Transport connection implementation for Jakarta WebSocket.
     */
    static class ServletWebSocketConnection implements WebSocketConnection {

        private final jakarta.websocket.Session session;

        ServletWebSocketConnection(jakarta.websocket.Session session) {
            this.session = session;
        }

        @Override
        public boolean isOpen() {
            return session != null && session.isOpen();
        }

        @Override
        public void send(String payload) throws IOException {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendText(payload);
            }
        }

        @Override
        public void ping() throws IOException {
            if (session != null && session.isOpen()) {
                session.getBasicRemote().sendPing(java.nio.ByteBuffer.wrap(new byte[0]));
            }
        }

        @Override
        public void close(int statusCode, String reason) {
            if (session != null && session.isOpen()) {
                try {
                    CloseReason.CloseCode code = CloseReason.CloseCodes.getCloseCode(statusCode);
                    if (code == null) {
                        code = CloseReason.CloseCodes.GOING_AWAY;
                    }
                    session.close(new CloseReason(code, reason == null ? "" : reason));
                } catch (IOException e) {
                    LOG.trace("Could not close Jakarta WebSocket session", e);
                }
            }
        }

        @Override
        public void closeTransport() {
            // No separate transport to close; the session close handles it.
        }
    }
}